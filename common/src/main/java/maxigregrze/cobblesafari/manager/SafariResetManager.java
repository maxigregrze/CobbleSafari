package maxigregrze.cobblesafari.manager;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SafariConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.data.TimerSavedData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SafariResetManager {

    private static final String RESET_COMPLETED_KEY = "cobblesafari.reset.completed";

    private static boolean resetPending = false;
    private static int remainingGraceTicks = 0;

    private static final Set<UUID> pendingEvacuation = ConcurrentHashMap.newKeySet();

    private SafariResetManager() {}

    public static boolean isResetPending() {
        return resetPending;
    }

    public static int getRemainingGraceSeconds() {
        return remainingGraceTicks / 20;
    }

    public static String getFormattedRemainingTime() {
        int totalSeconds = remainingGraceTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static boolean hasPendingEvacuation(UUID playerId) {
        return pendingEvacuation.contains(playerId);
    }

    public static void addPendingEvacuation(UUID playerId) {
        pendingEvacuation.add(playerId);
    }

    public static void tick(MinecraftServer server) {
        if (!resetPending) return;

        remainingGraceTicks--;

        if (isSafariDimensionEmpty(server)) {
            executeFullReset(server);
            return;
        }

        int remainingSec = remainingGraceTicks / 20;
        boolean shouldWarn = remainingSec > 0
                && ((remainingSec > 30 && remainingGraceTicks % (20 * 60) == 0)
                    || (remainingSec <= 30 && remainingGraceTicks % (20 * 10) == 0));

        if (shouldWarn) {
            broadcastToSafariPlayers(server,
                    Component.translatable("cobblesafari.reset.grace.warning", getFormattedRemainingTime()));
        }

        if (remainingGraceTicks <= 0) {
            executeFullReset(server);
        }
    }

    private static boolean isSafariDimensionEmpty(MinecraftServer server) {
        String safariDimensionId = SafariTimerConfig.getSafariDimensionId();
        ResourceKey<Level> safariDimension = ResourceKey.create(
                Registries.DIMENSION, ResourceLocation.parse(safariDimensionId));
        ServerLevel safariLevel = server.getLevel(safariDimension);
        if (safariLevel == null) return true;
        return safariLevel.players().isEmpty();
    }

    public static ResetResult initiateReset(MinecraftServer server) {
        if (resetPending) {
            return ResetResult.ALREADY_PENDING;
        }

        String safariDimensionId = SafariTimerConfig.getSafariDimensionId();
        ResourceKey<Level> safariDimension = ResourceKey.create(
                Registries.DIMENSION, ResourceLocation.parse(safariDimensionId));

        ServerLevel safariLevel = server.getLevel(safariDimension);
        if (safariLevel == null) {
            return ResetResult.DIMENSION_NOT_FOUND;
        }

        List<ServerPlayer> playersInDimension = safariLevel.players();
        boolean shouldKick = shouldKickPlayers();

        if (playersInDimension.isEmpty()) {
            if (shouldKick) {
                markDisconnectedPlayersForEvacuation(server, safariDimensionId);
            }
            performDataReset(server, safariDimensionId);
            broadcastResetCompleted(server);
            return ResetResult.SUCCESS_IMMEDIATE;
        }

        if (!shouldKick) {
            performSoftReset(server, safariDimensionId);
            broadcastResetCompleted(server);
            return ResetResult.SUCCESS_SOFT_RESET;
        }

        if (SafariConfig.isAllowGracePeriod() && SafariConfig.getGracePeriodDuration() > 0) {
            startGracePeriod(server, playersInDimension, safariDimensionId);
            return ResetResult.GRACE_PERIOD_STARTED;
        }

        kickAllPlayersFromSafari(server, safariDimensionId);
        markDisconnectedPlayersForEvacuation(server, safariDimensionId);
        performDataReset(server, safariDimensionId);
        broadcastResetCompleted(server);
        return ResetResult.SUCCESS_IMMEDIATE;
    }

    private static boolean shouldKickPlayers() {
        if (SafariConfig.isKickOnReset()) return true;
        return SafariConfig.isEntryFeeEnabled();
    }

    private static void startGracePeriod(MinecraftServer server,
                                          List<ServerPlayer> players,
                                          String safariDimensionId) {
        resetPending = true;
        remainingGraceTicks = SafariConfig.getGracePeriodDuration() * 20;

        for (ServerPlayer player : players) {
            player.sendSystemMessage(
                    Component.translatable("cobblesafari.reset.grace.started", getFormattedRemainingTime()));
        }

        markDisconnectedPlayersForEvacuation(server, safariDimensionId);

        CobbleSafari.LOGGER.info("Safari reset grace period started: {} seconds",
                SafariConfig.getGracePeriodDuration());
    }

    private static void executeFullReset(MinecraftServer server) {
        String safariDimensionId = SafariTimerConfig.getSafariDimensionId();

        kickAllPlayersFromSafari(server, safariDimensionId);
        markDisconnectedPlayersForEvacuation(server, safariDimensionId);
        performDataReset(server, safariDimensionId);

        resetPending = false;
        remainingGraceTicks = 0;

        broadcastResetCompleted(server);
        CobbleSafari.LOGGER.info("Safari reset completed");
    }

    private static void kickAllPlayersFromSafari(MinecraftServer server, String safariDimensionId) {
        ResourceKey<Level> safariDimension = ResourceKey.create(
                Registries.DIMENSION, ResourceLocation.parse(safariDimensionId));
        ServerLevel safariLevel = server.getLevel(safariDimension);
        if (safariLevel == null) return;

        List<ServerPlayer> players = List.copyOf(safariLevel.players());
        for (ServerPlayer player : players) {
            PlayerTimerData data = TimerManager.getOrCreateData(player, safariDimensionId);
            TimerManager.teleportOnTimerExpired(player, safariDimensionId, data);
            data.setActive(false);
            TimerManager.savePlayerData(player, data);
            player.sendSystemMessage(
                    Component.translatable("cobblesafari.reset.evacuated"));
        }
    }

    private static void markDisconnectedPlayersForEvacuation(MinecraftServer server,
                                                               String safariDimensionId) {
        TimerSavedData savedData = TimerSavedData.get(server);
        for (Map.Entry<UUID, Map<String, PlayerTimerData>> entry : savedData.getAllPlayerData().entrySet()) {
            UUID playerId = entry.getKey();
            PlayerTimerData timerData = entry.getValue().get(safariDimensionId);
            if (timerData == null) continue;

            if (timerData.getOriginPos() != null && timerData.getOriginDimension() != null) {
                ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerId);
                if (onlinePlayer == null) {
                    pendingEvacuation.add(playerId);
                    timerData.setNeedsEvacuation(true);
                }
            }
        }
        savedData.setDirty();
    }

    private static void performDataReset(MinecraftServer server, String safariDimensionId) {
        TimerSavedData.get(server).resetDimensionTimers(safariDimensionId);
        TimerManager.resetDimensionTimers(safariDimensionId);
    }

    private static void performSoftReset(MinecraftServer server, String safariDimensionId) {
        TimerSavedData.get(server).resetDimensionTimers(safariDimensionId);
        TimerManager.resetDimensionTimers(safariDimensionId);

        ResourceKey<Level> safariDimension = ResourceKey.create(
                Registries.DIMENSION, ResourceLocation.parse(safariDimensionId));
        ServerLevel safariLevel = server.getLevel(safariDimension);
        if (safariLevel != null) {
            for (ServerPlayer player : safariLevel.players()) {
                TimerManager.loadPlayerData(player);
                player.sendSystemMessage(Component.translatable("cobblesafari.timer.reset"));
            }
        }
    }

    public static void handleReconnectEvacuation(ServerPlayer player) {
        if (!pendingEvacuation.contains(player.getUUID())) return;

        String safariDimensionId = SafariTimerConfig.getSafariDimensionId();
        ResourceLocation safariDimension = ResourceLocation.parse(safariDimensionId);
        ResourceLocation playerDimension = player.level().dimension().location();

        if (playerDimension.equals(safariDimension)) {
            PlayerTimerData data = TimerManager.getOrCreateData(player, safariDimensionId);
            TimerManager.teleportOnTimerExpired(player, safariDimensionId, data);
            data.setActive(false);
            TimerManager.savePlayerData(player, data);
            player.sendSystemMessage(
                    Component.translatable("cobblesafari.reset.evacuated"));
        }

        pendingEvacuation.remove(player.getUUID());
    }

    private static void broadcastResetCompleted(MinecraftServer server) {
        Component message = Component.translatable(RESET_COMPLETED_KEY);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    private static void broadcastToSafariPlayers(MinecraftServer server, Component message) {
        String safariDimensionId = SafariTimerConfig.getSafariDimensionId();
        ResourceKey<Level> safariDimension = ResourceKey.create(
                Registries.DIMENSION, ResourceLocation.parse(safariDimensionId));
        ServerLevel safariLevel = server.getLevel(safariDimension);
        if (safariLevel == null) return;

        for (ServerPlayer player : safariLevel.players()) {
            player.sendSystemMessage(message);
        }
    }

    public enum ResetResult {
        SUCCESS_IMMEDIATE,
        SUCCESS_SOFT_RESET,
        GRACE_PERIOD_STARTED,
        ALREADY_PENDING,
        DIMENSION_NOT_FOUND
    }
}
