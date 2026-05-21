package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneGTSScreen;
import maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneUnionScreen;
import maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneWonderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OnlineFeaturePcBlock extends HorizontalDirectionalBlock {

    public enum Kind {
        LEGACY,
        UNION,
        GTS,
        WONDER
    }

    public static final MapCodec<OnlineFeaturePcBlock> CODEC = simpleCodec(OnlineFeaturePcBlock::new);

    private static final VoxelShape LOWER_SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    /** Upper-half model in default north orientation (non-negative model Y), union of main cuboids from online_feature_pc_top. */
    private static final VoxelShape TOP_SHAPE_NORTH = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 2),
            Block.box(0, 0, 2, 16, 3, 4),
            Block.box(4.5, 0, 2.0, 11.5, 6, 2.5)
    );

    private final Kind kind;

    public OnlineFeaturePcBlock(Properties properties) {
        this(properties, Kind.LEGACY);
    }

    public OnlineFeaturePcBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.DOUBLE_BLOCK_HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return this.defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection())
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            BlockPos above = pos.above();
            level.setBlock(above, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        Direction facing = state.getValue(FACING);
        if (half == DoubleBlockHalf.LOWER) {
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        }
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof OnlineFeaturePcBlock
                && below.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                && below.getValue(FACING) == facing;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        BlockPos other = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        BlockState otherState = level.getBlockState(other);
        if (otherState.getBlock() instanceof OnlineFeaturePcBlock) {
            level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    private static VoxelShape shapeFor(BlockState state) {
        Direction facing = state.getValue(FACING);
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return rotateTopShape(TOP_SHAPE_NORTH, facing);
        }
        return LOWER_SHAPE;
    }

    /** Upper-half rotation; N/S were mirrored — swap their transforms. */
    private static VoxelShape rotateTopShape(VoxelShape shape, Direction facing) {
        return switch (facing) {
            case NORTH -> mirrorSouth(shape);
            case SOUTH -> shape;
            case EAST -> Shapes.box(
                    shape.min(Direction.Axis.Z), shape.min(Direction.Axis.Y), 1 - shape.max(Direction.Axis.X),
                    shape.max(Direction.Axis.Z), shape.max(Direction.Axis.Y), 1 - shape.min(Direction.Axis.X));
            case WEST -> Shapes.box(
                    1 - shape.max(Direction.Axis.Z), shape.min(Direction.Axis.Y), shape.min(Direction.Axis.X),
                    1 - shape.min(Direction.Axis.Z), shape.max(Direction.Axis.Y), shape.max(Direction.Axis.X));
            default -> shape;
        };
    }

    private static VoxelShape mirrorSouth(VoxelShape shape) {
        return Shapes.box(
                1 - shape.max(Direction.Axis.X), shape.min(Direction.Axis.Y), 1 - shape.max(Direction.Axis.Z),
                1 - shape.min(Direction.Axis.X), shape.max(Direction.Axis.Y), 1 - shape.min(Direction.Axis.Z));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (kind == Kind.LEGACY) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            openAppScreen(kind);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    private static void openAppScreen(Kind kind) {
        Minecraft mc = Minecraft.getInstance();
        switch (kind) {
            case UNION -> mc.setScreen(RotomPhoneUnionScreen.forOnlinePc());
            case GTS -> mc.setScreen(RotomPhoneGTSScreen.forOnlinePc());
            case WONDER -> mc.setScreen(RotomPhoneWonderScreen.forOnlinePc());
            default -> {}
        }
    }

    public static boolean isOnlineFeaturePc(Block block) {
        return block instanceof OnlineFeaturePcBlock;
    }
}
