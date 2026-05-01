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
        return !getPhoneFromSlot(player).isEmpty();
    }

    public static ItemStack getPhoneFromSlot(Player player) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) {
            return ItemStack.EMPTY;
        }

        Map<String, AccessoriesContainer> containers = capability.getContainers();
        AccessoriesContainer preferred = containers.get(PHONE_SLOT_KEY);
        if (preferred != null) {
            ItemStack fromPreferred = firstRotomPhoneIn(preferred);
            if (!fromPreferred.isEmpty()) {
                return fromPreferred;
            }
        }

        for (AccessoriesContainer container : containers.values()) {
            if (preferred != null && container == preferred) {
                continue;
            }
            ItemStack found = firstRotomPhoneIn(container);
            if (!found.isEmpty()) {
                return found;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack firstRotomPhoneIn(AccessoriesContainer container) {
        var stacks = container.getAccessories();
        for (int i = 0; i < stacks.getContainerSize(); i++) {
            ItemStack stack = stacks.getItem(i);
            if (stack.is(ModItems.ROTOM_PHONE)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
