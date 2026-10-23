package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A {@link SideConnectedModelBlock} that also knows its four diagonal neighbours on the same Y, so a
 * connected top texture can close its inner corners (plan 194 - 15).
 *
 * <p>A diagonal is stored {@code true} only when it changes the picture: both orthogonal neighbours
 * next to it connect <em>and</em> the diagonal block is itself one of these blocks that connects.
 * Everything else reads {@code false}, so one appearance has one state. Restricting the diagonal to
 * this family is also what makes {@link #updateIndirectNeighbourShapes} a complete propagation: only
 * these blocks can change a diagonal, and they notify their diagonals whenever they appear or go.</p>
 */
public class DiagonalSideConnectedModelBlock extends SideConnectedModelBlock {

    public static final BooleanProperty NORTH_EAST = BooleanProperty.create("north_east");
    public static final BooleanProperty NORTH_WEST = BooleanProperty.create("north_west");
    public static final BooleanProperty SOUTH_EAST = BooleanProperty.create("south_east");
    public static final BooleanProperty SOUTH_WEST = BooleanProperty.create("south_west");

    public DiagonalSideConnectedModelBlock(Properties properties, Settings settings) {
        super(properties, settings);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(NORTH_EAST, false)
                .setValue(NORTH_WEST, false)
                .setValue(SOUTH_EAST, false)
                .setValue(SOUTH_WEST, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new DiagonalSideConnectedModelBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST);
    }

    @Override
    protected BlockState computeState(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState out = super.computeState(state, level, pos);
        return out
                .setValue(NORTH_EAST, diagonal(state, out, level, pos, Direction.NORTH, Direction.EAST))
                .setValue(NORTH_WEST, diagonal(state, out, level, pos, Direction.NORTH, Direction.WEST))
                .setValue(SOUTH_EAST, diagonal(state, out, level, pos, Direction.SOUTH, Direction.EAST))
                .setValue(SOUTH_WEST, diagonal(state, out, level, pos, Direction.SOUTH, Direction.WEST));
    }

    private boolean diagonal(BlockState self, BlockState sides, BlockGetter level, BlockPos pos,
                             Direction a, Direction b) {
        if (!sides.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(a))
                || !sides.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(b))) {
            return false;
        }
        BlockState corner = ApparentState.at(level, pos.relative(a).relative(b));
        return corner.getBlock() instanceof DiagonalSideConnectedModelBlock && connectsTo(self, corner, a);
    }

    /**
     * Vanilla's hook for indirect neighbours, the one redstone wire uses: {@code Level.setBlock} calls
     * it for the old and the new state, so a platform appearing or leaving refreshes its diagonals in
     * the same tick. {@code updateShape} alone only ever reaches the six face neighbours.
     */
    @Override
    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos,
                                                 int flags, int recursionLeft) {
        for (Direction a : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
            for (Direction b : new Direction[]{Direction.EAST, Direction.WEST}) {
                BlockPos cornerPos = pos.relative(a).relative(b);
                BlockState corner = level.getBlockState(cornerPos);
                if (corner.getBlock() instanceof DiagonalSideConnectedModelBlock block) {
                    BlockState fixed = block.computeState(corner, level, cornerPos);
                    if (fixed != corner) {
                        level.setBlock(cornerPos, fixed, flags, recursionLeft);
                    }
                }
            }
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState out = super.rotate(state, rotation);
        boolean ne = state.getValue(NORTH_EAST);
        boolean nw = state.getValue(NORTH_WEST);
        boolean se = state.getValue(SOUTH_EAST);
        boolean sw = state.getValue(SOUTH_WEST);
        return switch (rotation) {
            case CLOCKWISE_90 -> out.setValue(NORTH_EAST, nw).setValue(SOUTH_EAST, ne)
                    .setValue(SOUTH_WEST, se).setValue(NORTH_WEST, sw);
            case CLOCKWISE_180 -> out.setValue(NORTH_EAST, sw).setValue(SOUTH_EAST, nw)
                    .setValue(SOUTH_WEST, ne).setValue(NORTH_WEST, se);
            case COUNTERCLOCKWISE_90 -> out.setValue(NORTH_EAST, se).setValue(SOUTH_EAST, sw)
                    .setValue(SOUTH_WEST, nw).setValue(NORTH_WEST, ne);
            default -> out;
        };
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState out = super.mirror(state, mirror);
        return switch (mirror) {
            case LEFT_RIGHT -> out.setValue(NORTH_EAST, state.getValue(SOUTH_EAST))
                    .setValue(SOUTH_EAST, state.getValue(NORTH_EAST))
                    .setValue(NORTH_WEST, state.getValue(SOUTH_WEST))
                    .setValue(SOUTH_WEST, state.getValue(NORTH_WEST));
            case FRONT_BACK -> out.setValue(NORTH_EAST, state.getValue(NORTH_WEST))
                    .setValue(NORTH_WEST, state.getValue(NORTH_EAST))
                    .setValue(SOUTH_EAST, state.getValue(SOUTH_WEST))
                    .setValue(SOUTH_WEST, state.getValue(SOUTH_EAST));
            default -> out;
        };
    }
}
