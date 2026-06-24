package maxigregrze.cobblesafari.block.base;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Shared {@link VoxelShape} transforms for orientable blocks with custom models.
 *
 * <p>Every blockstate in the mod rotates its model with the convention
 * {@code north=0, east=90, south=180, west=270} (clockwise, viewed from above), and the
 * hitbox must follow the same rotation. {@link #rotateHorizontal} reproduces exactly the
 * per‑facing tables that the legacy blocks (tombstones, statues, karate mannequin) used to
 * hand‑roll: author a shape for one reference facing, and this rotates it to the others.</p>
 *
 * <p>The unit rotation below maps the cycle {@code NORTH → WEST → SOUTH → EAST → NORTH}
 * (one application = one step). The per‑facing step count is calibrated so that, for a
 * shape authored for {@code SOUTH}, the result equals the old {@code shapeForFacing}
 * tables, and for a shape authored for {@code NORTH} it equals the karate table — both
 * verified against the in‑game blockstate {@code y} rotations.</p>
 *
 * <p>Note: {@code block.hyperspace.HyperspaceShapes} reuses {@link #rotateClockwise90} as
 * its primitive but keeps a different facing→step mapping for the ramp / scaffolding‑stairs
 * blocks, whose blockstates were authored against it.</p>
 */
public final class BlockShapeUtils {

    private BlockShapeUtils() {}

    /**
     * One 90° step of the horizontal rotation used by the legacy cardinal blocks.
     * In [0,1] space: {@code box(minZ, minY, 1-maxX, maxZ, maxY, 1-minX)}.
     */
    public static VoxelShape rotateClockwise90(VoxelShape shape) {
        VoxelShape[] acc = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                acc[0] = Shapes.or(acc[0],
                        Shapes.box(minZ, minY, 1 - maxX, maxZ, maxY, 1 - minX)));
        return acc[0];
    }

    /**
     * Rotates {@code shape} (drawn for {@code authored}) to {@code facing}, following the
     * mod's blockstate rotation convention. {@code authored}/{@code facing} must be
     * horizontal.
     */
    public static VoxelShape rotateHorizontal(VoxelShape shape, Direction authored, Direction facing) {
        int steps = (index(facing) - index(authored) + 4) % 4;
        VoxelShape current = shape;
        for (int i = 0; i < steps; i++) {
            current = rotateClockwise90(current);
        }
        return current;
    }

    /** Pre‑computes the four horizontal facings of a shape authored for {@code authored}. */
    public static Map<Direction, VoxelShape> precompute(VoxelShape shape, Direction authored) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            map.put(d, rotateHorizontal(shape, authored, d));
        }
        return map;
    }

    /** Vertical mirror (floor → ceiling): {@code box(x0, 16-y1, z0, x1, 16-y0, z1)}. */
    public static VoxelShape flipVertical(VoxelShape shape) {
        VoxelShape[] acc = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                acc[0] = Shapes.or(acc[0],
                        Shapes.box(minX, 1 - maxY, minZ, maxX, 1 - minY, maxZ)));
        return acc[0];
    }

    // Cycle index matching one application of rotateClockwise90: N → W → S → E.
    private static int index(Direction d) {
        return switch (d) {
            case NORTH -> 0;
            case WEST -> 1;
            case SOUTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }
}
