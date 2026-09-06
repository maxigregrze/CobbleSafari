package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
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

    public static final ResourceKey<Level> DUNGEON_DISTORTION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_distortion")
    );

    public static final ResourceKey<Level> DUNGEON_MEGALOPOLIS = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_megalopolis")
    );

    /** The sub-dungeon (action plan 183, B1). Registered like any other, excluded in two places. */
    public static final ResourceKey<Level> DUNGEON_ULTRASPACE = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_ultraspace")
    );

    private static final List<DungeonConfig> REGISTERED_DUNGEONS = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static int totalWeight = 0;

    public static void register() {
        REGISTERED_DUNGEONS.add(new DungeonConfig(
                "dungeon_underground",
                DUNGEON_UNDERGROUND,
                "cobblesafari:underground/start",
                9, 3, 3,
                900,
                1,
                DungeonConfig.StructureType.JIGSAW,
                5,
                512,   
                1,      
                1      
        ));

        REGISTERED_DUNGEONS.add(new DungeonConfig(
                "dungeon_jump",
                DUNGEON_JUMP,
                "cobblesafari:jump/entrance",
                0, 0, 0,   // spawn offsets unused for a JIGSAW dungeon
                900,
                1,
                DungeonConfig.StructureType.JIGSAW,
                12,        // jigsaw depth: entrance -> 3f..10f -> roof is 9 deep
                1024,      // zoneSize, same spacing as the distortion world
                0,         // clearSectionsBelow: clearing starts at Y 0, sparing the scenery floor at -8/-7
                10         // clearSectionsAbove: up to Y 175, the 155-block tower tops out at 154
        ).withFixedStructureY(8));

        REGISTERED_DUNGEONS.add(new DungeonConfig(
                "dungeon_distortion",
                DUNGEON_DISTORTION,
                "cobblesafari:distortion/entrance",
                9, 3, 3,
                900,
                1,
                DungeonConfig.StructureType.JIGSAW,
                20,
                1024,    
                2,      
                10      
        ));

        // The wormhole's host (action plan 183, section 3.3). An ordinary dungeon in every
        // respect; the only thing that distinguishes it is that its pool carries a wormhole room.
        REGISTERED_DUNGEONS.add(new DungeonConfig(
                "dungeon_megalopolis",
                DUNGEON_MEGALOPOLIS,
                "cobblesafari:megalopolis/entrance",
                0, 0, 0,   // spawn offsets unused for a JIGSAW dungeon
                900,
                1,
                DungeonConfig.StructureType.JIGSAW,
                10,        // jigsaw depth
                512,       // zoneSize - the slot grid pitch
                1,         // clearSectionsBelow
                10         // clearSectionsAbove
        ));

        // The sub-dungeon (action plan 183, section 3.4). structureId is deliberately empty: the
        // pool comes from the destination file, supplied by the caller at generation time.
        // weight 0 is redundant with the two exclusions below, and deliberately so.
        REGISTERED_DUNGEONS.add(new DungeonConfig(
                "dungeon_ultraspace",
                DUNGEON_ULTRASPACE,
                "",
                0, 0, 0,
                300,
                0,
                DungeonConfig.StructureType.JIGSAW,
                5,
                512, 1, 10
        ).withFixedStructureY(64).asSubDungeon());

        recalculateTotalWeight();
        CobbleSafari.LOGGER.info("Registered {} dungeon dimensions with total weight {}", 
                REGISTERED_DUNGEONS.size(), totalWeight);
    }

    private static void recalculateTotalWeight() {
        totalWeight = REGISTERED_DUNGEONS.stream()
                .filter(d -> !d.isSubDungeon())
                .mapToInt(DungeonConfig::getWeight)
                .sum();
    }

    public static DungeonConfig getRandomDungeon() {
        List<DungeonConfig> enabledDungeons = new ArrayList<>();
        int enabledTotalWeight = 0;

        for (DungeonConfig dungeon : REGISTERED_DUNGEONS) {
            // A sub-dungeon is never a Hoopa portal destination (action plan 183, section 3.2).
            if (dungeon.isSubDungeon()) continue;

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

    /**
     * A dungeon dimension registered <em>and</em> managed by the mod: it owns
     * {@code DungeonInstanceRecord}s, so a timer session can be scoped to one of its instances
     * (action plan 181, V1). Externally managed dungeons have no mod-side record and are therefore
     * never instanced for timer purposes.
     * <p>
     * This is the derivation behind {@code DimensionTimerEntry.isInstanced()}. It is read lazily
     * rather than stamped into the config so that dungeons registered through
     * {@code DungeonRegistrationAPI} after start-up answer correctly too.
     */
    public static boolean isInstancedDungeonDimension(String dimensionId) {
        return REGISTERED_DUNGEONS.stream()
                .anyMatch(d -> d.getDimensionId().equals(dimensionId) && !d.isExternallyManaged());
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

    public static List<String> getRegisteredDungeonIdsSorted() {
        return REGISTERED_DUNGEONS.stream()
                .map(DungeonConfig::getId)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /**
     * Dungeons a Hoopa portal may legitimately be pointed at: enabled, and <b>not</b> a sub-dungeon.
     *
     * <p>A sub-dungeon is reached from inside another dungeon and its structure comes from a
     * destination file, not from its {@code DungeonConfig}. A portal aimed at one generates nothing
     * and drops the player into an empty region, so it is excluded from the command's suggestions
     * and refused outright by the command itself (action plan 183, section 3.2).</p>
     */
    public static List<String> getPortalDestinationIdsSorted() {
        return REGISTERED_DUNGEONS.stream()
                .filter(d -> !d.isSubDungeon())
                .filter(d -> PortalSpawnConfig.getDimensionConfig(d.getId())
                        .map(DungeonDimensionEntry::isEnabled)
                        .orElse(true))
                .map(DungeonConfig::getId)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public static List<String> getEnabledDungeonIdsSorted() {
        return REGISTERED_DUNGEONS.stream()
                .filter(d -> PortalSpawnConfig.getDimensionConfig(d.getId())
                        .map(DungeonDimensionEntry::isEnabled)
                        .orElse(true))
                .map(DungeonConfig::getId)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public static void addDungeon(DungeonConfig config) {
        REGISTERED_DUNGEONS.add(config);
        recalculateTotalWeight();
    }

    public static int getTotalWeight() {
        return totalWeight;
    }
}
