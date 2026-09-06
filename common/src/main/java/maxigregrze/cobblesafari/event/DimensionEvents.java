package maxigregrze.cobblesafari.event;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.config.RotomPhoneConfig;
import maxigregrze.cobblesafari.gts.GtsService;
import maxigregrze.cobblesafari.wondertrade.WonderTradeService;
import maxigregrze.cobblesafari.cstrader.logic.CsTraderDataLoader;
import maxigregrze.cobblesafari.dungeon.DungeonRegionClearer;
import maxigregrze.cobblesafari.dungeon.DungeonTeleportHandler;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import maxigregrze.cobblesafari.manager.BannedItemsManager;
import maxigregrze.cobblesafari.manager.SafariResetManager;
import maxigregrze.cobblesafari.manager.TimerManager;
import maxigregrze.cobblesafari.rotomphone.RotoGlideServerLogic;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneConfigSync;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneEquipped;
import maxigregrze.cobblesafari.security.JoinThrottle;
import maxigregrze.cobblesafari.security.RateLimiter;
import maxigregrze.cobblesafari.unionroom.UnionRoomDisconnectHandler;
import maxigregrze.cobblesafari.network.DimensionalBanSyncPayload;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.advancement.ModCriteria;
import maxigregrze.cobblesafari.data.StatProgressSavedData;
import maxigregrze.cobblesafari.init.ModStats;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DimensionEvents {

    private DimensionEvents() {}

    private static final ResourceLocation SAFARI_DIM =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "domedimension");
    private static final ResourceLocation DISTORTION_DIM =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_distortion");
    private static final ResourceLocation UNDERGROUND_DIM =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_underground");
    private static final ResourceLocation UNION_DIM =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "unionroom");

    /** Player's dimension on the previous tick, to detect a front-edge entry into the Safari. */
    private static final Map<UUID, ResourceLocation> LAST_DIMENSION = new ConcurrentHashMap<>();
    /** Play-time stats are flushed in batches of this many ticks rather than every tick (C4). */
    private static final int STAT_FLUSH_INTERVAL = 20;

    public static void onServerStarted(MinecraftServer server) {
        TimerManager.setServer(server);
        maxigregrze.cobblesafari.dungeon.DungeonInstanceCleanup.resumePendingClears(server);
        // Nobody can be inside a sub-instance across a restart - sessions persist, players do not -
        // so every one of them is marked pending here, and the login path returns whoever was in
        // one to their recorded origin (action plan 183, section 7.4).
        maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager.sweepSubInstancesOnBoot(server);
        // Covers a crash between a region clear and its purge: any session whose instance no longer
        // exists is dropped here (plan 181 § 7). Idempotent, and cheap - it walks stored records once.
        TimerManager.sweepOrphanedSessions(server);
        PortalSpawnManager.setServer(server);
        maxigregrze.cobblesafari.incubator.EggIncubatorRegistry.load(server);
        CsTraderDataLoader.load(server);
        WonderTradeService.onServerStarted(server);
        GtsService.onServerStarted(server);
        maxigregrze.cobblesafari.underground.UndergroundMinigame.loadDatapacks(server);
        maxigregrze.cobblesafari.rotomphone.RotomPhoneSkinDataLoader.load(server);
        maxigregrze.cobblesafari.chat.ChatConversationDataLoader.load(server);
        maxigregrze.cobblesafari.csboss.CsBossDataLoader.load(server);
        maxigregrze.cobblesafari.csboss.BossBattleManager.recoverAll(server);
        // Order matters: the trigger loader warns about rules targeting an unknown music id, which
        // requires the definition registry to be populated first.
        maxigregrze.cobblesafari.csmusic.CsMusicDataLoader.load(server);
        maxigregrze.cobblesafari.csmusic.CsMusicStructureTracker.clear();
        maxigregrze.cobblesafari.csmusic.CsMusicTriggerDataLoader.load(server);
        maxigregrze.cobblesafari.csmusic.CsMusicAreaStore.loadAll(server);
        maxigregrze.cobblesafari.objectives.DimensionalObjectivesDataLoader.load(server);
        maxigregrze.cobblesafari.warpride.logic.WarpRideDifficultyLoader.load(server);
        maxigregrze.cobblesafari.warpride.logic.WarpRideDestinationLoader.load(server);
        // The initial resource load also fires the loaders' reload hook; swallow that flag so the
        // first tick does not redo everything we have just done.
        datapackReloadPending = false;
    }

    /**
     * Set when datapacks were reloaded ({@code /reload}). The actual reloading is deferred to the
     * next server tick <b>on purpose</b>: the loaders read {@code server.getResourceManager()}, and
     * the server only swaps that field once the whole reload chain has completed. Running inside the
     * reload - which is where NeoForge's {@code AddReloadListenerEvent} sits - would re-read the
     * <b>previous</b> pack set, and would also race the rebinding of registry tags that
     * {@code CsMusicTriggerDataLoader} validates against.
     */
    private static volatile boolean datapackReloadPending = false;

    /** Called by both loaders' datapack-reload hook. */
    public static void onDatapackReload() {
        datapackReloadPending = true;
    }

    /**
     * Re-reads every datapack-driven registry of the mod. Kept in common so Fabric and NeoForge
     * cannot drift apart on what {@code /reload} covers.
     */
    private static void reloadDatapackContent(MinecraftServer server) {
        CsTraderDataLoader.load(server);
        maxigregrze.cobblesafari.underground.UndergroundMinigame.loadDatapacks(server);
        maxigregrze.cobblesafari.underground.UndergroundMinigame.syncRegistryToAllPlayers(server);
        maxigregrze.cobblesafari.rotomphone.RotomPhoneSkinDataLoader.load(server);
        maxigregrze.cobblesafari.chat.ChatConversationDataLoader.load(server);
        maxigregrze.cobblesafari.rotomphone.ChatConversationSync.syncToAll(server);
        maxigregrze.cobblesafari.csboss.CsBossDataLoader.load(server);
        // Same order as onServerStarted: the trigger loader reports rules targeting an unknown music
        // id, which needs the definition registry populated first.
        maxigregrze.cobblesafari.csmusic.CsMusicDataLoader.load(server);
        maxigregrze.cobblesafari.csmusic.CsMusicStructureTracker.clear();
        maxigregrze.cobblesafari.csmusic.CsMusicTriggerDataLoader.load(server);
        // Clients hold a copy of their pool's entries; a pool whose membership just changed must be
        // re-sent or they would keep sequencing the old list.
        maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.invalidateAll();
        maxigregrze.cobblesafari.warpride.logic.WarpRideDifficultyLoader.load(server);
        maxigregrze.cobblesafari.warpride.logic.WarpRideDestinationLoader.load(server);
        // Clients hold the typeId -> tint map to draw portal sprites, so a changed colour must be re-sent.
        maxigregrze.cobblesafari.warpride.WarpRideManager.syncDestinationsToAll(server);
    }

    public static void onServerStopping(MinecraftServer server) {
        maxigregrze.cobblesafari.dungeon.sub.SubDungeonGeneration.onServerStopping();
        TimerManager.saveAllData();
        TimerManager.clearServer();
        maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.clearAll();
        maxigregrze.cobblesafari.csmusic.CsMusicStructureTracker.clear();
        PortalSpawnManager.clearServer();
        maxigregrze.cobblesafari.dungeon.DungeonTeleportHandler.clearGenerationStates();
    }

    public static void onServerTick(MinecraftServer server) {
        if (datapackReloadPending) {
            datapackReloadPending = false;
            reloadDatapackContent(server);
        }
        TimerManager.tickAllTimers();
        DungeonRegionClearer.tick(server);
        PortalSpawnManager.tick(server);
        maxigregrze.cobblesafari.warpride.WarpRideManager.tickAll(server);
        // The staged sub-dungeon generation: one phase per tick, never a blocking chunk call on
        // the server thread (plan 138, V1).
        maxigregrze.cobblesafari.dungeon.sub.SubDungeonGeneration.tick(server);
        SafariResetManager.tick(server);
        maxigregrze.cobblesafari.csboss.BossBattleManager.onServerTick(server);
        maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.tick(server);
        maxigregrze.cobblesafari.csmusic.CsMusicStructureTracker.purge(server);
        if ((server.getTickCount() % 20) == 0) {
            // S15: settlement hangs off having LEFT the sub-dimension, never off a portal, so a
            // /tp out cannot keep the parent credit. Aligned with the presence guard's cadence.
            maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager.sweepSettlements(server);
        }
        if ((server.getTickCount() % 100) == 0) {
            // Feature A's guarantee (plan 182 § 5.1): release any detached instance that has emptied.
            // A sweep rather than hooks on the departure paths, so no exit can be forgotten - and
            // Feature B will add one.
            maxigregrze.cobblesafari.dungeon.DungeonInstanceCleanup.sweepDetachedInstances(server);
            WonderTradeService.tickDailyScheduler(server);
            // Budgeted top-up (C6): gradually refills the Wonder Trade pool a few entries at a time instead
            // of generating the whole deficit in the single startup/daily-reset tick.
            WonderTradeService.runAutofillIfNeeded(server);
            GtsService.tickDailyScheduler(server);
            GtsService.tickGcLocks(server);
            maxigregrze.cobblesafari.chat.ChatConversationService.tickDailyScheduler(server);
            maxigregrze.cobblesafari.objectives.ObjectivesManager.tickDailyScheduler(server);
            maxigregrze.cobblesafari.power.GuaranteedShinyManager.sweepExpired(server);
            DungeonTeleportHandler.sweepStaleGenerations();
            // The same 145 C11 lesson one level down: a stale lock would soft-lock the device for
            // the rest of the run, with the once-per-run entry already spent.
            maxigregrze.cobblesafari.dungeon.sub.SubDungeonGeneration.sweepStaleSubGenerations(server);
        }
        DimensionalBanEventHandler.tickMountEnforcement(server);
        maxigregrze.cobblesafari.safari.SafariStateManager.onServerTick(server);
        maxigregrze.cobblesafari.unionroom.UnionRoomManager.tickSessionCheck(server);
        maxigregrze.cobblesafari.unionroom.UnionRoomManager.tickPendingExitSetups(server);
        tickPlaytimeStats(server);
        maxigregrze.cobblesafari.objectives.ObjectivesManager.tickContext(server);
    }

    /**
     * Per-tick play-time accounting for the four mod dimensions and front-edge
     * Safari-entry day tracking (). Counts presence regardless of timer/bypass state.
     */
    private static void tickPlaytimeStats(MinecraftServer server) {
        // Batch the four play-time awards to once per second (amount = interval) instead of an awardStat call
        // per player per stat every single tick; the per-tick dimension tracking below is kept intact (C4).
        boolean flush = (server.getTickCount() % STAT_FLUSH_INTERVAL) == 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ResourceLocation dim = player.level().dimension().location();

            if (flush) {
                if (dim.equals(SAFARI_DIM)) {
                    ModStats.add(player, ModStats.TIME_IN_SAFARI, STAT_FLUSH_INTERVAL);
                } else if (dim.equals(DISTORTION_DIM)) {
                    ModStats.add(player, ModStats.TIME_IN_DISTORTION, STAT_FLUSH_INTERVAL);
                } else if (dim.equals(UNDERGROUND_DIM)) {
                    ModStats.add(player, ModStats.TIME_IN_UNDERGROUND, STAT_FLUSH_INTERVAL);
                } else if (dim.equals(UNION_DIM)) {
                    int unionTicks = ModStats.addAndGet(player, ModStats.TIME_IN_UNION_ROOM, STAT_FLUSH_INTERVAL);
                    // "Hyper-social" unlocks at 60 min (72000 ticks, a multiple of the interval).
                    ModCriteria.HYPER_SOCIAL.trigger(player, unionTicks);
                }
            }

            ResourceLocation last = LAST_DIMENSION.put(player.getUUID(), dim);
            if (!dim.equals(last)) {
                onDimensionEntered(player);
                if (dim.equals(SAFARI_DIM)) {
                    onSafariEntered(player);
                }
            }
        }
    }

    /**
     * Front edge of any dimension change: warns the carrier of a Rotom device that this dimension
     * suppresses the phone's safety features (action plan 176). Also fires on the first tick after
     * login, which is deliberate - reconnecting inside the dungeon is exactly when the reminder is
     * worth having.
     *
     * <p>Three guards, cheapest first. The last one keeps the message from spoiling a feature the
     * player has not met yet: the safety toggles live in the settings app, which is not unlocked from
     * the start, so a player without it has nothing to be told about.
     */
    private static void onDimensionEntered(ServerPlayer player) {
        if (BannedItemsManager.isRotomSafetyAllowed(player.level().dimension())) {
            return;
        }
        if (!RotomPhoneEquipped.hasRotomDevice(player)) {
            return;
        }
        if (!RotomPhoneConfigSync.isAppUnlockedByPlayer(player, RotomPhoneConfig.APP_SETTINGS)) {
            return;
        }
        player.sendSystemMessage(Component.translatable("cobblesafari.ban.rotom_safety_banned"));
    }

    /** First Safari entry of a given day records a distinct day for the "Safari Addict" achievement. */
    private static void onSafariEntered(ServerPlayer player) {
        StatProgressSavedData data = StatProgressSavedData.get(player.getServer());
        if (data == null) return;
        long today = LocalDate.now(ZoneId.systemDefault()).toEpochDay();
        int distinctDays = data.recordSafariDay(player.getUUID(), today);
        if (distinctDays > 0) {
            ModCriteria.SAFARI_DAY.trigger(player, distinctDays);
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        TimerManager.loadPlayerData(player);
        // If the instance they logged out of was cleared while they were away, put them back where
        // they came in rather than leaving them inside erased blocks (plan 182, A3).
        TimerManager.handleDungeonReturnOnJoin(player);
        UnionRoomDisconnectHandler.onPlayerLogin(player);
        SafariResetManager.handleReconnectEvacuation(player);
        Services.PLATFORM.sendPayloadToPlayer(player,
                new DimensionalBanSyncPayload(DimensionalBanConfig.data.dimensions));
        maxigregrze.cobblesafari.underground.UndergroundMinigame.syncRegistryToPlayer(player);
        maxigregrze.cobblesafari.rotomphone.RotomPhoneConfigSync.syncToPlayer(player);
        maxigregrze.cobblesafari.rotomphone.ChatConversationSync.syncToPlayer(player);
        GtsService.onPlayerJoin(player);
        // A server killed mid-ride leaves the rider's protection in their saved NBT; this is the net.
        maxigregrze.cobblesafari.warpride.RiderBodyGuard.clearIfMarked(player);
        maxigregrze.cobblesafari.warpride.WarpRideManager.syncDestinations(player);
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        RateLimiter.clear(player.getUUID());
        JoinThrottle.clear(player.getUUID());
        maxigregrze.cobblesafari.network.GtsAppServerHandler.clear(player.getUUID());
        maxigregrze.cobblesafari.network.WonderAppServerHandler.clear(player.getUUID());
        maxigregrze.cobblesafari.network.ChatAppServerHandler.clear(player.getUUID());
        GtsService.releasePendingFor(player);
        UnionRoomDisconnectHandler.onPlayerDisconnect(player);
        // Read the scope BEFORE onPlayerDisconnect tears the player's timer state down: logging out
        // is a departure, so the instance may now be empty (plan 182, A3).
        maxigregrze.cobblesafari.data.TimerScope leftScope = TimerManager.scopeOnDisconnect(player);
        if (leftScope != null) {
            TimerManager.recordPendingReturn(player, leftScope);
        }
        TimerManager.onPlayerDisconnect(player);
        if (leftScope != null && player.getServer() != null) {
            try {
                maxigregrze.cobblesafari.dungeon.DungeonInstanceCleanup.maybeReleaseInstance(
                        player.getServer(), java.util.UUID.fromString(leftScope.scopeId()));
            } catch (IllegalArgumentException ignored) {
                // Non-UUID scope id (a future subsystem): the 100-tick sweep still covers it.
            }
        }
        // A visit that ends without settling leaves its running counter behind; it is only used
        // for the "time spent" message, but it must not survive into the next visit.
        maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager.forgetSpent(player.getUUID());
        maxigregrze.cobblesafari.csboss.BossBattleManager.onPlayerDisconnect(player);
        maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onPlayerDisconnect(player.getUUID());
        DungeonTeleportHandler.clearPlayerData(player.getUUID());
        RotoGlideServerLogic.removeState(player.getUUID());
        maxigregrze.cobblesafari.rotomphone.EmptyPhoneServerHandler.clear(player.getUUID());
        maxigregrze.cobblesafari.rotomphone.OnlinePcAccess.clear(player.getUUID());
        LAST_DIMENSION.remove(player.getUUID());
        maxigregrze.cobblesafari.objectives.ObjectivesManager.onPlayerDisconnect(player.getUUID());
        maxigregrze.cobblesafari.warpride.WarpRideManager.onPlayerDisconnect(player);
    }
}
