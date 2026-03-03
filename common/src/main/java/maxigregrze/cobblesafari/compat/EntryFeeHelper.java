package maxigregrze.cobblesafari.compat;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EntryFeeHelper {

    public enum FeeType {
        NONE,
        ITEM,
        COBBLEDOLLAR
    }

    private EntryFeeHelper() {}

    public static FeeType getEffectiveFeeType(boolean enableFee, boolean useCobbledollar) {
        if (!enableFee) {
            return FeeType.NONE;
        }
        if (useCobbledollar && CobbleDollarHelper.isAvailable()) {
            return FeeType.COBBLEDOLLAR;
        }
        return FeeType.ITEM;
    }

    public static boolean tryChargeFee(ServerPlayer player, boolean enableFee, String itemId,
                                       boolean useCobbledollar, int amount) {
        FeeType feeType = getEffectiveFeeType(enableFee, useCobbledollar);

        switch (feeType) {
            case NONE:
                return true;

            case COBBLEDOLLAR:
                if (CobbleDollarHelper.tryRemove(player, amount)) {
                    CobbleSafari.LOGGER.info("Player {} paid {} CobbleDollars entry fee",
                            player.getName().getString(), amount);
                    return true;
                }
                CobbleSafari.LOGGER.debug("Player {} has insufficient CobbleDollars for fee of {}",
                        player.getName().getString(), amount);
                return false;

            case ITEM:
                Item item = resolveItem(itemId);
                if (item == Items.AIR) {
                    CobbleSafari.LOGGER.error("Invalid entry fee item: {}", itemId);
                    return true;
                }
                if (countItemInInventory(player, item) >= 1) {
                    removeItemsFromInventory(player, item, 1);
                    CobbleSafari.LOGGER.info("Player {} paid 1x {} as entry fee",
                            player.getName().getString(), itemId);
                    return true;
                }
                CobbleSafari.LOGGER.debug("Player {} does not have 1x {} for entry fee",
                        player.getName().getString(), itemId);
                return false;

            default:
                return true;
        }
    }

    public static boolean hasEnoughForFee(ServerPlayer player, boolean enableFee, String itemId,
                                          boolean useCobbledollar, int amount) {
        FeeType feeType = getEffectiveFeeType(enableFee, useCobbledollar);

        switch (feeType) {
            case NONE:
                return true;
            case COBBLEDOLLAR:
                return CobbleDollarHelper.hasBalance(player, amount);
            case ITEM:
                Item item = resolveItem(itemId);
                if (item == Items.AIR) return true;
                return countItemInInventory(player, item) >= amount;
            default:
                return true;
        }
    }

    public static boolean removeItemsFromInventory(ServerPlayer player, Item item, int count) {
        int remaining = count;

        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }

        return remaining <= 0;
    }

    public static int countItemInInventory(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static Item resolveItem(String itemId) {
        try {
            ResourceLocation location = ResourceLocation.parse(itemId);
            return BuiltInRegistries.ITEM.get(location);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("Failed to resolve item: {}", itemId, e);
            return Items.AIR;
        }
    }
}
