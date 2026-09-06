package maxigregrze.cobblesafari.block.base;

import maxigregrze.cobblesafari.manager.BannedItemsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Turning one block into another by right-clicking it with a hoe, the vanilla way (dirt to path,
 * grass to farmland), plus the mod's shared build guard.
 *
 * <p>Nothing is wired in here: the caller passes its own target, so any block family can reuse it.
 * The guard mirrors {@code HyperspaceBarrierBlock}: a conversion destroys a block and places
 * another, so it is refused to players who may not build, and in dimensions whose dimensional-ban
 * config forbids either half. Creative bypasses, matching {@code DimensionalBanEventHandler}.</p>
 */
public final class HoeConversion {

    private HoeConversion() {
    }

    /**
     * Converts the block at {@code pos} into {@code target}. Returns
     * {@link ItemInteractionResult#PASS_TO_DEFAULT_BLOCK_INTERACTION} when the held item is not a
     * hoe, so an unrelated click keeps its normal handling.
     */
    public static ItemInteractionResult tryConvert(ItemStack stack, Level level, BlockPos pos,
                                                   @Nullable Player player, InteractionHand hand, Block target) {
        if (!(stack.getItem() instanceof HoeItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player == null || !player.mayBuild()) {
            return ItemInteractionResult.FAIL;
        }
        if (!player.isCreative()
                && (!BannedItemsManager.isBlockBreakingAllowed(level.dimension())
                    || !BannedItemsManager.isBlockPlacingAllowed(level.dimension()))) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("cobblesafari.ban.block_breaking_banned"));
            }
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            BlockState converted = target.defaultBlockState();
            level.setBlock(pos, converted, Block.UPDATE_ALL);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, converted));
            level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
