package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AirKelpBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<AirKelpBlock> CODEC = simpleCodec(AirKelpBlock::new);
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_25;

    private static final VoxelShape FLOOR_SHAPE = Block.box(2, 0, 2, 14, 16, 14);
    private static final VoxelShape CEILING_SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public AirKelpBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HANGING, false)
                .setValue(AGE, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HANGING, AGE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        boolean hanging = clickedFace == Direction.DOWN;

        BlockPos pos = context.getClickedPos();
        BlockPos supportPos = hanging ? pos.above() : pos.below();
        BlockState supportState = context.getLevel().getBlockState(supportPos);

        if (supportState.is(ModBlocks.AIR_KELP)) {
            return this.defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(HANGING, hanging)
                    .setValue(AGE, 0);
        }

        if (!isValidSupport(supportState)) {
            return null;
        }

        Direction supportDir = hanging ? Direction.DOWN : Direction.UP;
        if (!supportState.isFaceSturdy(context.getLevel(), supportPos, supportDir)) {
            return null;
        }

        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HANGING, hanging)
                .setValue(AGE, 0);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Boolean.TRUE.equals(state.getValue(HANGING)) ? CEILING_SHAPE : FLOOR_SHAPE;
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

        if (supportState.is(ModBlocks.AIR_KELP)) {
            return true;
        }

        if (!isValidSupport(supportState)) {
            return false;
        }

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

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return !Boolean.TRUE.equals(state.getValue(HANGING))
                && state.getValue(AGE) < 25;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= 0.14f) {
            return;
        }
        int age = state.getValue(AGE);
        if (age >= 25) {
            return;
        }
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir()) {
            level.setBlock(above, state.setValue(AGE, age + 1), 3);
        }
    }

    private static boolean isValidSupport(BlockState state) {
        return state.is(Blocks.PRISMARINE)
                || state.is(Blocks.PRISMARINE_BRICKS)
                || state.is(Blocks.DARK_PRISMARINE)
                || state.is(ModBlocks.AIR_KELP);
    }
}
