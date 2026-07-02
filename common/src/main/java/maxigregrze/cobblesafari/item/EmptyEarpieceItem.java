package maxigregrze.cobblesafari.item;

import maxigregrze.cobblesafari.rotomphone.EmptyPhoneServerHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Empty earpiece — a pure item (not a block) that, on right-click, opens the shared Rotom
 * confirmation screen and, on confirm, is turned into a {@link RotomEarpieceItem} using the first
 * Rotom of the player's party (mirrors {@link EmptyPhoneItem} but targets the earpiece).
 */
public class EmptyEarpieceItem extends Item {

    public EmptyEarpieceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int slot = hand == InteractionHand.MAIN_HAND
                    ? player.getInventory().selected
                    : 40;
            EmptyPhoneServerHandler.attemptFill(serverPlayer, false, null, slot,
                    EmptyPhoneServerHandler.FillTarget.EARPIECE);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }
}
