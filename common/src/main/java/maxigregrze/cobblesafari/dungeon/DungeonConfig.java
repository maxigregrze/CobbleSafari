package maxigregrze.cobblesafari.dungeon;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DungeonConfig {

    public enum StructureType {
        SIMPLE,
        JIGSAW
    }

    public static final int DEFAULT_ZONE_SIZE = 512;
    public static final int DEFAULT_CLEAR_SECTIONS_BELOW = 1;
    public static final int DEFAULT_CLEAR_SECTIONS_ABOVE = 1;

    /** Sentinel for {@link #getFixedStructureY()}: no imposed altitude, the historical 96/32 alternation applies. */
    public static final int NO_FIXED_Y = Integer.MIN_VALUE;

    /**
     * Historical void-guard altitude, and the default for every dungeon that does not state one.
     * Until action plan 187 this value was a literal {@code 0} written twice inside
     * {@code TimerManager}; it is a config field now because the guard's job changed. In an empty
     * void dimension it catches a player falling out of the world, and 0 is the only sensible line.
     * In a dimension with a generated floor it catches a player who <em>jumped</em>, just above the
     * rooftops - a height that has nothing to do with the bottom of the world and differs per
     * dimension (action plan 187, C1).
     */
    public static final int DEFAULT_VOID_GUARD_Y = 0;

    private final String id;
    private final ResourceKey<Level> dimensionKey;
    private final String structureId;
    private final int playerSpawnOffsetX;
    private final int playerSpawnOffsetY;
    private final int playerSpawnOffsetZ;
    private final int timerDurationSeconds;
    private final int weight;
    private final StructureType structureType;
    private final int jigsawDepth;
    private final boolean skipTeleportScreen;
    private final boolean externallyManaged;
    private final int zoneSize;
    private final int clearSectionsBelow;
    private final int clearSectionsAbove;
    private final int fixedStructureY;
    private final boolean subDungeon;
    private final int voidGuardY;
    private final boolean restoresWorldgen;

    public DungeonConfig(String id, ResourceKey<Level> dimensionKey, String structureId,
                         int playerSpawnOffsetX, int playerSpawnOffsetY, int playerSpawnOffsetZ,
                         int timerDurationSeconds) {
        this(id, dimensionKey, structureId, playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
             timerDurationSeconds, 1, StructureType.SIMPLE, 1, false, false,
             DEFAULT_ZONE_SIZE, DEFAULT_CLEAR_SECTIONS_BELOW, DEFAULT_CLEAR_SECTIONS_ABOVE);
    }

    public DungeonConfig(String id, ResourceKey<Level> dimensionKey, String structureId,
                         int playerSpawnOffsetX, int playerSpawnOffsetY, int playerSpawnOffsetZ,
                         int timerDurationSeconds, int weight) {
        this(id, dimensionKey, structureId, playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
             timerDurationSeconds, weight, StructureType.SIMPLE, 1, false, false,
             DEFAULT_ZONE_SIZE, DEFAULT_CLEAR_SECTIONS_BELOW, DEFAULT_CLEAR_SECTIONS_ABOVE);
    }

    public DungeonConfig(String id, ResourceKey<Level> dimensionKey, String structureId,
                         int playerSpawnOffsetX, int playerSpawnOffsetY, int playerSpawnOffsetZ,
                         int timerDurationSeconds, int weight, boolean skipTeleportScreen) {
        this(id, dimensionKey, structureId, playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
             timerDurationSeconds, weight, StructureType.SIMPLE, 1, skipTeleportScreen, false,
             DEFAULT_ZONE_SIZE, DEFAULT_CLEAR_SECTIONS_BELOW, DEFAULT_CLEAR_SECTIONS_ABOVE);
    }

    public DungeonConfig(String id, ResourceKey<Level> dimensionKey, String structureId,
                         int playerSpawnOffsetX, int playerSpawnOffsetY, int playerSpawnOffsetZ,
                         int timerDurationSeconds, int weight,
                         boolean skipTeleportScreen, boolean externallyManaged) {
        this(id, dimensionKey, structureId, playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
             timerDurationSeconds, weight, StructureType.SIMPLE, 1, skipTeleportScreen, externallyManaged,
             DEFAULT_ZONE_SIZE, DEFAULT_CLEAR_SECTIONS_BELOW, DEFAULT_CLEAR_SECTIONS_ABOVE);
    }

    public DungeonConfig(String id, ResourceKey<Level> dimensionKey, String structureId,
                         int playerSpawnOffsetX, int playerSpawnOffsetY, int playerSpawnOffsetZ,
                         int timerDurationSeconds, int weight, StructureType structureType, int jigsawDepth) {
        this(id, dimensionKey, structureId, playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
             timerDurationSeconds, weight, structureType, jigsawDepth, false, false,
             DEFAULT_ZONE_SIZE, DEFAULT_CLEAR_SECTIONS_BELOW, DEFAULT_CLEAR_SECTIONS_ABOVE);
    }

    public DungeonConfig(String id, ResourceKey<Level> dimensionKey, String structureId,
                         int playerSpawnOffsetX, int playerSpawnOffsetY, int playerSpawnOffsetZ,
                         int timerDurationSeconds, int weight, StructureType structureType, int jigsawDepth,
                         boolean skipTeleportScreen) {
        this(id, dimensionKey, structureId, playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
             timerDurationSeconds, weight, structureType, jigsawDepth, skipTeleportScreen, false,
             DEFAULT_ZONE_SIZE, DEFAULT_CLEAR_SECTIONS_BELOW, DEFAULT_CLEAR_SECTIONS_ABOVE);
    }

    public DungeonConfig(String id, ResourceKey<Level> dimensionKey, String structureId,
                         int playerSpawnOffsetX, int playerSpawnOffsetY, int playerSpawnOffsetZ,
                         int timerDurationSeconds, int weight, StructureType structureType, int jigsawDepth,
                         boolean skipTeleportScreen, boolean externallyManaged) {
        this(id, dimensionKey, structureId, playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
             timerDurationSeconds, weight, structureType, jigsawDepth, skipTeleportScreen, externallyManaged,
             DEFAULT_ZONE_SIZE, DEFAULT_CLEAR_SECTIONS_BELOW, DEFAULT_CLEAR_SECTIONS_ABOVE);
    }

    public DungeonConfig(String id, ResourceKey<Level> dimensionKey, String structureId,
                         int playerSpawnOffsetX, int playerSpawnOffsetY, int playerSpawnOffsetZ,
                         int timerDurationSeconds, int weight, StructureType structureType, int jigsawDepth,
                         int zoneSize, int clearSectionsBelow, int clearSectionsAbove) {
        this(id, dimensionKey, structureId, playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
             timerDurationSeconds, weight, structureType, jigsawDepth, false, false,
             zoneSize, clearSectionsBelow, clearSectionsAbove);
    }

    public DungeonConfig(String id, ResourceKey<Level> dimensionKey, String structureId,
                         int playerSpawnOffsetX, int playerSpawnOffsetY, int playerSpawnOffsetZ,
                         int timerDurationSeconds, int weight, StructureType structureType, int jigsawDepth,
                         boolean skipTeleportScreen, boolean externallyManaged,
                         int zoneSize, int clearSectionsBelow, int clearSectionsAbove) {
        this(id, dimensionKey, structureId, playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
             timerDurationSeconds, weight, structureType, jigsawDepth, skipTeleportScreen, externallyManaged,
             zoneSize, clearSectionsBelow, clearSectionsAbove, NO_FIXED_Y);
    }

    private DungeonConfig(String id, ResourceKey<Level> dimensionKey, String structureId,
                          int playerSpawnOffsetX, int playerSpawnOffsetY, int playerSpawnOffsetZ,
                          int timerDurationSeconds, int weight, StructureType structureType, int jigsawDepth,
                          boolean skipTeleportScreen, boolean externallyManaged,
                          int zoneSize, int clearSectionsBelow, int clearSectionsAbove, int fixedStructureY) {
        this(id, dimensionKey, structureId, playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
             timerDurationSeconds, weight, structureType, jigsawDepth, skipTeleportScreen, externallyManaged,
             zoneSize, clearSectionsBelow, clearSectionsAbove, fixedStructureY, false,
             DEFAULT_VOID_GUARD_Y, false);
    }

    private DungeonConfig(String id, ResourceKey<Level> dimensionKey, String structureId,
                          int playerSpawnOffsetX, int playerSpawnOffsetY, int playerSpawnOffsetZ,
                          int timerDurationSeconds, int weight, StructureType structureType, int jigsawDepth,
                          boolean skipTeleportScreen, boolean externallyManaged,
                          int zoneSize, int clearSectionsBelow, int clearSectionsAbove, int fixedStructureY,
                          boolean subDungeon, int voidGuardY, boolean restoresWorldgen) {
        this.id = id;
        this.dimensionKey = dimensionKey;
        this.structureId = structureId;
        this.playerSpawnOffsetX = playerSpawnOffsetX;
        this.playerSpawnOffsetY = playerSpawnOffsetY;
        this.playerSpawnOffsetZ = playerSpawnOffsetZ;
        this.timerDurationSeconds = timerDurationSeconds;
        this.weight = weight;
        this.structureType = structureType;
        this.jigsawDepth = jigsawDepth;
        this.skipTeleportScreen = skipTeleportScreen;
        this.externallyManaged = externallyManaged;
        this.zoneSize = zoneSize;
        this.clearSectionsBelow = clearSectionsBelow;
        this.clearSectionsAbove = clearSectionsAbove;
        this.fixedStructureY = fixedStructureY;
        this.subDungeon = subDungeon;
        this.voidGuardY = voidGuardY;
        this.restoresWorldgen = restoresWorldgen;
    }

    /**
     * Copy of this configuration with an imposed anchor altitude. Required as soon as the host
     * dimension has a floor at a fixed altitude: the structure must then always anchor at the same Y,
     * otherwise the instance cleanup window can no longer be aligned with it (action plan 175, § 2).
     */
    public DungeonConfig withFixedStructureY(int y) {
        return new DungeonConfig(id, dimensionKey, structureId,
                playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
                timerDurationSeconds, weight, structureType, jigsawDepth,
                skipTeleportScreen, externallyManaged,
                zoneSize, clearSectionsBelow, clearSectionsAbove, y, subDungeon, voidGuardY, restoresWorldgen);
    }

    /**
     * Copy of this configuration with an explicit void-guard altitude (action plan 187, C1).
     *
     * <p>Required as soon as the dimension has a generated floor: the guard must then sit a few
     * blocks <em>above the tallest surface decoration</em> and below the structure's lowest block,
     * so that a player who jumps is pulled out mid-fall instead of landing on scenery they can
     * never leave. The ordering the caller must respect is
     * {@code decoration tops < voidGuardY < structure bounds.minY()} - the right-hand side is
     * load-bearing, because any dungeon block at or below the guard evacuates the players standing
     * in it.</p>
     */
    public DungeonConfig withVoidGuardY(int y) {
        return new DungeonConfig(id, dimensionKey, structureId,
                playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
                timerDurationSeconds, weight, structureType, jigsawDepth,
                skipTeleportScreen, externallyManaged,
                zoneSize, clearSectionsBelow, clearSectionsAbove, fixedStructureY, subDungeon, y, restoresWorldgen);
    }

    /**
     * Copy of this configuration marked as a <b>sub-dungeon</b>: a dimension reached from inside
     * another dungeon rather than from a Hoopa portal (action plan 183, B1).
     *
     * <p>The flag is read in exactly two places, both exclusions - {@code getRandomDungeon()} and
     * {@code PortalSpawnManager.getEnabledDungeonIdsForCycle()}. Every other part of the chain
     * applies unchanged: slot allocation, bounds capture, the async clearer, the timer registry
     * sync, and {@code isDungeonDimension} (and therefore the void guard and the presence sweep).</p>
     */
    public DungeonConfig asSubDungeon() {
        return new DungeonConfig(id, dimensionKey, structureId,
                playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
                timerDurationSeconds, weight, structureType, jigsawDepth,
                skipTeleportScreen, externallyManaged,
                zoneSize, clearSectionsBelow, clearSectionsAbove, fixedStructureY, true, voidGuardY, restoresWorldgen);
    }

    public String getId() {
        return id;
    }

    public ResourceKey<Level> getDimensionKey() {
        return dimensionKey;
    }

    public String getStructureId() {
        return structureId;
    }

    public int getPlayerSpawnOffsetX() {
        return playerSpawnOffsetX;
    }

    public int getPlayerSpawnOffsetY() {
        return playerSpawnOffsetY;
    }

    public int getPlayerSpawnOffsetZ() {
        return playerSpawnOffsetZ;
    }

    public int getTimerDurationSeconds() {
        return timerDurationSeconds;
    }

    public int getTimerDurationTicks() {
        return timerDurationSeconds * 20;
    }

    public String getDimensionId() {
        return dimensionKey.location().toString();
    }

    public int getWeight() {
        return weight;
    }

    public StructureType getStructureType() {
        return structureType;
    }

    public int getJigsawDepth() {
        return jigsawDepth;
    }

    public boolean isJigsaw() {
        return structureType == StructureType.JIGSAW;
    }

    public boolean shouldSkipTeleportScreen() {
        return skipTeleportScreen;
    }

    public boolean isExternallyManaged() {
        return externallyManaged;
    }

    public int getZoneSize() {
        return zoneSize;
    }

    public int getClearSectionsBelow() {
        return clearSectionsBelow;
    }

    public int getClearSectionsAbove() {
        return clearSectionsAbove;
    }

    /** @return true when this dungeon anchors its structure at a fixed altitude instead of the 96/32 alternation. */
    public boolean hasFixedStructureY() {
        return fixedStructureY != NO_FIXED_Y;
    }

    public int getFixedStructureY() {
        return fixedStructureY;
    }

    /** @return true when this dimension is entered from inside another dungeon (action plan 183, B1). */
    public boolean isSubDungeon() {
        return subDungeon;
    }

    /**
     * Altitude below which a player standing in this dimension is evacuated (action plan 187, C1).
     * Defaults to {@link #DEFAULT_VOID_GUARD_Y}, which is the value every dungeon used before that
     * plan made it configurable.
     */
    public int getVoidGuardY() {
        return voidGuardY;
    }

    /**
     * Copy of this configuration whose instances have their worldgen <b>put back</b> when they are
     * cleared, instead of being left as a blanked band of air.
     *
     * <p>This is what lets a dungeon rest on the ground it is generated over. The structure's lower
     * courses sit inside the surface and the decoration band, so the clear window cannot avoid the
     * terrain - it necessarily erases the flat layers and the decoration structures along with the
     * dungeon. Restoration is therefore not a polish step but the thing that makes the layout
     * possible: without it the second instance in a slot would generate over a crater.</p>
     *
     * <p>Only safe where a chunk regenerates deterministically on its own, which is why it is
     * opt-in: a {@code minecraft:flat} or {@code minecraft:noise} generator with per-column terrain
     * (no carvers, aquifers or ore veins), one fixed biome, surface patterns from surface rules and
     * scenery from structure sets only - biome features are never replayed. Used by Megalopolis, the
     * Ultra dimensions and {@code dungeon_jump} (maple canopy built by surface rules, plan 199 § 17).</p>
     */
    public DungeonConfig withWorldgenRestoration() {
        return new DungeonConfig(id, dimensionKey, structureId,
                playerSpawnOffsetX, playerSpawnOffsetY, playerSpawnOffsetZ,
                timerDurationSeconds, weight, structureType, jigsawDepth,
                skipTeleportScreen, externallyManaged,
                zoneSize, clearSectionsBelow, clearSectionsAbove, fixedStructureY, subDungeon,
                voidGuardY, true);
    }

    /** @return true when clearing an instance of this dungeon must rebuild the worldgen it erased. */
    public boolean restoresWorldgen() {
        return restoresWorldgen;
    }
}
