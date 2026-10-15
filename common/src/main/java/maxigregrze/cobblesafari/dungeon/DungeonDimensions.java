package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * The four sub-dungeons that own their dimension (action plan 187, § 5.3).
     *
     * <p>They differ from {@link #DUNGEON_ULTRASPACE} in exactly one respect: their dimension has a
     * generated floor, so they need their own biome source, their own sky and - above all - their
     * own {@code voidGuardY}. Everything else about them is an ordinary sub-dungeon.</p>
     */
    public static final ResourceKey<Level> DUNGEON_ULTRA_JUNGLE = subDimension("dungeon_ultra_jungle");
    public static final ResourceKey<Level> DUNGEON_ULTRA_DESERT = subDimension("dungeon_ultra_desert");
    public static final ResourceKey<Level> DUNGEON_ULTRA_PLANT = subDimension("dungeon_ultra_plant");
    public static final ResourceKey<Level> DUNGEON_ULTRA_CRATER = subDimension("dungeon_ultra_crater");

    private static ResourceKey<Level> subDimension(String path) {
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, path));
    }

    private static final List<DungeonConfig> REGISTERED_DUNGEONS = new ArrayList<>();

    /**
     * Index of {@link #REGISTERED_DUNGEONS} by dimension id (action plan 187, decision § 3.1).
     *
     * <p>The void guard now reads a <em>value</em> off the config instead of a boolean, and it runs
     * once per player per tick. Walking a stream for that - as {@code isDungeonDimension} and
     * {@code getDungeonByDimension} both do - is a cost that no longer needs paying. The map is
     * rebuilt wholesale by {@link #reindex()} on every mutation of the list, which happens at
     * start-up and on addon registration only.</p>
     */
    private static final Map<String, DungeonConfig> BY_DIMENSION_ID = new HashMap<>();

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
        // respect; the only thing that distinguishes it is that its pool carries wormhole rooms.
        //
        // Action plan 187 § 4.1 turns it into the top of the tallest tower of a generated city.
        // The structure is a single NBT, expressed as a one-element pool at depth 1 rather than as
        // StructureType.SIMPLE: the SIMPLE path places the exit portal at a blind offset facing
        // NORTH, while the jigsaw path reads the hoopa_ring_portal marker and its facing. Keeping
        // the marker costs nothing and is worth more than the depth.
        //
        // The two altitudes are PROVISIONAL, like the four below (decision § 5.1): the city floor
        // tops out at Y 3, its buildings are 32-48 tall, so the guard clears the rooftops and the
        // anchor sits just above them.
        REGISTERED_DUNGEONS.add(new DungeonConfig(
                "dungeon_megalopolis",
                DUNGEON_MEGALOPOLIS,
                "cobblesafari:megalopolis/entrance",
                0, 0, 0,   // spawn offsets unused for a JIGSAW dungeon
                900,
                1,
                DungeonConfig.StructureType.JIGSAW,
                1,         // jigsaw depth: a single piece
                512,       // zoneSize - the slot grid pitch
                1,         // clearSectionsBelow
                10         // clearSectionsAbove
        // Anchor 92, guard 58 - the same shape as the four sub-dungeons, one storey higher.
        //
        // The city floor is a single course of black concrete at Y 0 and its buildings reach about
        // Y 48, so a guard at 58 clears the rooftops: a player who steps off the tower is pulled
        // out above the skyline instead of landing on a roof they cannot leave. The anchor sits far
        // enough above that the whole occupied structure - including the wormhole devices the rider
        // returns to - stays above the line. That last point is the one that matters: a device
        // below the guard evacuates every return from Ultra Space on arrival, which reads as a
        // timer bug and is not one, so SubDungeonManager.leave logs an ERROR naming both numbers if
        // the structure ever ends up shaped otherwise.
        ).withFixedStructureY(92).withVoidGuardY(58).withWorldgenRestoration());

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

        // The four sub-dungeons that own their dimension (action plan 187, § 5; heights from the
        // delivered .nbt, plan 190 § 4.4).
        //
        // /place jigsaw sinks a single-piece start by one course (JigsawPlacement.addPieces moves it
        // by -groundLevelDelta), so the start jigsaw lands at anchor - 1 and:
        //
        //     anchor = floor + 1 + jigsaw Y in the .nbt      floor = first free block above ground
        //
        //   jungle  ground top 153, jigsaw 46 -> 201. Clouds (fixed at 192) cross the middle of the
        //           76-high structure; the guard is the top of the jungle decoration.
        //   desert  ground top 1,   jigsaw 10 -> 13.  Guard 3 below the jigsaw.
        //   crater  ground top 163, jigsaw 12 -> 177. Clouds 16 above the jigsaw; guard 3 below it.
        //   plant   unchanged from plan 187: it still straddles its guard by design.
        //
        // All four opt into worldgen restoration: the clear window runs to the bottom of the world,
        // through the ground, so the ground must be rebuilt behind it.
        registerUltraSubDungeon("dungeon_ultra_jungle", DUNGEON_ULTRA_JUNGLE, 201, 176);
        registerUltraSubDungeon("dungeon_ultra_desert", DUNGEON_ULTRA_DESERT, 13, 9);
        registerUltraSubDungeon("dungeon_ultra_plant", DUNGEON_ULTRA_PLANT, 35, 19);
        registerUltraSubDungeon("dungeon_ultra_crater", DUNGEON_ULTRA_CRATER, 177, 173);

        recalculateTotalWeight();
        reindex();
        CobbleSafari.LOGGER.info("Registered {} dungeon dimensions with total weight {}",
                REGISTERED_DUNGEONS.size(), totalWeight);
    }

    /**
     * One Ultra Space sub-dungeon with its own dimension. {@code structureId} is deliberately empty
     * for the same reason as {@code dungeon_ultraspace}: the pool comes from the destination file,
     * supplied by the caller at generation time. Weight 0 is redundant with {@code asSubDungeon()},
     * and deliberately so.
     */
    private static void registerUltraSubDungeon(String id, ResourceKey<Level> dimension,
                                                int anchorY, int voidGuardY) {
        REGISTERED_DUNGEONS.add(new DungeonConfig(
                id,
                dimension,
                "",
                0, 0, 0,
                300,
                0,
                DungeonConfig.StructureType.JIGSAW,
                5,
                512, 1, 10      // clearSections* kept for the signature: C2 measures them instead
        ).withFixedStructureY(anchorY).withVoidGuardY(voidGuardY)
         .withWorldgenRestoration().asSubDungeon());
    }

    /** Rebuilds {@link #BY_DIMENSION_ID}. Called on every mutation of the registry. */
    private static void reindex() {
        BY_DIMENSION_ID.clear();
        for (DungeonConfig dungeon : REGISTERED_DUNGEONS) {
            BY_DIMENSION_ID.put(dungeon.getDimensionId(), dungeon);
        }
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
        return BY_DIMENSION_ID.containsKey(dimensionId);
    }

    /**
     * The config registered for a dimension id, or {@code null}. Indexed rather than streamed
     * because the void guard calls it once per player per tick (action plan 187, decision § 3.1).
     */
    public static DungeonConfig getDungeonByDimensionId(String dimensionId) {
        return BY_DIMENSION_ID.get(dimensionId);
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
        DungeonConfig config = BY_DIMENSION_ID.get(dimensionId);
        return config != null && !config.isExternallyManaged();
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
        reindex();
    }

    public static int getTotalWeight() {
        return totalWeight;
    }
}
