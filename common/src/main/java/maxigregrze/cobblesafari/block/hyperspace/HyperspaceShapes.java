package maxigregrze.cobblesafari.block.hyperspace;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Helpers to rotate a NORTH-authored {@link VoxelShape} to the four horizontal facings. */
public final class HyperspaceShapes {

    private HyperspaceShapes() {}

    /**
     * Legacy rotation keyed on {@code get2DDataValue} (cycle N→E→S→W). This does <em>not</em>
     * line up with the blockstate {@code y} rotation, so it is only safe for shapes that are
     * symmetric on the off-axis (e.g. the flat directional banner panels). For collision boxes
     * that must match the rendered model, use {@link #rotateForFacing}.
     */
    public static VoxelShape rotate(VoxelShape north, Direction facing) {
        int times = (facing.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;
        return apply(north, times);
    }

    /**
     * Rotates a NORTH-authored shape so it matches the standard blockstate {@code y} rotation
     * ({@code north=0, east=90, south=180, west=270}). Use this whenever the collision/selection
     * box has to line up with the model (cycle N→W→S→E).
     */
    public static VoxelShape rotateForFacing(VoxelShape north, Direction facing) {
        int times = switch (facing) {
            case WEST -> 1;
            case SOUTH -> 2;
            case EAST -> 3;
            default -> 0; // NORTH (and the unused vertical directions)
        };
        return apply(north, times);
    }

    private static VoxelShape apply(VoxelShape shape, int quarterTurns) {
        VoxelShape current = shape;
        for (int i = 0; i < quarterTurns; i++) {
            current = rotateOnceClockwise(current);
        }
        return current;
    }

    private static VoxelShape rotateOnceClockwise(VoxelShape shape) {
        VoxelShape[] acc = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                acc[0] = Shapes.or(acc[0],
                        Shapes.box(minZ, minY, 1 - maxX, maxZ, maxY, 1 - minX)));
        return acc[0];
    }
}
