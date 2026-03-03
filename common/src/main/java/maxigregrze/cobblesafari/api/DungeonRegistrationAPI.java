package maxigregrze.cobblesafari.api;

import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.config.DimensionalBanData;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.PortalSpawnConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public API for addon mods to register custom dungeon dimensions into the CobbleSafari dungeon pool.
 * Registered dungeons can appear as portal destinations and get an automatic timer entry.
 */
public final class DungeonRegistrationAPI {

    private DungeonRegistrationAPI() {}

    private static final Map<String, PortalInterceptor> interceptors = new ConcurrentHashMap<>();
    private static final Map<String, PortalExpirationHandler> expirationHandlers = new ConcurrentHashMap<>();
    private static final Map<String, PortalTooltipProvider> tooltipProviders = new ConcurrentHashMap<>();
    private static final Map<String, PortalSpawnCallback> spawnCallbacks = new ConcurrentHashMap<>();

    /**
     * Registers a dungeon dimension into the pool. The dungeon can then be chosen when a portal
     * spawns in the overworld.
     * <p>
     * If {@link DungeonConfig#isExternallyManaged()} is {@code false}, a timer entry is created
     * automatically for the dimension so that players are teleported out when time expires.
     * If {@code true}, no timer entry is created (the external mod manages the lifecycle).
     *
     * @param config the dungeon configuration (id, dimension key, structure, spawn offsets, timer, weight)
     * @return true if the dungeon was added to the pool; false if a dungeon with the same id already exists
     */
    public static boolean registerDungeon(DungeonConfig config) {
        if (config == null) {
            return false;
        }
        if (DungeonDimensions.getDungeonById(config.getId()) != null) {
            return false;
        }
        DungeonDimensions.addDungeon(config);
        PortalSpawnConfig.ensureDimensionEntry(config.getId(), true, config.getWeight());
        if (!config.isExternallyManaged()) {
            SafariTimerConfig.ensureDimensionEntry(config.getDimensionId(), config.getTimerDurationSeconds(), 0);
        }
        return true;
    }

    /**
     * Registers a portal interceptor for a dungeon dimension. When a player interacts with a portal
     * leading to this dimension, the interceptor is called instead of CobbleSafari's default flow.
     *
     * @param dungeonId   the dungeon id (e.g. "cobblemonraiddens:raid_dimension")
     * @param interceptor the interceptor to register
     */
    public static void registerPortalInterceptor(String dungeonId, PortalInterceptor interceptor) {
        interceptors.put(dungeonId, interceptor);
    }

    /**
     * Returns the portal interceptor registered for the given dungeon id, or {@code null} if none.
     */
    public static PortalInterceptor getPortalInterceptor(String dungeonId) {
        return interceptors.get(dungeonId);
    }

    /**
     * Registers a portal expiration handler for a dungeon dimension. When a portal leading to
     * an externally-managed dungeon expires or is destroyed, this handler is called instead
     * of CobbleSafari's default cleanup.
     *
     * @param dungeonId the dungeon id
     * @param handler   the expiration handler to register
     */
    public static void registerPortalExpirationHandler(String dungeonId, PortalExpirationHandler handler) {
        expirationHandlers.put(dungeonId, handler);
    }

    /**
     * Returns the portal expiration handler registered for the given dungeon id, or {@code null} if none.
     */
    public static PortalExpirationHandler getPortalExpirationHandler(String dungeonId) {
        return expirationHandlers.get(dungeonId);
    }

    /**
     * Registers a custom tooltip provider for a dungeon dimension. When Jade or WTHIT displays
     * the portal destination, this provider is called to supply a custom string (e.g. raid boss
     * species and tier) instead of the default translation key.
     *
     * @param dungeonId the dungeon id (e.g. "cobblemonraiddens:raid_dimension")
     * @param provider  the provider to register; may return null for default behaviour
     */
    public static void registerPortalTooltipProvider(String dungeonId, PortalTooltipProvider provider) {
        tooltipProviders.put(dungeonId, provider);
    }

    /**
     * Returns the tooltip provider registered for the given dungeon id, or {@code null} if none.
     */
    public static PortalTooltipProvider getPortalTooltipProvider(String dungeonId) {
        return tooltipProviders.get(dungeonId);
    }

    /**
     * Registers a callback invoked when a portal leading to this dungeon dimension is spawned.
     * Use this to e.g. pre-create external state (e.g. a raid crystal) so tooltips and first
     * interaction work immediately.
     *
     * @param dungeonId the dungeon id (e.g. "cobblemonraiddens:raid_dimension")
     * @param callback  the callback to register
     */
    public static void registerPortalSpawnCallback(String dungeonId, PortalSpawnCallback callback) {
        spawnCallbacks.put(dungeonId, callback);
    }

    /**
     * Returns the spawn callback registered for the given dungeon id, or {@code null} if none.
     */
    public static PortalSpawnCallback getPortalSpawnCallback(String dungeonId) {
        return spawnCallbacks.get(dungeonId);
    }

    /**
     * Registers dimensional restrictions (block breaking/placing ban, battle toggle) for a dimension.
     * Creates a new entry in the dimensional restrictions config if one does not already exist.
     *
     * @param dimensionId       the full dimension id (e.g. "cobblemonraiddens:raid_dimension")
     * @param allowBlockBreaking whether block breaking is allowed
     * @param allowBlockPlacing  whether block placing is allowed
     * @param allowBattle        whether battles are allowed
     */
    public static void registerDimensionalRestrictions(String dimensionId,
                                                       boolean allowBlockBreaking,
                                                       boolean allowBlockPlacing,
                                                       boolean allowBattle) {
        DimensionalBanData.DimensionRestrictions restrictions = new DimensionalBanData.DimensionRestrictions();
        restrictions.allowBlockBreaking = allowBlockBreaking;
        restrictions.allowBlockPlacing = allowBlockPlacing;
        restrictions.allowBattle = allowBattle;
        DimensionalBanConfig.ensureDimensionEntry(dimensionId, restrictions);
    }

    /**
     * Returns {@code true} if the dungeon with the given id has {@code skipTeleportScreen} enabled.
     */
    public static boolean shouldSkipTeleportScreen(String dungeonId) {
        DungeonConfig config = DungeonDimensions.getDungeonById(dungeonId);
        return config != null && config.shouldSkipTeleportScreen();
    }

    /**
     * Returns {@code true} if the dungeon with the given id is externally managed.
     */
    public static boolean isExternallyManaged(String dungeonId) {
        DungeonConfig config = DungeonDimensions.getDungeonById(dungeonId);
        return config != null && config.isExternallyManaged();
    }
}
