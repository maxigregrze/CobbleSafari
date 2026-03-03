package maxigregrze.cobblesafari.event;

import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.dungeon.DungeonTeleportHandler;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import maxigregrze.cobblesafari.manager.SafariResetManager;
import maxigregrze.cobblesafari.manager.TimerManager;
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
        maxigregrze.cobblesafari.underground.UndergroundMinigame.loadDatapacks(server);
    }

    public static void onServerStopping(MinecraftServer server) {
        TimerManager.saveAllData();
        TimerManager.clearServer();
        PortalSpawnManager.clearServer();
    }

    public static void onServerTick(MinecraftServer server) {
        TimerManager.tickAllTimers();
        PortalSpawnManager.tick(server);
        SafariResetManager.tick(server);
        maxigregrze.cobblesafari.safari.SafariStateManager.onServerTick(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        TimerManager.loadPlayerData(player);
        SafariResetManager.handleReconnectEvacuation(player);
        Services.PLATFORM.sendPayloadToPlayer(player,
                new DimensionalBanSyncPayload(DimensionalBanConfig.data.dimensions));
        maxigregrze.cobblesafari.underground.UndergroundMinigame.syncRegistryToPlayer(player);
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        TimerManager.onPlayerDisconnect(player);
        DungeonTeleportHandler.clearPlayerData(player.getUUID());
    }
}
