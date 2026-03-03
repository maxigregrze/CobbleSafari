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
    private static int totalWeight = 0;

    public static void register(TreasureDefinition def) {
        TREASURES.put(def.getId(), def);
        TREASURE_LIST.add(def);
        totalWeight += def.getWeight();
    }

    public static void clear() {
        TREASURES.clear();
        TREASURE_LIST.clear();
        totalWeight = 0;
    }

    public static TreasureDefinition getById(String id) {
        return TREASURES.get(id);
    }

    public static List<TreasureDefinition> getAllTreasures() {
        return new ArrayList<>(TREASURE_LIST);
    }

    public static TreasureDefinition getRandomTreasure(Random random) {
        if (TREASURE_LIST.isEmpty() || totalWeight <= 0) {
            CobbleSafari.LOGGER.warn("[TreasureRegistry] No treasures loaded, cannot pick random treasure");
            return null;
        }
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (TreasureDefinition t : TREASURE_LIST) {
            cumulative += t.getWeight();
            if (roll < cumulative) {
                return t;
            }
        }
        return TREASURE_LIST.get(TREASURE_LIST.size() - 1);
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
                    null, entry.weight(), entry.minQty(), entry.maxQty()
            );
            register(def);
        }
        CobbleSafari.LOGGER.info("[TreasureRegistry] Client sync applied: {} entries (existing definitions kept for reward items)", entries.size());
    }

    public static void init() {
        CobbleSafari.LOGGER.info("[TreasureRegistry] Initialized (awaiting datapack load)");
    }
}
