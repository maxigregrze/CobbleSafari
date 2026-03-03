package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.context.CommandContext;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class DungeonCommand {

    private DungeonCommand() {}

    static int executeSpawn(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return spawnPortalForPlayer(context.getSource(), target, false);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to spawn portal: " + e.getMessage()));
            return 0;
        }
    }

    static int executeSpawnSelf(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        return spawnPortalForPlayer(context.getSource(), context.getSource().getPlayer(), false);
    }

    static int executeSpawnForce(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return spawnPortalForPlayer(context.getSource(), target, true);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to spawn portal: " + e.getMessage()));
            return 0;
        }
    }

    static int executeSpawnForceSelf(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        return spawnPortalForPlayer(context.getSource(), context.getSource().getPlayer(), true);
    }

    private static int spawnPortalForPlayer(CommandSourceStack source, ServerPlayer player, boolean force) {
        boolean success = PortalSpawnManager.spawnPortalNearPlayer(player, force);

        if (success) {
            source.sendSuccess(() -> Component.translatable("cobblesafari.command.dungeon.spawn.success",
                    player.getName().getString()), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("cobblesafari.command.dungeon.spawn.failed",
                    player.getName().getString()));
            return 0;
        }
    }

    static int executeList(CommandContext<CommandSourceStack> context) {
        List<PortalSpawnManager.ActivePortal> portals = PortalSpawnManager.getActivePortals();

        if (portals.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.dungeon.list.empty"), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.dungeon.list.header",
                portals.size()), false);

        for (PortalSpawnManager.ActivePortal portal : portals) {
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("  - %s at [%d, %d, %d] (%s)",
                            portal.dungeonId(),
                            portal.pos().getX(),
                            portal.pos().getY(),
                            portal.pos().getZ(),
                            portal.dimension().location().toString())
            ), false);
        }

        return 1;
    }

    static int executeForceList(CommandContext<CommandSourceStack> context) {
        int found = PortalSpawnManager.forceScanWorldForPortals(context.getSource().getServer());
        if (found > 0) {
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Force scan found %d additional portals", found)), false);
        }

        return executeList(context);
    }

    static int executeDimensions(CommandContext<CommandSourceStack> context) {
        List<DungeonConfig> dungeons = DungeonDimensions.getAllDungeons();

        if (dungeons.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No dungeon dimensions registered"), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.literal("Registered dungeon dimensions:"), false);

        for (DungeonConfig dungeon : dungeons) {
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("  - %s (%s) - Timer: %ds",
                            dungeon.getId(),
                            dungeon.getDimensionId(),
                            dungeon.getTimerDurationSeconds())
            ), false);
        }

        return 1;
    }
}
