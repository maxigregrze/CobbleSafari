package maxigregrze.cobblesafari.block.underground;

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

public class UndergroundBoulderBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<UndergroundBoulderBlock> CODEC = simpleCodec(UndergroundBoulderBlock::new);
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;

    private static final VoxelShape SHAPE_FLOOR = Block.box(-2, 0, -2, 18, 18, 18);
    private static final VoxelShape SHAPE_CEILING = Block.box(-2, -2, -2, 18, 16, 18);

    public UndergroundBoulderBlock(Properties properties) {
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
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HANGING, hanging);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Boolean.TRUE.equals(state.getValue(HANGING)) ? SHAPE_CEILING : SHAPE_FLOOR;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        boolean hanging = Boolean.TRUE.equals(state.getValue(HANGING));
        BlockPos supportPos = hanging ? pos.above() : pos.below();
        BlockState supportState = level.getBlockState(supportPos);
        Direction supportDir = hanging ? Direction.DOWN : Direction.UP;
        return supportState.isFaceSturdy(level, supportPos, supportDir);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean hanging = Boolean.TRUE.equals(state.getValue(HANGING));
        Direction checkDir = hanging ? Direction.UP : Direction.DOWN;
        if (direction == checkDir && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
