package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DistortionWeedBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<DistortionWeedBlock> CODEC = simpleCodec(DistortionWeedBlock::new);
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;

    private static final VoxelShape FLOOR_SHAPE = Block.box(2, 0, 2, 14, 4, 14);
    private static final VoxelShape CEILING_SHAPE = Block.box(2, 12, 2, 14, 16, 14);

    public DistortionWeedBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HANGING, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HANGING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        boolean hanging = clickedFace == Direction.DOWN;
        BlockPos pos = context.getClickedPos();
        BlockPos supportPos = hanging ? pos.above() : pos.below();
        BlockState supportState = context.getLevel().getBlockState(supportPos);
        Direction supportDir = hanging ? Direction.DOWN : Direction.UP;
        if (!supportState.isFaceSturdy(context.getLevel(), supportPos, supportDir)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HANGING, hanging);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING).booleanValue() ? CEILING_SHAPE : FLOOR_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        boolean hanging = state.getValue(HANGING);
        BlockPos supportPos = hanging ? pos.above() : pos.below();
        BlockState supportState = level.getBlockState(supportPos);
        Direction supportDir = hanging ? Direction.DOWN : Direction.UP;
        return supportState.isFaceSturdy(level, supportPos, supportDir);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean hanging = state.getValue(HANGING);
        Direction checkDir = hanging ? Direction.UP : Direction.DOWN;
        if (direction == checkDir && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
