package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.BlockShapeUtils;
import maxigregrze.cobblesafari.rotomphone.OnlinePcAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.Map;

public class OnlineFeaturePcBlock extends HorizontalDirectionalBlock {

    public enum Kind {
        LEGACY,
        UNION,
        GTS,
        WONDER
    }

    public static final MapCodec<OnlineFeaturePcBlock> CODEC = simpleCodec(OnlineFeaturePcBlock::new);

    /**
     * Client-only screen opener for the online-feature PC apps. Injected by the client at init
     * (see {@code ClientBlockHooks}); {@code null} on a dedicated server. Kept as a plain JDK
     * functional type so this common class carries no client-class references (a direct
     * {@code Minecraft}/screen reference would fail verification and crash a dedicated server).
     */
    public static java.util.function.Consumer<Kind> screenOpener;

    private static final VoxelShape LOWER_SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    /** Upper-half model in default north orientation (non-negative model Y), union of main cuboids from online_feature_pc_top. */
    private static final VoxelShape TOP_SHAPE_NORTH = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 2),
            Block.box(0, 0, 2, 16, 3, 4),
            Block.box(4.5, 0, 2.0, 11.5, 6, 2.5)
    );
    /** Upper-half shape per facing, rotated from the north-authored shape with the shared helper. */
    private static final Map<Direction, VoxelShape> TOP_SHAPES =
            BlockShapeUtils.precompute(TOP_SHAPE_NORTH, Direction.NORTH);

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
        maxigregrze.cobblesafari.block.base.MultiCellBreak.breakOtherHalf(level, pos, state, player, this);
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
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return TOP_SHAPES.getOrDefault(state.getValue(FACING), TOP_SHAPE_NORTH);
        }
        return LOWER_SHAPE;
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
            if (screenOpener != null) {
                screenOpener.accept(kind);
            }
            return InteractionResult.SUCCESS;
        }
        // Lets the app handlers accept this player's payloads without a Rotom Phone: the block is
        // itself a legitimate terminal for the online features (see OnlinePcAccess).
        if (player instanceof ServerPlayer serverPlayer) {
            OnlinePcAccess.grant(serverPlayer, kind, pos);
        }
        return InteractionResult.CONSUME;
    }

    public static boolean isOnlineFeaturePc(Block block) {
        return block instanceof OnlineFeaturePcBlock;
    }
}
