package maxigregrze.cobblesafari.item.donut;

import com.cobblemon.mod.common.api.cooking.Flavour;

import maxigregrze.cobblesafari.init.ModComponents;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public final class DonutFlavorLogic {

    private static final Random RNG = new Random();

    private static final float[] CALORIE_MULTIPLIERS = {1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1.0f};

    private static final int[][] GENERATION_RANGES = {
            {50, 220, 1, 1},
            {210, 290, 2, 3},
            {275, 430, 3, 4},
            {395, 450, 4, 7},
            {410, 440, 6, 8},
            {400, 405, 9, 9},
    };

    private static final int PRIMARY_ONLY_MAX_TIER = 2;

    private DonutFlavorLogic() {}

    public static int tierFromBonusLevelSum(int sum) {
        if (sum <= 0) {
            return 0;
        }
        if (sum == 1) {
            return 0;
        }
        if (sum == 2) {
            return 1;
        }
        if (sum <= 4) {
            return 2;
        }
        if (sum <= 6) {
            return 3;
        }
        if (sum <= 8) {
            return 4;
        }
        return 5;
    }

    public static boolean isNoneBonusSlot(String raw) {
        return raw != null && raw.trim().equalsIgnoreCase("none");
    }

    public static Optional<DonutBonus> parseBonusSpec(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String t = raw.trim();
        String powerId;
        int level;
        int type;
        if (t.contains(":")) {
            String[] p = t.split(":", 3);
            if (p.length != 3) {
                return Optional.empty();
            }
            powerId = p[0].trim();
            try {
                level = Integer.parseInt(p[1].trim());
                type = Integer.parseInt(p[2].trim());
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        } else {
            int last = t.lastIndexOf('_');
            if (last <= 0) {
                return Optional.empty();
            }
            int second = t.lastIndexOf('_', last - 1);
            if (second < 0) {
                return Optional.empty();
            }
            powerId = t.substring(0, second);
            try {
                level = Integer.parseInt(t.substring(second + 1, last));
                type = Integer.parseInt(t.substring(last + 1));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        DonutPower power = DonutPowerRegistry.get(powerId);
        if (power == null) {
            return Optional.empty();
        }
        if (level < 1 || level > 3) {
            return Optional.empty();
        }
        if (power.typeNbr() <= 1) {
            type = 0;
        } else if (type < 0 || type >= power.typeNbr() || type >= PowerVariantRegistry.VARIANT_COUNT) {
            return Optional.empty();
        }
        DonutBonus bonus = new DonutBonus(power.id(), level, type);
        if (ModPowerEffects.resolveBonus(bonus) == null) {
            return Optional.empty();
        }
        return Optional.of(bonus);
    }

    public static void validateDistinctFlavorCategory(List<DonutBonus> bonuses) {
        for (int i = 0; i < bonuses.size(); i++) {
            DonutPower pi = Objects.requireNonNull(DonutPowerRegistry.get(bonuses.get(i).powerId()));
            for (int j = i + 1; j < bonuses.size(); j++) {
                DonutPower pj = Objects.requireNonNull(DonutPowerRegistry.get(bonuses.get(j).powerId()));
                if (pi.flavor() == pj.flavor() && pi.category() == pj.category()) {
                    throw new IllegalArgumentException("duplicate_flavor_category");
                }
            }
        }
    }

    public static DonutFlavorComponent fromCustomBonuses(List<DonutBonus> bonuses, Random rng) {
        if (bonuses.isEmpty()) {
            throw new IllegalArgumentException("empty_bonuses");
        }
        int sum = 0;
        for (DonutBonus b : bonuses) {
            sum += b.level();
        }
        if (sum < 1 || sum > 9) {
            throw new IllegalArgumentException("invalid_level_sum");
        }
        validateDistinctFlavorCategory(bonuses);
        int tier = tierFromBonusLevelSum(sum);
        DonutMainFlavor flavor = resolveCustomDonutFlavor(bonuses);
        int calories = rollCaloriesForTier(rng, tier);
        return new DonutFlavorComponent(flavor, tier, List.of(), calories, List.copyOf(bonuses));
    }

    private static DonutMainFlavor resolveCustomDonutFlavor(List<DonutBonus> bonuses) {
        DonutPower p1 = DonutPowerRegistry.get(bonuses.get(0).powerId());
        if (bonuses.size() == 1) {
            return p1.flavor();
        }
        if (bonuses.size() == 2) {
            return p1.flavor();
        }
        DonutPower p2 = DonutPowerRegistry.get(bonuses.get(1).powerId());
        if (p1.flavor() == p2.flavor()) {
            return p1.flavor();
        }
        return DonutMainFlavor.MIX;
    }

    public static int rollCaloriesForTier(Random rng, int tier) {
        int t = Math.max(0, Math.min(tier, DonutFlavorComponent.MAX_TIER));
        int[] r = GENERATION_RANGES[t];
        int calMin = r[0];
        int calMax = r[1];
        return calMin == calMax ? calMin : calMin + rng.nextInt(calMax - calMin + 1);
    }

    public static DonutFlavorComponent generateDonut(DonutMainFlavor flavor, int tier) {
        if (flavor == DonutMainFlavor.MIX) {
            throw new IllegalArgumentException("mix_not_supported");
        }
        int t = Math.max(0, Math.min(tier, DonutFlavorComponent.MAX_TIER));
        Random rng = RNG;

        int[] r = GENERATION_RANGES[t];
        int calMin = r[0];
        int calMax = r[1];
        int budgetMin = r[2];
        int budgetMax = r[3];

        int calories = calMin == calMax ? calMin : calMin + rng.nextInt(calMax - calMin + 1);

        int totalBudget = budgetMin == budgetMax ? budgetMin : budgetMin + rng.nextInt(budgetMax - budgetMin + 1);

        boolean primaryOnly = (t <= PRIMARY_ONLY_MAX_TIER);
        boolean forceMax = (totalBudget >= 9);

        int primaryBudget;
        int secondaryBudget;

        if (primaryOnly) {
            primaryBudget = totalBudget;
            secondaryBudget = 0;
        } else {
            primaryBudget = 1 + (totalBudget > 1 ? rng.nextInt(totalBudget) : 0);
            secondaryBudget = totalBudget - primaryBudget;
        }

        List<DonutPower> cat1Pool = new ArrayList<>(DonutPowerRegistry.byFlavorAndCategory(flavor, 1));
        List<DonutPower> cat2Pool = new ArrayList<>(DonutPowerRegistry.byFlavorAndCategory(flavor, 2));

        DonutBonus bonus1 = pickRandomWithType(rng, cat1Pool);

        DonutBonus bonus2 = (t == 0) ? null : pickRandomWithType(rng, cat2Pool);

        DonutBonus bonus3 = null;
        if (!primaryOnly) {
            List<DonutPower> secPool = DonutPowerRegistry.all().stream()
                    .filter(p -> p.flavor() != flavor)
                    .collect(Collectors.toCollection(ArrayList::new));
            bonus3 = secPool.isEmpty() ? null : pickRandomWithType(rng, secPool);
        }

        int[] levels = new int[3];

        if (forceMax) {
            if (bonus1 != null) {
                levels[0] = 3;
            }
            if (bonus2 != null) {
                levels[1] = 3;
            }
            if (bonus3 != null) {
                levels[2] = 3;
            }
        } else {
            int budget1 = primaryBudget;
            for (int i = 0; i < 2; i++) {
                if (budget1 == 0) {
                    break;
                }
                levels[i] = rollLevel(rng, Math.min(3, budget1));
                budget1 -= levels[i];
            }
            if (budget1 > 0) {
                if (budget1 >= 6) {
                    if (levels[0] > 0) {
                        levels[0] = 3;
                    }
                    if (levels[1] > 0) {
                        levels[1] = 3;
                    }
                } else {
                    distributeRemainingBudget(rng, new int[]{0, 1}, levels, budget1);
                }
            }

            if (bonus3 != null) {
                if (secondaryBudget == 0) {
                    bonus3 = null;
                } else {
                    levels[2] = rollLevel(rng, Math.min(3, secondaryBudget));
                    int rem = secondaryBudget - levels[2];
                    if (rem > 0) {
                        levels[2] = Math.min(3, levels[2] + rem);
                    }
                }
            }
        }

        List<DonutBonus> bonuses = new ArrayList<>();
        if (bonus1 != null && levels[0] > 0) {
            bonuses.add(new DonutBonus(bonus1.powerId(), levels[0], bonus1.type()));
        }
        if (bonus2 != null && levels[1] > 0) {
            bonuses.add(new DonutBonus(bonus2.powerId(), levels[1], bonus2.type()));
        }
        if (bonus3 != null && levels[2] > 0) {
            bonuses.add(new DonutBonus(bonus3.powerId(), levels[2], bonus3.type()));
        }

        return new DonutFlavorComponent(flavor, t, List.of(), calories, bonuses);
    }

    public static boolean isUnrolledShell(DonutFlavorComponent comp) {
        return comp != null && comp.calories() == 0 && comp.bonuses().isEmpty();
    }

    public static ItemStack createGeneratedStack(DonutMainFlavor flavor, int tier) {
        ItemStack stack = new ItemStack(ModItems.DONUT);
        stack.set(ModComponents.DONUT_FLAVOR, generateDonut(flavor, tier));
        return stack;
    }

    public static int computeTier(int sum) {
        if (sum < 150) return 0;
        if (sum < 210) return 1;
        if (sum < 330) return 2;
        if (sum < 375) return 3;
        if (sum < 400) return 4;
        return 5;
    }

    public static int perFlavorBudget(int flavorTotal) {
        if (flavorTotal <= 0) {
            return 0;
        }
        return Math.min(9, (flavorTotal + 44) / 45);
    }

    public static int computeCalories(int totalFlavor, int tier) {
        int t = Math.min(Math.max(tier, 0), CALORIE_MULTIPLIERS.length - 1);
        return Math.round(totalFlavor * CALORIE_MULTIPLIERS[t]);
    }

    public static DonutFlavorComponent fromCooking(Map<Flavour, Integer> flavours, List<ResourceLocation> inputBerries) {
        int spicy = flavours.getOrDefault(Flavour.SPICY, 0);
        int dry = flavours.getOrDefault(Flavour.DRY, 0);
        int sweet = flavours.getOrDefault(Flavour.SWEET, 0);
        int sour = flavours.getOrDefault(Flavour.SOUR, 0);
        int bitter = flavours.getOrDefault(Flavour.BITTER, 0);

        int sum = spicy + dry + sweet + sour + bitter;
        if (sum <= 0) {
            return null;
        }

        Map<DonutMainFlavor, Integer> flavorTotals = new EnumMap<>(DonutMainFlavor.class);
        flavorTotals.put(DonutMainFlavor.SPICY, spicy);
        flavorTotals.put(DonutMainFlavor.DRY, dry);
        flavorTotals.put(DonutMainFlavor.SWEET, sweet);
        flavorTotals.put(DonutMainFlavor.SOUR, sour);
        flavorTotals.put(DonutMainFlavor.BITTER, bitter);

        DonutMainFlavor mainFlavor = resolveDominantFlavor(flavorTotals);
        int tier = computeTier(sum);
        int calories = computeCalories(sum, tier);
        List<DonutBonus> bonuses = rollBonuses(mainFlavor, flavorTotals);

        return new DonutFlavorComponent(mainFlavor, tier, inputBerries, calories, bonuses);
    }

    private static DonutMainFlavor resolveDominantFlavor(Map<DonutMainFlavor, Integer> byFlavor) {
        int max = 0;
        for (int v : byFlavor.values()) {
            max = Math.max(max, v);
        }
        int tieCount = 0;
        DonutMainFlavor chosen = DonutMainFlavor.MIX;
        for (Map.Entry<DonutMainFlavor, Integer> entry : byFlavor.entrySet()) {
            if (entry.getValue() == max) {
                tieCount++;
                chosen = entry.getKey();
            }
        }
        return tieCount > 1 ? DonutMainFlavor.MIX : chosen;
    }

    public static List<DonutBonus> rollBonuses(DonutMainFlavor mainFlavor, Map<DonutMainFlavor, Integer> flavorTotals) {
        Random rng = RNG;

        Map<DonutMainFlavor, Integer> budgets = new EnumMap<>(DonutMainFlavor.class);
        for (Map.Entry<DonutMainFlavor, Integer> entry : flavorTotals.entrySet()) {
            budgets.put(entry.getKey(), perFlavorBudget(entry.getValue()));
        }

        if (mainFlavor == DonutMainFlavor.MIX) {
            return rollBonusesMix(rng, budgets);
        }
        return rollBonusesNormal(rng, mainFlavor, budgets, flavorTotals);
    }

    private static int rollLevel(Random rng, int max) {
        return 1 + rng.nextInt(max);
    }

    private static void distributeRemainingBudget(Random rng, int[] indices, int[] levels, int budget) {
        while (budget > 0) {
            List<Integer> eligible = new ArrayList<>();
            for (int idx : indices) {
                if (levels[idx] > 0 && levels[idx] < 3) {
                    eligible.add(idx);
                }
            }
            if (eligible.isEmpty()) {
                break;
            }
            int pick = eligible.get(rng.nextInt(eligible.size()));
            levels[pick]++;
            budget--;
        }
    }

    private static DonutBonus pickRandomWithType(Random rng, List<DonutPower> pool) {
        if (pool.isEmpty()) {
            return null;
        }
        DonutPower power = pool.remove(rng.nextInt(pool.size()));
        int type = rng.nextInt(power.typeNbr());
        return new DonutBonus(power.id(), 0, type);
    }

    private static DonutMainFlavor pickSecondaryFlavor(
            Map<DonutMainFlavor, Integer> flavorTotals,
            DonutMainFlavor primary) {
        return flavorTotals.entrySet().stream()
                .filter(e -> e.getKey() != primary && e.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static List<DonutBonus> rollBonusesNormal(
            Random rng,
            DonutMainFlavor primary,
            Map<DonutMainFlavor, Integer> budgets,
            Map<DonutMainFlavor, Integer> flavorTotals) {

        DonutMainFlavor secondary = pickSecondaryFlavor(flavorTotals, primary);

        List<DonutPower> cat1Pool = new ArrayList<>(DonutPowerRegistry.byFlavorAndCategory(primary, 1));
        List<DonutPower> cat2Pool = new ArrayList<>(DonutPowerRegistry.byFlavorAndCategory(primary, 2));

        DonutBonus bonus1 = pickRandomWithType(rng, cat1Pool);
        DonutBonus bonus2 = pickRandomWithType(rng, cat2Pool);
        DonutBonus bonus3 = null;
        if (secondary != null) {
            List<DonutPower> secPool = new ArrayList<>(DonutPowerRegistry.byFlavor(secondary));
            bonus3 = pickRandomWithType(rng, secPool);
        }

        int[] levels = new int[3];
        int budget1 = budgets.getOrDefault(primary, 0);

        for (int i = 0; i < 2; i++) {
            if (budget1 == 0) {
                break;
            }
            levels[i] = rollLevel(rng, Math.min(3, budget1));
            budget1 -= levels[i];
        }

        if (budget1 > 0) {
            if (budget1 >= 6) {
                if (levels[0] > 0) {
                    levels[0] = 3;
                }
                if (levels[1] > 0) {
                    levels[1] = 3;
                }
            } else {
                distributeRemainingBudget(rng, new int[]{0, 1}, levels, budget1);
            }
        }

        if (bonus3 != null) {
            int budget2 = budgets.getOrDefault(secondary, 0);
            if (budget2 == 0) {
                bonus3 = null;
            } else {
                levels[2] = rollLevel(rng, Math.min(3, budget2));
                budget2 -= levels[2];
                if (budget2 > 0) {
                    if (budget2 >= 3) {
                        levels[2] = 3;
                    } else {
                        levels[2] = Math.min(3, levels[2] + budget2);
                    }
                }
            }
        }

        List<DonutBonus> result = new ArrayList<>();
        if (bonus1 != null && levels[0] > 0) {
            result.add(new DonutBonus(bonus1.powerId(), levels[0], bonus1.type()));
        }
        if (bonus2 != null && levels[1] > 0) {
            result.add(new DonutBonus(bonus2.powerId(), levels[1], bonus2.type()));
        }
        if (bonus3 != null && levels[2] > 0) {
            result.add(new DonutBonus(bonus3.powerId(), levels[2], bonus3.type()));
        }
        return result;
    }

    private static List<DonutBonus> rollBonusesMix(Random rng, Map<DonutMainFlavor, Integer> budgets) {
        int totalBudget = budgets.values().stream().mapToInt(Integer::intValue).sum() + 1;
        totalBudget = Math.min(9, totalBudget);

        boolean forceMax = totalBudget >= 9;

        List<DonutMainFlavor> ranked = new ArrayList<>(budgets.keySet());
        Collections.shuffle(ranked, rng);
        ranked.sort((a, b) -> Integer.compare(budgets.get(b), budgets.get(a)));

        DonutBonus bonus1;
        DonutBonus bonus2;
        DonutBonus bonus3;
        boolean caseA = budgets.get(ranked.get(2)) == 0;

        if (caseA) {
            List<DonutPower> cat1 = new ArrayList<>(DonutPowerRegistry.byFlavorAndCategory(ranked.get(0), 1));
            List<DonutPower> cat2 = new ArrayList<>(DonutPowerRegistry.byFlavorAndCategory(ranked.get(0), 2));
            List<DonutPower> sec = new ArrayList<>(DonutPowerRegistry.byFlavor(ranked.get(1)));

            bonus1 = pickRandomWithType(rng, cat1);
            bonus2 = pickRandomWithType(rng, cat2);
            bonus3 = pickRandomWithType(rng, sec);
        } else {
            List<DonutPower> pool1 = new ArrayList<>(DonutPowerRegistry.byFlavor(ranked.get(0)));
            List<DonutPower> pool2 = new ArrayList<>(DonutPowerRegistry.byFlavor(ranked.get(1)));
            List<DonutPower> pool3 = new ArrayList<>(DonutPowerRegistry.byFlavor(ranked.get(2)));

            bonus1 = pickRandomWithType(rng, pool1);
            bonus2 = pickRandomWithType(rng, pool2);
            bonus3 = pickRandomWithType(rng, pool3);
        }

        int[] levels = new int[3];

        if (forceMax) {
            if (bonus1 != null) {
                levels[0] = 3;
            }
            if (bonus2 != null) {
                levels[1] = 3;
            }
            if (bonus3 != null) {
                levels[2] = 3;
            }
        } else {
            int budget = totalBudget;
            DonutBonus[] bonuses = {bonus1, bonus2, bonus3};
            for (int i = 0; i < 3; i++) {
                if (bonuses[i] == null || budget == 0) {
                    continue;
                }
                levels[i] = rollLevel(rng, Math.min(3, budget));
                budget -= levels[i];
            }
            if (budget > 0) {
                distributeRemainingBudget(rng, new int[]{0, 1, 2}, levels, budget);
            }
        }

        List<DonutBonus> result = new ArrayList<>();
        if (bonus1 != null && levels[0] > 0) {
            result.add(new DonutBonus(bonus1.powerId(), levels[0], bonus1.type()));
        }
        if (bonus2 != null && levels[1] > 0) {
            result.add(new DonutBonus(bonus2.powerId(), levels[1], bonus2.type()));
        }
        if (bonus3 != null && levels[2] > 0) {
            result.add(new DonutBonus(bonus3.powerId(), levels[2], bonus3.type()));
        }
        return result;
    }
}
