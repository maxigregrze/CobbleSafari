package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Animated trashcan: orientable single block whose lid swings open on right-click (rendered by
 * {@code HyperspaceTrashcanBlockEntityRenderer}), holds ~1 s, then falls back. Open/close sounds
 * and a short cooldown. Mechanics mirror {@code PunchingBagBlock} (block event + block entity).
 */
public class HyperspaceTrashcanBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<HyperspaceTrashcanBlock> CODEC = simpleCodec(HyperspaceTrashcanBlock::new);

    public static final int EVENT_OPEN = 1;
    /** Open + hold ticks before the lid starts falling (the "close" sound + cooldown reference). */
    private static final int CLOSE_DELAY_TICKS = 25;
    private static final int COOLDOWN_TICKS = 40;

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public HyperspaceTrashcanBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HyperspaceTrashcanBlockEntity trash) {
                long now = level.getGameTime();
                long last = trash.getLastOpenGameTime();
                if (last != Long.MIN_VALUE && now - last < COOLDOWN_TICKS) {
                    return InteractionResult.CONSUME;
                }
                level.blockEvent(pos, this, EVENT_OPEN, 0);
                level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.8F, 1.2F);
                level.scheduleTick(pos, this, CLOSE_DELAY_TICKS);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Plays the "close" sound when the lid starts to fall back.
        level.playSound(null, pos, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(id, param);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HyperspaceTrashcanBlockEntity(pos, state);
    }
}
