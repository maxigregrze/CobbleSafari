package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.compat.accessories.AccessoriesCompat;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class RotomPhoneEquipped {

    private RotomPhoneEquipped() {}

    public static ItemStack findPhoneForHandOrAccessory(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.ROTOM_PHONE)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.is(ModItems.ROTOM_PHONE)) {
            return off;
        }
        if (Services.PLATFORM.isModLoaded("accessories")) {
            ItemStack accessory = AccessoriesCompat.getPhoneFromSlot(player);
            if (!accessory.isEmpty()) {
                return accessory;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * True when the player carries either Rotom device - phone or Rotie Earpiece - in a hand or in an
     * Accessories slot. Used to decide whether Rotom has a mouth to speak through; the Accessories
     * lookup already accepts both items ({@code AccessoriesCompat.isPhoneOrEarpiece}).
     *
     * <p>The rest of the inventory is deliberately not searched: those are exactly the slots where a
     * safety feature can apply, and a phone at the bottom of the bag has never protected anything.
     */
    public static boolean hasRotomDevice(Player player) {
        if (isRotomDevice(player.getMainHandItem()) || isRotomDevice(player.getOffhandItem())) {
            return true;
        }
        return Services.PLATFORM.isModLoaded("accessories")
                && !AccessoriesCompat.getPhoneFromSlot(player).isEmpty();
    }

    private static boolean isRotomDevice(ItemStack stack) {
        return stack.is(ModItems.ROTOM_PHONE) || stack.is(ModItems.ROTOM_EARPIECE);
    }
}
