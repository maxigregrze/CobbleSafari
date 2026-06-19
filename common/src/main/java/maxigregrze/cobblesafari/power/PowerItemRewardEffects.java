package maxigregrze.cobblesafari.power;

import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Reads the player's active {@code big_haul} / {@code item} donut power effects and exposes
 * the numeric bonuses applied to the item-reward blocks (Lost Item, Auspicious Pokéballs).
 * Mirrors the lookup style of {@code PowerSalvageLootHandler}.
 */
public final class PowerItemRewardEffects {

    private PowerItemRewardEffects() {}

    /** Extra item rolls granted by big_haul (highest active level wins; no stacking). */
    public static int bigHaulExtraItems(ServerPlayer player) {
        for (int lv = 3; lv >= 1; lv--) {
            if (player.hasEffect(ModPowerEffects.bigHaul(lv))) {
                var es = SpawnBoostConfig.data.effectSettings;
                return switch (lv) {
                    case 1 -> es.bigHaulPowerLevel1ExtraItems;
                    case 2 -> es.bigHaulPowerLevel2ExtraItems;
                    case 3 -> es.bigHaulPowerLevel3ExtraItems;
                    default -> 0;
                };
            }
        }
        return 0;
    }

    /**
     * Weight bonus for a single pool category (0=berry, 1=candy, 2=balls, 3=treasures),
     * matching the reward block entities' {@code getPoolIdForCategory(int)} order.
     * Highest active level for that category wins; categories are independent.
     */
    public static int itemWeightBonus(ServerPlayer player, int categoryIndex) {
        if (categoryIndex < 0 || categoryIndex >= ItemCategoryVariantRegistry.COUNT) {
            return 0;
        }
        for (int lv = 3; lv >= 1; lv--) {
            if (player.hasEffect(ModPowerEffects.item(categoryIndex, lv))) {
                var es = SpawnBoostConfig.data.effectSettings;
                return switch (lv) {
                    case 1 -> es.itemPowerLevel1WeightBonus;
                    case 2 -> es.itemPowerLevel2WeightBonus;
                    case 3 -> es.itemPowerLevel3WeightBonus;
                    default -> 0;
                };
            }
        }
        return 0;
    }
}
