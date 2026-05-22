package maxigregrze.cobblesafari.event;

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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class DimensionEvents {

    private DimensionEvents() {}

    public static void onServerStarted(MinecraftServer server) {
        TimerManager.setServer(server);
        PortalSpawnManager.setServer(server);
        maxigregrze.cobblesafari.incubator.EggIncubatorRegistry.load(server);
        CsTraderDataLoader.load(server);
        WonderTradeService.onServerStarted(server);
        GtsService.onServerStarted(server);
        maxigregrze.cobblesafari.underground.UndergroundMinigame.loadDatapacks(server);
        maxigregrze.cobblesafari.rotomphone.RotomPhoneSkinDataLoader.load(server);
    }

    public static void onServerStopping(MinecraftServer server) {
        TimerManager.saveAllData();
        TimerManager.clearServer();
        PortalSpawnManager.clearServer();
    }

    public static void onServerTick(MinecraftServer server) {
        TimerManager.tickAllTimers();
        DungeonRegionClearer.tick(server);
        PortalSpawnManager.tick(server);
        SafariResetManager.tick(server);
        WonderTradeService.tickDailyScheduler(server);
        GtsService.tickDailyScheduler(server);
        if ((server.getTickCount() % 100) == 0) {
            GtsService.tickGcLocks(server);
        }
        maxigregrze.cobblesafari.safari.SafariStateManager.onServerTick(server);
        maxigregrze.cobblesafari.unionroom.UnionRoomManager.tickSessionCheck(server);
        maxigregrze.cobblesafari.unionroom.UnionRoomManager.tickPendingExitSetups(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        TimerManager.loadPlayerData(player);
        UnionRoomDisconnectHandler.onPlayerLogin(player);
        SafariResetManager.handleReconnectEvacuation(player);
        Services.PLATFORM.sendPayloadToPlayer(player,
                new DimensionalBanSyncPayload(DimensionalBanConfig.data.dimensions));
        maxigregrze.cobblesafari.underground.UndergroundMinigame.syncRegistryToPlayer(player);
        maxigregrze.cobblesafari.rotomphone.RotomPhoneConfigSync.syncToPlayer(player);
        GtsService.onPlayerJoin(player);
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        RateLimiter.clear(player.getUUID());
        JoinThrottle.clear(player.getUUID());
        GtsService.releasePendingFor(player);
        UnionRoomDisconnectHandler.onPlayerDisconnect(player);
        TimerManager.onPlayerDisconnect(player);
        DungeonTeleportHandler.clearPlayerData(player.getUUID());
        RotoGlideServerLogic.removeState(player.getUUID());
        RotoFallGroundClear.removePlayer(player.getUUID());
    }
}
