package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.StairShapeLogic;
import maxigregrze.cobblesafari.block.base.Waterlogged;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Kalosian Handrail (plan 194 - 4): a railing that connects like a stair. It stands where a vanilla
 * stair in the same spot would have its high step - along the FACING edge - and takes the stair's
 * inner / outer corners from neighbouring handrails only, so it never bends a real stair.
 *
 * <p>The models overhang the cell by a pixel on each side so two handrails share their post; the
 * collision does the same, which vanilla supports for out-of-cell shapes.</p>
 */
public class HyperspaceHandrailBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<HyperspaceHandrailBlock> CODEC = simpleCodec(HyperspaceHandrailBlock::new);
    public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;

    // Authored for FACING=north: the envelope of the rail and its two posts, the corner post alone,
    // and the north + east rails of the inner corner.
    private static final Map<StairsShape, Map<Direction, VoxelShape>> SHAPES = StairShapeLogic.rotations(
            Block.box(-1, 0, -1, 17, 14, 1),
            Shapes.or(Block.box(-1, 0, -1, 17, 14, 1), Block.box(15, 0, 1, 17, 14, 17)),
            Block.box(15, 0, -1, 17, 14, 1));

    public HyperspaceHandrailBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SHAPE, StairsShape.STRAIGHT)
                .setValue(Waterlogged.WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE, Waterlogged.WATERLOGGED);
    }

    private static StairsShape shapeAt(Direction facing, BlockGetter level, BlockPos pos) {
        return StairShapeLogic.compute(facing, level, pos,
                neighbour -> neighbour.getBlock() instanceof HyperspaceHandrailBlock,
                neighbour -> neighbour.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(SHAPE, shapeAt(facing, context.getLevel(), context.getClickedPos()))
                .setValue(Waterlogged.WATERLOGGED, Waterlogged.atPlacement(context));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Waterlogged.scheduleTick(state, level, pos);
        if (direction.getAxis().isHorizontal()) {
            return state.setValue(SHAPE, shapeAt(state.getValue(FACING), level, pos));
        }
        return state;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Waterlogged.fluidState(state, super.getFluidState(state));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(SHAPE)).get(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /** Any mirror swaps handedness: a left corner becomes a right one. */
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
                .setValue(SHAPE, mirrorSides(state.getValue(SHAPE)));
    }

    private static StairsShape mirrorSides(StairsShape shape) {
        return switch (shape) {
            case INNER_LEFT -> StairsShape.INNER_RIGHT;
            case INNER_RIGHT -> StairsShape.INNER_LEFT;
            case OUTER_LEFT -> StairsShape.OUTER_RIGHT;
            case OUTER_RIGHT -> StairsShape.OUTER_LEFT;
            default -> shape;
        };
    }
}
