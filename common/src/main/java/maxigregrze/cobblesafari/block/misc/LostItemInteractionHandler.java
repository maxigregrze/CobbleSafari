package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.item.LostItemTemplateItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Creative authoring gestures on a Lost Item that hold an item (action plan 193, sections 4 and 5).
 *
 * <p>Vanilla skips a block's own use methods when a sneaking player holds anything, so these gestures
 * cannot live in {@link LostItemBlock}: both loaders call this from their use-block event, which fires
 * first, on both sides. An empty hand, or any other case, returns {@code PASS} and vanilla carries on.
 */
public final class LostItemInteractionHandler {

    private LostItemInteractionHandler() {
    }

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.isCreative()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof LostItemBlock)
                || !(level.getBlockEntity(pos) instanceof LostItemBlockEntity be)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return InteractionResult.PASS;
        }
        // The template wins over the mode 2 copy, whatever the block's mode.
        if (held.is(ModItems.LOST_ITEM_TEMPLATE)) {
            return LostItemTemplateItem.onUseLostItem(player, level, pos, be, held);
        }
        if (player.isShiftKeyDown() && be.getMode() == 2) {
            if (!level.isClientSide()) {
                be.setLootItemStack(held);
                be.syncConfigToClients();
                player.displayClientMessage(Component.translatable(
                        "gui.cobblesafari.lost_item.loot_item_set", held.getHoverName(), held.getCount()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }
}
