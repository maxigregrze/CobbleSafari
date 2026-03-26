package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.context.CommandContext;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.config.IncubatorConfig;
import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.config.SafariConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.config.SecretBasePCConfig;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.DungeonRegionClearer;
import maxigregrze.cobblesafari.dungeon.PortalSpawnConfig;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import maxigregrze.cobblesafari.manager.SafariResetManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

public class ResetCommand {

    private ResetCommand() {}

    static int executeSafariReset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();

        SafariResetManager.ResetResult result = SafariResetManager.initiateReset(server);

        switch (result) {
            case SUCCESS_IMMEDIATE:
                source.sendSuccess(
                        () -> Component.translatable("cobblesafari.command.reset.safari_success"), true);
                CobbleSafari.LOGGER.info("Safari dimension and all player timers reset by {}",
                        source.getTextName());
                return 1;

            case SUCCESS_SOFT_RESET:
                source.sendSuccess(
                        () -> Component.translatable("cobblesafari.command.reset.safari_soft_success"), true);
                CobbleSafari.LOGGER.info("Safari timers soft-reset by {} (players remained in dimension)",
                        source.getTextName());
                return 1;

            case GRACE_PERIOD_STARTED:
                int seconds = SafariConfig.getGracePeriodDuration();
                source.sendSuccess(
                        () -> Component.translatable("cobblesafari.command.reset.grace_started", seconds), true);
                source.sendSystemMessage(
                        Component.translatable("cobblesafari.command.reset.players_warned"));
                CobbleSafari.LOGGER.warn(
                        "Safari reset initiated by {} - players present, grace period of {}s started",
                        source.getTextName(), seconds);
                return 1;

            case ALREADY_PENDING:
                source.sendFailure(
                        Component.translatable("cobblesafari.command.reset.already_pending",
                                SafariResetManager.getFormattedRemainingTime()));
                return 0;

            case DIMENSION_NOT_FOUND:
                source.sendFailure(
                        Component.translatable("cobblesafari.command.reset.dimension_not_found",
                                SafariTimerConfig.getSafariDimensionId()));
                return 0;

            default:
                return 0;
        }
    }

    static int executeRefresh(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        SafariTimerConfig.load();
        SafariTimerConfig.syncDungeonDimensionTimersFromRegistry();
        IncubatorConfig.load();
        SafariConfig.load();
        SpawnBoostConfig.load();
        PortalSpawnConfig.load();
        DimensionalBanConfig.load();
        MiscConfig.load();
        SecretBasePCConfig.load();

        source.sendSuccess(() -> Component.translatable("cobblesafari.command.refresh.success"), true);
        CobbleSafari.LOGGER.info("All configs refreshed by {}", source.getTextName());

        return 1;
    }

    static int executeDungeonReset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();

        List<DungeonConfig> dungeons = DungeonDimensions.getAllDungeons();

        for (DungeonConfig dungeon : dungeons) {
            ServerLevel dungeonLevel = server.getLevel(dungeon.getDimensionKey());
            if (dungeonLevel != null) {
                List<ServerPlayer> playersInDimension = dungeonLevel.players();
                if (!playersInDimension.isEmpty()) {
                    source.sendFailure(Component.translatable("cobblesafari.command.reset.players_present",
                            playersInDimension.size(), dungeon.getDimensionId()));
                    return 0;
                }
            }
        }

        PortalSpawnManager.removeAllDungeonPortals(server);

        for (DungeonConfig dungeon : dungeons) {
            ServerLevel dungeonLevel = server.getLevel(dungeon.getDimensionKey());
            if (dungeonLevel != null) {
                clearAllForcedChunks(dungeonLevel);
            }
        }

        DungeonPositionSavedData.get(server).clearAllDungeons();

        source.sendSuccess(() -> Component.translatable("cobblesafari.command.reset.dungeon_success", dungeons.size()), true);
        CobbleSafari.LOGGER.info("All dungeon dimensions reset by {}", source.getTextName());

        return 1;
    }

    private static void clearAllForcedChunks(ServerLevel level) {
        var forcedChunks = level.getForcedChunks();
        if (forcedChunks.isEmpty()) return;

        int minCX = Integer.MAX_VALUE;
        int minCZ = Integer.MAX_VALUE;
        int maxCX = Integer.MIN_VALUE;
        int maxCZ = Integer.MIN_VALUE;

        for (long pos : forcedChunks) {
            int cx = ChunkPos.getX(pos);
            int cz = ChunkPos.getZ(pos);
            minCX = Math.min(minCX, cx);
            minCZ = Math.min(minCZ, cz);
            maxCX = Math.max(maxCX, cx);
            maxCZ = Math.max(maxCZ, cz);
        }

        DungeonRegionClearer.clearRegion(level, minCX, minCZ, maxCX, maxCZ);
        CobbleSafari.LOGGER.info("Cleared all forced chunks in dimension {}: [{},{}] to [{},{}]",
                level.dimension().location(), minCX, minCZ, maxCX, maxCZ);
    }
}
