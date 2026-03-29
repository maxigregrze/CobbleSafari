package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class DungeonCommand {

    public static final String ARG_DUNGEON_ID = "dungeon_id";
    private static final String MSG_PLAYER_ONLY = "This command must be run by a player";

    private DungeonCommand() {}

    static int executeSpawn(CommandContext<CommandSourceStack> context) {
        return spawnForPlayer(context, false, null);
    }

    static int executeSpawnWithDungeon(CommandContext<CommandSourceStack> context) {
        String dungeonId = StringArgumentType.getString(context, ARG_DUNGEON_ID);
        return spawnForPlayer(context, false, dungeonId);
    }

    static int executeSpawnForce(CommandContext<CommandSourceStack> context) {
        return spawnForPlayer(context, true, null);
    }

    static int executeSpawnForceWithDungeon(CommandContext<CommandSourceStack> context) {
        String dungeonId = StringArgumentType.getString(context, ARG_DUNGEON_ID);
        return spawnForPlayer(context, true, dungeonId);
    }

    private static int spawnForPlayer(CommandContext<CommandSourceStack> context, boolean force, String dungeonId) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return spawnPortalForPlayer(context.getSource(), target, force, dungeonId);
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("Failed to spawn portal: " + e.getMessage()));
            return 0;
        }
    }

    static int executeSpawnSelf(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(Component.literal(MSG_PLAYER_ONLY));
            return 0;
        }
        return spawnPortalForPlayer(context.getSource(), context.getSource().getPlayer(), false, null);
    }

    static int executeSpawnForceSelf(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(Component.literal(MSG_PLAYER_ONLY));
            return 0;
        }
        return spawnPortalForPlayer(context.getSource(), context.getSource().getPlayer(), true, null);
    }

    static int executeSpawnForceSelfWithDungeon(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(Component.literal(MSG_PLAYER_ONLY));
            return 0;
        }
        String dungeonId = StringArgumentType.getString(context, ARG_DUNGEON_ID);
        return spawnPortalForPlayer(context.getSource(), context.getSource().getPlayer(), true, dungeonId);
    }

    private static int spawnPortalForPlayer(CommandSourceStack source, ServerPlayer player, boolean force, String dungeonId) {
        String trimmed = dungeonId == null ? null : dungeonId.trim();
        if (trimmed != null && !trimmed.isEmpty()) {
            if (DungeonDimensions.getDungeonById(trimmed) == null) {
                source.sendFailure(Component.translatable("cobblesafari.command.dungeon.spawn.unknown_dungeon", trimmed));
                return 0;
            }
        } else {
            trimmed = null;
        }

        boolean success = PortalSpawnManager.spawnPortalNearPlayer(player, force, trimmed);

        if (success) {
            if (trimmed != null) {
                final String dungeonForMessage = trimmed;
                source.sendSuccess(() -> Component.translatable("cobblesafari.command.dungeon.spawn.success_destination",
                        player.getName().getString(), dungeonForMessage), true);
            } else {
                source.sendSuccess(() -> Component.translatable("cobblesafari.command.dungeon.spawn.success",
                        player.getName().getString()), true);
            }
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
