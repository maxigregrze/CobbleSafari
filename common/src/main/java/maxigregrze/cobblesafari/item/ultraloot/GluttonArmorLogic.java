package maxigregrze.cobblesafari.item.ultraloot;

import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Glutton armor and Gluttonous effect logic (action plan 192, section 11). The bonuses are real
 * vanilla effects applied hidden (ambient, no particles, no icon), so only Gluttonous shows and any
 * mod reading {@code hasEffect(DAMAGE_RESISTANCE)} still sees them.
 */
public final class GluttonArmorLogic {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int GLUTTONOUS_TICKS = 1200;
    private static final int BONUS_TICKS = 100;
    /** Refresh window: 100 down to 41 always holds a multiple of 50 and of 25, so Regeneration ticks. */
    private static final int BONUS_REFRESH_BELOW = 40;

    /** Resistance / Regeneration / Strength levels (0 = none) per food tier, highest tier first. */
    private static final int[][] TIERS = {
            {20, 4, 2, 2},
            {15, 3, 2, 2},
            {10, 2, 2, 1},
            {5, 2, 1, 1},
            {1, 1, 1, 0},
            {0, 1, 0, 0}
    };

    /**
     * Peaceful only: the fullness Gluttonous measures, since vanilla refills the food bar there (+1 food
     * per half second) and the tier would never move. Set to the food level when eating, lowered by one
     * point per drain interval. Weak keys: an entry goes with its player object; after a relog the
     * real food bar is used until the next meal.
     */
    private static final Map<Player, Integer> PEACEFUL_FULLNESS = new WeakHashMap<>();

    private GluttonArmorLogic() {}

    private static boolean isPeaceful(Player player) {
        return player.level().getDifficulty() == Difficulty.PEACEFUL;
    }

    public static int piecesWorn(Player player) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof GlutonArmorItem) {
                count++;
            }
        }
        return count;
    }

    /** Called server side by {@code LivingEntityGluttonousEatMixin} once a player has eaten. */
    public static void onEat(Player player) {
        int pieces = piecesWorn(player);
        if (pieces > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.GLUTTONOUS.holder, GLUTTONOUS_TICKS, pieces - 1));
            PEACEFUL_FULLNESS.put(player, player.getFoodData().getFoodLevel());
        }
    }

    /**
     * Removes one saturation point, or one food point when saturation is empty: the branch vanilla's
     * {@code FoodData#tick} takes once exhaustion passes 4. Done directly, because 4.0 exhaustion alone
     * never passes it ({@code > 4.0} is strict), which left the first drain silent and every later one
     * a full interval late. Like Hunger, it spares invulnerable players (creative, spectator) and never
     * takes food on Peaceful - there, the Peaceful fullness counter loses the point instead.
     */
    public static void drainOnePoint(Player player) {
        if (player.getAbilities().invulnerable) {
            return;
        }
        if (isPeaceful(player)) {
            PEACEFUL_FULLNESS.computeIfPresent(player, (p, fullness) -> Math.max(fullness - 1, 0));
        }
        FoodData food = player.getFoodData();
        if (food.getSaturationLevel() > 0.0f) {
            food.setSaturation(Math.max(food.getSaturationLevel() - 1.0f, 0.0f));
        } else if (player.level().getDifficulty() != Difficulty.PEACEFUL) {
            food.setFoodLevel(Math.max(food.getFoodLevel() - 1, 0));
        }
    }

    /** Re-applies the bonus tier matching the current food level (section 11.3). */
    public static void applyBonuses(Player player) {
        int food = player.getFoodData().getFoodLevel();
        if (isPeaceful(player)) {
            food = Math.min(food, PEACEFUL_FULLNESS.getOrDefault(player, food));
        }
        int[] tier = TIERS[TIERS.length - 1];
        for (int[] candidate : TIERS) {
            if (food >= candidate[0]) {
                tier = candidate;
                break;
            }
        }
        applyBonus(player, MobEffects.DAMAGE_RESISTANCE, tier[1]);
        applyBonus(player, MobEffects.REGENERATION, tier[2]);
        applyBonus(player, MobEffects.DAMAGE_BOOST, tier[3]);
    }

    private static void applyBonus(Player player, Holder<MobEffect> effect, int level) {
        MobEffectInstance current = player.getEffect(effect);
        boolean ours = current != null && isHiddenBonus(current);
        if (level <= 0) {
            if (ours) {
                player.removeEffect(effect);
            }
            return;
        }
        int amplifier = level - 1;
        if (current != null && !ours && current.getAmplifier() >= amplifier) {
            return; // a real effect at least as strong: leave it alone
        }
        if (!ours || current.getAmplifier() != amplifier || current.getDuration() <= BONUS_REFRESH_BELOW) {
            player.addEffect(new MobEffectInstance(effect, BONUS_TICKS, amplifier, true, false, false));
        }
    }

    private static boolean isHiddenBonus(MobEffectInstance instance) {
        return instance.isAmbient() && !instance.isVisible() && !instance.showIcon();
    }
}
