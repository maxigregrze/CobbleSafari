package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonDimensions {

    private DungeonDimensions() {}

    public static final ResourceKey<Level> DUNGEON_JUMP = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_jump")
    );

    public static final ResourceKey<Level> DUNGEON_UNDERGROUND = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_underground")
    );

    private static final List<DungeonConfig> REGISTERED_DUNGEONS = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static int totalWeight = 0;

    public static void register() {
        /* REGISTERED_DUNGEONS.add(new DungeonConfig(
                "dungeon_jump",
                DUNGEON_JUMP,
                "cobblesafari:test_dungeon",
                16, 3, 2,
                1800,
                1
        )); */

        REGISTERED_DUNGEONS.add(new DungeonConfig(
                "dungeon_underground",
                DUNGEON_UNDERGROUND,
                "cobblesafari:underground/start",
                9, 3, 3,
                900,
                1,
                DungeonConfig.StructureType.JIGSAW,
                5
        ));

        recalculateTotalWeight();
        CobbleSafari.LOGGER.info("Registered {} dungeon dimensions with total weight {}", 
                REGISTERED_DUNGEONS.size(), totalWeight);
    }

    private static void recalculateTotalWeight() {
        totalWeight = REGISTERED_DUNGEONS.stream()
                .mapToInt(DungeonConfig::getWeight)
                .sum();
    }

    public static DungeonConfig getRandomDungeon() {
        List<DungeonConfig> enabledDungeons = new ArrayList<>();
        int enabledTotalWeight = 0;

        for (DungeonConfig dungeon : REGISTERED_DUNGEONS) {
            java.util.Optional<DungeonDimensionEntry> dimConfig =
                    PortalSpawnConfig.getDimensionConfig(dungeon.getId());

            boolean enabled = dimConfig.map(DungeonDimensionEntry::isEnabled).orElse(true);
            if (!enabled) continue;

            int weight = dimConfig.map(DungeonDimensionEntry::getSpawnWeight)
                    .orElse(dungeon.getWeight());

            enabledDungeons.add(dungeon);
            enabledTotalWeight += weight;
        }

        if (enabledDungeons.isEmpty() || enabledTotalWeight <= 0) {
            return null;
        }

        int randomValue = RANDOM.nextInt(enabledTotalWeight);
        int currentWeight = 0;

        for (DungeonConfig dungeon : enabledDungeons) {
            java.util.Optional<DungeonDimensionEntry> dimConfig =
                    PortalSpawnConfig.getDimensionConfig(dungeon.getId());
            int weight = dimConfig.map(DungeonDimensionEntry::getSpawnWeight)
                    .orElse(dungeon.getWeight());

            currentWeight += weight;
            if (randomValue < currentWeight) {
                return dungeon;
            }
        }

        return enabledDungeons.get(enabledDungeons.size() - 1);
    }

    public static DungeonConfig getDungeonById(String id) {
        return REGISTERED_DUNGEONS.stream()
                .filter(d -> d.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static boolean isDungeonDimension(String dimensionId) {
        return REGISTERED_DUNGEONS.stream()
                .anyMatch(d -> d.getDimensionId().equals(dimensionId));
    }

    public static DungeonConfig getDungeonByDimension(ResourceKey<Level> dimension) {
        return REGISTERED_DUNGEONS.stream()
                .filter(d -> d.getDimensionKey().equals(dimension))
                .findFirst()
                .orElse(null);
    }

    public static List<DungeonConfig> getAllDungeons() {
        return new ArrayList<>(REGISTERED_DUNGEONS);
    }

    public static void addDungeon(DungeonConfig config) {
        REGISTERED_DUNGEONS.add(config);
        recalculateTotalWeight();
    }

    public static int getTotalWeight() {
        return totalWeight;
    }
}
