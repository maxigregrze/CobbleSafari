package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.HorizontalModelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Kalosian Bench (plan 194 - 6): a chair-like seat that joins the benches beside it - same FACING,
 * on its left or right - into one long bench. The ends keep their legs, the middle has none.
 *
 * <p>The model stands as drawn at FACING=south, so its east side is {@code facing.getCounterClockWise()}:
 * the blockstate turns the model with FACING, and that side always lands on the same neighbour.</p>
 */
public class HyperspaceBenchBlock extends HorizontalModelBlock {

    public static final EnumProperty<BenchPart> PART = EnumProperty.create("part", BenchPart.class);

    public HyperspaceBenchBlock(Properties properties, Settings settings) {
        super(properties, settings);
        this.registerDefaultState(this.defaultBlockState().setValue(PART, BenchPart.SOLO));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new HyperspaceBenchBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    private static BlockState withPart(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        boolean east = joins(facing, level.getBlockState(pos.relative(facing.getCounterClockWise())));
        boolean west = joins(facing, level.getBlockState(pos.relative(facing.getClockWise())));
        return state.setValue(PART, BenchPart.of(east, west));
    }

    private static boolean joins(Direction facing, BlockState neighbour) {
        return neighbour.getBlock() instanceof HyperspaceBenchBlock && neighbour.getValue(FACING) == facing;
    }

    /** A mirror swaps the bench's two sides. */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState out = super.mirror(state, mirror);
        if (mirror == Mirror.NONE) {
            return out;
        }
        BenchPart part = state.getValue(PART);
        return out.setValue(PART, switch (part) {
            case EAST -> BenchPart.WEST;
            case WEST -> BenchPart.EAST;
            default -> part;
        });
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : withPart(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if (!updated.is(this) || direction.getAxis() == state.getValue(FACING).getAxis()
                || direction.getAxis() == Direction.Axis.Y) {
            return updated;
        }
        return withPart(updated, level, pos);
    }
}
