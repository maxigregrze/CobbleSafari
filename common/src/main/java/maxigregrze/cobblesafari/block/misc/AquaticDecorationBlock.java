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

public class AquaticDecorationBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<AquaticDecorationBlock> CODEC = simpleCodec(AquaticDecorationBlock::new);
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;

    private final VoxelShape floorShape;
    private final VoxelShape ceilingShape;

    public AquaticDecorationBlock(Properties properties) {
        this(properties, Block.box(2, 0, 2, 14, 16, 14));
    }

    public AquaticDecorationBlock(Properties properties, VoxelShape floorShape) {
        super(properties);
        this.floorShape = floorShape;
        this.ceilingShape = Block.box(
                floorShape.min(Direction.Axis.X) * 16,
                16 - floorShape.max(Direction.Axis.Y) * 16,
                floorShape.min(Direction.Axis.Z) * 16,
                floorShape.max(Direction.Axis.X) * 16,
                16 - floorShape.min(Direction.Axis.Y) * 16,
                floorShape.max(Direction.Axis.Z) * 16
        );
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

        if (!isPrismarine(supportState)) {
            return null;
        }

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
        return Boolean.TRUE.equals(state.getValue(HANGING)) ? ceilingShape : floorShape;
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

        if (!isPrismarine(supportState)) {
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

    private static boolean isPrismarine(BlockState state) {
        return state.is(Blocks.PRISMARINE)
                || state.is(Blocks.PRISMARINE_BRICKS)
                || state.is(Blocks.DARK_PRISMARINE);
    }
}
