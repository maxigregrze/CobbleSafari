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
}
