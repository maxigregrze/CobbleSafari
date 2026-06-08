package maxigregrze.cobblesafari.block.csboss;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.csboss.BossBattleManager;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Arena Trigger block (plan 100 § 3). Two states: {@code active} false/true (distinct models).
 */
public class CsBossTriggerBlock extends BaseEntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final MapCodec<CsBossTriggerBlock> CODEC = simpleCodec(CsBossTriggerBlock::new);

    public CsBossTriggerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CsBossTriggerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)
                || !(level instanceof ServerLevel serverLevel)
                || !(level.getBlockEntity(pos) instanceof CsBossTriggerBlockEntity be)) {
            return InteractionResult.PASS;
        }
        // Creative + empty hand + no shift ⇒ config GUI.
        if (sp.isCreative() && !sp.isShiftKeyDown()) {
            Services.PLATFORM.sendPayloadToPlayer(sp, be.createOpenPayload());
            return InteractionResult.CONSUME;
        }
        return BossBattleManager.tryActivate(sp, serverLevel, pos, be);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Empty hand: let useWithoutItem handle (GUI / activation).
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)
                || !(level instanceof ServerLevel serverLevel)
                || !(level.getBlockEntity(pos) instanceof CsBossTriggerBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BossBattleManager.tryActivate(sp, serverLevel, pos, be);
        return ItemInteractionResult.CONSUME;
    }
}
