package maxigregrze.cobblesafari.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Vanilla's stair corner rule ({@code StairBlock.getStairsShape}, private there) for blocks that
 * connect like stairs without being stairs, plus the quarter-cell geometry it implies.
 *
 * <p>Conventions are vanilla's: the high part of a stair sits on its FACING side, and "right" is
 * {@code facing.getClockWise()} - so {@code OUTER_RIGHT} facing north is the north-east quarter and
 * {@code INNER_RIGHT} facing north is the north half plus the south-east quarter.</p>
 */
public final class StairShapeLogic {

    private StairShapeLogic() {}

    /**
     * The corner a stair-like block takes from its front and back neighbours. {@code peer} says
     * which neighbours count (same family, same half...), {@code facingOf} reads their FACING.
     */
    public static StairsShape compute(Direction facing, BlockGetter level, BlockPos pos,
                                      Predicate<BlockState> peer, Function<BlockState, Direction> facingOf) {
        BlockState front = level.getBlockState(pos.relative(facing));
        if (peer.test(front)) {
            Direction frontFacing = facingOf.apply(front);
            if (frontFacing.getAxis() != facing.getAxis()
                    && canTakeShape(facing, level, pos, frontFacing.getOpposite(), peer, facingOf)) {
                return frontFacing == facing.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
            }
        }
        BlockState back = level.getBlockState(pos.relative(facing.getOpposite()));
        if (peer.test(back)) {
            Direction backFacing = facingOf.apply(back);
            if (backFacing.getAxis() != facing.getAxis()
                    && canTakeShape(facing, level, pos, backFacing, peer, facingOf)) {
                return backFacing == facing.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
            }
        }
        return StairsShape.STRAIGHT;
    }

    private static boolean canTakeShape(Direction facing, BlockGetter level, BlockPos pos, Direction side,
                                        Predicate<BlockState> peer, Function<BlockState, Direction> facingOf) {
        BlockState neighbour = level.getBlockState(pos.relative(side));
        return !peer.test(neighbour) || facingOf.apply(neighbour) != facing;
    }

    /**
     * Shape for {@code shape} at {@code facing}, from the one authored for FACING=north. The LEFT
     * corners are the RIGHT corners of the facing one quarter turn counter-clockwise, which is how
     * the blockstates turn the model too ({@code y = angle + 270}).
     */
    public static Map<StairsShape, Map<Direction, VoxelShape>> rotations(VoxelShape straightNorth,
                                                                       VoxelShape innerRightNorth,
                                                                       VoxelShape outerRightNorth) {
        Map<Direction, VoxelShape> straight = BlockShapeUtils.precompute(straightNorth, Direction.NORTH);
        Map<Direction, VoxelShape> inner = BlockShapeUtils.precompute(innerRightNorth, Direction.NORTH);
        Map<Direction, VoxelShape> outer = BlockShapeUtils.precompute(outerRightNorth, Direction.NORTH);
        Map<StairsShape, Map<Direction, VoxelShape>> out = new EnumMap<>(StairsShape.class);
        out.put(StairsShape.STRAIGHT, straight);
        out.put(StairsShape.INNER_RIGHT, inner);
        out.put(StairsShape.OUTER_RIGHT, outer);
        out.put(StairsShape.INNER_LEFT, leftOf(inner));
        out.put(StairsShape.OUTER_LEFT, leftOf(outer));
        return out;
    }

    private static Map<Direction, VoxelShape> leftOf(Map<Direction, VoxelShape> right) {
        Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            out.put(facing, right.get(facing.getCounterClockWise()));
        }
        return out;
    }

    /**
     * The quarters (8x8 columns) a stair's high step covers, as one box per quarter between
     * {@code minY} and {@code maxY} pixels.
     */
    public static VoxelShape stepQuarters(Direction facing, StairsShape shape, double minY, double maxY) {
        Direction right = facing.getClockWise();
        Direction left = facing.getCounterClockWise();
        Direction back = facing.getOpposite();
        VoxelShape out = Shapes.or(quarter(facing, right, minY, maxY), quarter(facing, left, minY, maxY));
        return switch (shape) {
            case STRAIGHT -> out;
            case OUTER_RIGHT -> quarter(facing, right, minY, maxY);
            case OUTER_LEFT -> quarter(facing, left, minY, maxY);
            case INNER_RIGHT -> Shapes.or(out, quarter(back, right, minY, maxY));
            case INNER_LEFT -> Shapes.or(out, quarter(back, left, minY, maxY));
        };
    }

    /** The quarter column at the corner of two perpendicular horizontal directions. */
    private static VoxelShape quarter(Direction a, Direction b, double minY, double maxY) {
        Direction xDir = a.getAxis() == Direction.Axis.X ? a : b;
        Direction zDir = a.getAxis() == Direction.Axis.Z ? a : b;
        double x0 = xDir == Direction.EAST ? 8 : 0;
        double z0 = zDir == Direction.SOUTH ? 8 : 0;
        return Block.box(x0, minY, z0, x0 + 8, maxY, z0 + 8);
    }
}
