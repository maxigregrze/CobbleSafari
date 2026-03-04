package maxigregrze.cobblesafari.underground.logic;

import maxigregrze.cobblesafari.CobbleSafari;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TreasureRegistry {

    private TreasureRegistry() {}

    private static final Map<String, TreasureDefinition> TREASURES = new HashMap<>();
    private static final List<TreasureDefinition> TREASURE_LIST = new ArrayList<>();

    public static void register(TreasureDefinition def) {
        TREASURES.put(def.getId(), def);
        TREASURE_LIST.add(def);
    }

    public static void clear() {
        TREASURES.clear();
        TREASURE_LIST.clear();
    }

    public static TreasureDefinition getById(String id) {
        return TREASURES.get(id);
    }

    public static List<TreasureDefinition> getAllTreasures() {
        return new ArrayList<>(TREASURE_LIST);
    }

    public static TreasureDefinition getRandomTreasure(Random random) {
        return getRandomTreasure(random, 0);
    }

    public static TreasureDefinition getRandomTreasure(Random random, int luckLevel) {
        if (TREASURE_LIST.isEmpty()) {
            CobbleSafari.LOGGER.warn("[TreasureRegistry] No treasures loaded, cannot pick random treasure");
            return null;
        }
        int effectiveLuck = Math.max(-10, luckLevel);
        List<TreasureDefinition> eligible = new ArrayList<>();
        int totalEffectiveWeight = 0;
        for (TreasureDefinition t : TREASURE_LIST) {
            if (t.isDisabled()) continue;
            int effectiveWeight = t.getWeight() + effectiveLuck;
            if (effectiveWeight <= 0) continue;
            eligible.add(t);
            totalEffectiveWeight += effectiveWeight;
        }
        if (eligible.isEmpty() || totalEffectiveWeight <= 0) {
            CobbleSafari.LOGGER.warn("[TreasureRegistry] No eligible treasures for luck level {}", luckLevel);
            return null;
        }
        int roll = random.nextInt(totalEffectiveWeight);
        int cumulative = 0;
        for (TreasureDefinition t : eligible) {
            cumulative += t.getWeight() + effectiveLuck;
            if (roll < cumulative) {
                return t;
            }
        }
        return eligible.get(eligible.size() - 1);
    }

    public static int getTreasureCount() {
        return TREASURE_LIST.size();
    }

    public static void applyClientSync(java.util.List<maxigregrze.cobblesafari.underground.network.UndergroundPayloads.TreasureEntryData> entries) {
        for (var entry : entries) {
            ShapeRegistry.registerClientShape(entry.id(), entry.shapeMatrix());
            if (TREASURES.containsKey(entry.id())) {
                continue;
            }
            TreasureDefinition def = new TreasureDefinition(
                    entry.id(), entry.textureId(), entry.shapeMatrix(),
                    null, entry.weight(), entry.minQty(), entry.maxQty(), entry.isDisabled()
            );
            register(def);
        }
        CobbleSafari.LOGGER.info("[TreasureRegistry] Client sync applied: {} entries (existing definitions kept for reward items)", entries.size());
    }

    public static void init() {
        CobbleSafari.LOGGER.info("[TreasureRegistry] Initialized (awaiting datapack load)");
    }
}
