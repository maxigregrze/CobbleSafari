package maxigregrze.cobblesafari.event;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.gts.GtsService;
import maxigregrze.cobblesafari.wondertrade.WonderTradeService;
import maxigregrze.cobblesafari.cstrader.logic.CsTraderDataLoader;
import maxigregrze.cobblesafari.dungeon.DungeonRegionClearer;
import maxigregrze.cobblesafari.dungeon.DungeonTeleportHandler;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import maxigregrze.cobblesafari.manager.SafariResetManager;
import maxigregrze.cobblesafari.manager.TimerManager;
import maxigregrze.cobblesafari.rotomphone.RotoFallGroundClear;
import maxigregrze.cobblesafari.rotomphone.RotoGlideServerLogic;
import maxigregrze.cobblesafari.security.JoinThrottle;
import maxigregrze.cobblesafari.security.RateLimiter;
import maxigregrze.cobblesafari.unionroom.UnionRoomDisconnectHandler;
import maxigregrze.cobblesafari.network.DimensionalBanSyncPayload;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.advancement.ModCriteria;
import maxigregrze.cobblesafari.data.StatProgressSavedData;
import maxigregrze.cobblesafari.init.ModStats;
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

    public static void onServerStarted(MinecraftServer server) {
        TimerManager.setServer(server);
        maxigregrze.cobblesafari.dungeon.DungeonInstanceCleanup.resumePendingClears(server);
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
        maxigregrze.cobblesafari.csmusic.CsMusicDataLoader.load(server);
    }

    public static void onServerStopping(MinecraftServer server) {
        TimerManager.saveAllData();
        TimerManager.clearServer();
        PortalSpawnManager.clearServer();
        maxigregrze.cobblesafari.dungeon.DungeonTeleportHandler.clearGenerationStates();
    }

    public static void onServerTick(MinecraftServer server) {
        TimerManager.tickAllTimers();
        DungeonRegionClearer.tick(server);
        PortalSpawnManager.tick(server);
        SafariResetManager.tick(server);
        maxigregrze.cobblesafari.csboss.BossBattleManager.onServerTick(server);
        maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.tick(server);
        if ((server.getTickCount() % 100) == 0) {
            WonderTradeService.tickDailyScheduler(server);
            GtsService.tickDailyScheduler(server);
            GtsService.tickGcLocks(server);
            maxigregrze.cobblesafari.chat.ChatConversationService.tickDailyScheduler(server);
        }
        maxigregrze.cobblesafari.safari.SafariStateManager.onServerTick(server);
        maxigregrze.cobblesafari.unionroom.UnionRoomManager.tickSessionCheck(server);
        maxigregrze.cobblesafari.unionroom.UnionRoomManager.tickPendingExitSetups(server);
        tickPlaytimeStats(server);
    }

    /**
     * Per-tick play-time accounting for the four mod dimensions (cf. plan 94 §3.1) and front-edge
     * Safari-entry day tracking (§3.16). Counts presence regardless of timer/bypass state.
     */
    private static void tickPlaytimeStats(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ResourceLocation dim = player.level().dimension().location();

            if (dim.equals(SAFARI_DIM)) {
                ModStats.award(player, ModStats.TIME_IN_SAFARI);
            } else if (dim.equals(DISTORTION_DIM)) {
                ModStats.award(player, ModStats.TIME_IN_DISTORTION);
            } else if (dim.equals(UNDERGROUND_DIM)) {
                ModStats.award(player, ModStats.TIME_IN_UNDERGROUND);
            } else if (dim.equals(UNION_DIM)) {
                ModStats.award(player, ModStats.TIME_IN_UNION_ROOM);
            }

            ResourceLocation last = LAST_DIMENSION.put(player.getUUID(), dim);
            if (dim.equals(SAFARI_DIM) && !dim.equals(last)) {
                onSafariEntered(player);
            }
        }
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
        UnionRoomDisconnectHandler.onPlayerLogin(player);
        SafariResetManager.handleReconnectEvacuation(player);
        Services.PLATFORM.sendPayloadToPlayer(player,
                new DimensionalBanSyncPayload(DimensionalBanConfig.data.dimensions));
        maxigregrze.cobblesafari.underground.UndergroundMinigame.syncRegistryToPlayer(player);
        maxigregrze.cobblesafari.rotomphone.RotomPhoneConfigSync.syncToPlayer(player);
        maxigregrze.cobblesafari.rotomphone.ChatConversationSync.syncToPlayer(player);
        GtsService.onPlayerJoin(player);
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        RateLimiter.clear(player.getUUID());
        JoinThrottle.clear(player.getUUID());
        maxigregrze.cobblesafari.network.GtsAppServerHandler.clear(player.getUUID());
        maxigregrze.cobblesafari.network.WonderAppServerHandler.clear(player.getUUID());
        GtsService.releasePendingFor(player);
        UnionRoomDisconnectHandler.onPlayerDisconnect(player);
        TimerManager.onPlayerDisconnect(player);
        maxigregrze.cobblesafari.csboss.BossBattleManager.onPlayerDisconnect(player);
        maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onPlayerDisconnect(player.getUUID());
        DungeonTeleportHandler.clearPlayerData(player.getUUID());
        RotoGlideServerLogic.removeState(player.getUUID());
        RotoFallGroundClear.removePlayer(player.getUUID());
        LAST_DIMENSION.remove(player.getUUID());
    }
}
