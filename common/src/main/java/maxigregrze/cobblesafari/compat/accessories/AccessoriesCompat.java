package maxigregrze.cobblesafari.compat.accessories;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class AccessoriesCompat {

    public static final ResourceLocation PHONE_SLOT_ID = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "rotomphone_slot");
    public static final String PHONE_SLOT_KEY = PHONE_SLOT_ID.toString();

    private AccessoriesCompat() {}

    public static void registerAccessoryItem() {
        AccessoriesAPI.registerAccessory(ModItems.ROTOM_PHONE, new RotomPhoneAccessory());
        CobbleSafari.LOGGER.info("Registered Rotom Phone accessory");
    }

    public static boolean hasPhoneInSlot(Player player) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) return false;

        Map<String, AccessoriesContainer> containers = capability.getContainers();
        AccessoriesContainer phoneContainer = containers.get(PHONE_SLOT_KEY);
        if (phoneContainer == null) return false;

        var stacks = phoneContainer.getAccessories();
        for (int i = 0; i < stacks.getContainerSize(); i++) {
            ItemStack stack = stacks.getItem(i);
            if (stack.is(ModItems.ROTOM_PHONE)) return true;
        }
        return false;
    }

    public static ItemStack getPhoneFromSlot(Player player) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) return ItemStack.EMPTY;

        Map<String, AccessoriesContainer> containers = capability.getContainers();
        AccessoriesContainer phoneContainer = containers.get(PHONE_SLOT_KEY);
        if (phoneContainer == null) return ItemStack.EMPTY;

        var stacks = phoneContainer.getAccessories();
        for (int i = 0; i < stacks.getContainerSize(); i++) {
            ItemStack stack = stacks.getItem(i);
            if (stack.is(ModItems.ROTOM_PHONE)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
