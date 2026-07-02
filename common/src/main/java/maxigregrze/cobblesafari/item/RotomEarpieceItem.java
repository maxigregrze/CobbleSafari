package maxigregrze.cobblesafari.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Filled "Rotie Earpiece" — an accessory variant of the Rotom Phone.
 *
 * <p>Reuses {@link RotomPhoneItem}'s static NBT helpers, tooltip and {@code inventoryTick}, but
 * unlike the phone the earpiece does <b>not</b> open the GUI on right-click: it only opens when
 * equipped in the Accessories {@code hat} slot and the open-phone hotkey is pressed.
 */
public class RotomEarpieceItem extends RotomPhoneItem {

    public RotomEarpieceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // No GUI on right-click; the earpiece is opened via hotkey only while equipped (see
        // RotomPhoneServerHandler.findPhoneInInventory + AccessoriesCompat).
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
