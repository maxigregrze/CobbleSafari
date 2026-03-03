package maxigregrze.cobblesafari.manager;

import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.config.DimensionalBanData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class BannedItemsManager {

    private BannedItemsManager() {}

    public static boolean isItemBanned(ResourceKey<Level> dimension, Item item) {
        String dimensionId = dimension.location().toString();
        DimensionalBanData.DimensionRestrictions restrictions = DimensionalBanConfig.getEffectiveData().dimensions.get(dimensionId);

        if (restrictions == null) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        return restrictions.bannedItems.contains(itemId.toString());
    }

    public static boolean isBlockBanned(ResourceKey<Level> dimension, Block block) {
        String dimensionId = dimension.location().toString();
        DimensionalBanData.DimensionRestrictions restrictions = DimensionalBanConfig.getEffectiveData().dimensions.get(dimensionId);

        if (restrictions == null) {
            return false;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        return restrictions.bannedBlocks.contains(blockId.toString());
    }

    public static boolean isBlockBreakingAllowed(ResourceKey<Level> dimension) {
        String dimensionId = dimension.location().toString();
        DimensionalBanData.DimensionRestrictions restrictions = DimensionalBanConfig.getEffectiveData().dimensions.get(dimensionId);

        if (restrictions == null) {
            return true;
        }

        return restrictions.allowBlockBreaking;
    }

    public static boolean isBlockPlacingAllowed(ResourceKey<Level> dimension) {
        String dimensionId = dimension.location().toString();
        DimensionalBanData.DimensionRestrictions restrictions = DimensionalBanConfig.getEffectiveData().dimensions.get(dimensionId);

        if (restrictions == null) {
            return true;
        }

        return restrictions.allowBlockPlacing;
    }

    public static boolean isBattleAllowed(ResourceKey<Level> dimension) {
        String dimensionId = dimension.location().toString();
        DimensionalBanData.DimensionRestrictions restrictions = DimensionalBanConfig.getEffectiveData().dimensions.get(dimensionId);

        if (restrictions == null) {
            return true;
        }

        return restrictions.allowBattle;
    }

    public static boolean hasDimensionRestrictions(ResourceKey<Level> dimension) {
        String dimensionId = dimension.location().toString();
        return DimensionalBanConfig.getEffectiveData().dimensions.containsKey(dimensionId);
    }
}
