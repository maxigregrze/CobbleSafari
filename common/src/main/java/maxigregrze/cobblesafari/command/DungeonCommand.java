package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.data.DungeonInstanceRecord;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class DungeonCommand {

    public static final String ARG_DUNGEON_ID = "dungeon_id";

    private DungeonCommand() {}

    static int executeSpawn(CommandContext<CommandSourceStack> context) {
        return spawnForPlayer(context, false, null);
    }

    static int executeSpawnSelfWithDungeon(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.player_only"));
            return 0;
        }
        return spawnPortalForPlayer(context.getSource(), context.getSource().getPlayer(), false,
                StringArgumentType.getString(context, ARG_DUNGEON_ID));
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
            context.getSource().sendFailure(
                    Component.translatable("cobblesafari.command.dungeon.spawn.syntax_error", e.getMessage()));
            return 0;
        }
    }

    static int executeSpawnSelf(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.player_only"));
            return 0;
        }
        return spawnPortalForPlayer(context.getSource(), context.getSource().getPlayer(), false, null);
    }

    static int executeSpawnForceSelf(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.player_only"));
            return 0;
        }
        return spawnPortalForPlayer(context.getSource(), context.getSource().getPlayer(), true, null);
    }

    static int executeSpawnForceSelfWithDungeon(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.player_only"));
            return 0;
        }
        String dungeonId = StringArgumentType.getString(context, ARG_DUNGEON_ID);
        return spawnPortalForPlayer(context.getSource(), context.getSource().getPlayer(), true, dungeonId);
    }

    private static int spawnPortalForPlayer(CommandSourceStack source, ServerPlayer player, boolean force, String dungeonId) {
        String trimmed = dungeonId == null ? null : dungeonId.trim();
        if (trimmed != null && !trimmed.isEmpty()) {
            DungeonConfig target = DungeonDimensions.getDungeonById(trimmed);
            if (target == null) {
                source.sendFailure(Component.translatable("cobblesafari.command.dungeon.spawn.unknown_dungeon", trimmed));
                return 0;
            }
            // A sub-dungeon takes its structure from a destination file, not from its config, so a
            // portal aimed at one opens onto an empty region. Refused in code rather than merely
            // hidden from the suggestions (action plan 183, section 3.2).
            if (target.isSubDungeon()) {
                source.sendFailure(Component.translatable(
                        "cobblesafari.command.dungeon.spawn.sub_dungeon", trimmed));
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

    /**
     * A position the operator can click to travel to. Runs {@code /execute in <dim> run tp @s ...}
     * <b>as the player</b>, so it is gated by their own permissions: someone allowed to use
     * CobbleSafari commands but not teleportation gets vanilla's "unknown command" reply rather
     * than a silent no-op or an escalation.
     */
    private static Component clickablePos(String dimensionId, BlockPos pos) {
        String command = String.format("/execute in %s run tp @s %d %d %d",
                dimensionId, pos.getX(), pos.getY(), pos.getZ());
        return Component.literal(String.format("%d %d %d", pos.getX(), pos.getY(), pos.getZ()))
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("cobblesafari.command.dungeon.list.click_teleport"))));
    }

    /** An instance id the operator can click to copy, mirroring {@code timer session list}. */
    private static Component clickableInstanceId(UUID instanceId) {
        String id = instanceId.toString();
        return Component.literal(id).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("cobblesafari.command.dungeon.list.click_copy"))));
    }

    /**
     * {@code /cs dungeon list} - the live instances. Since plan 182 an instance outlives its portal
     * while it is occupied, so instances, not portals, are what an operator needs by default;
     * {@code /cs dungeon list portal} still lists the portals.
     */
    static int executeList(CommandContext<CommandSourceStack> context) {
        List<DungeonInstanceRecord> instances =
                maxigregrze.cobblesafari.data.DungeonPositionSavedData.get(context.getSource().getServer())
                        .getInstancesSnapshot().stream()
                        .filter(r -> !r.pendingDeletion())
                        .sorted(java.util.Comparator
                                .comparing(DungeonInstanceRecord::dimensionId)
                                .thenComparingInt(DungeonInstanceRecord::slot))
                        .toList();

        if (instances.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.dungeon.list.instances_empty"), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.dungeon.list.instances_header", instances.size()), false);

        for (DungeonInstanceRecord record : instances) {
            int occupants = maxigregrze.cobblesafari.dungeon.DungeonInstanceCleanup
                    .countOccupants(context.getSource().getServer(), record);
            Component entrance = describeEntrance(context.getSource().getServer(), record);
            context.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.dungeon.list.instance_entry",
                            record.dimensionId(),
                            record.slot(),
                            clickableInstanceId(record.instanceId()),
                            clickablePos(record.dimensionId(), record.structurePos()),
                            entrance,
                            occupants),
                    false);
        }

        return 1;
    }

    /** "entrance closed", or "entrance open @ &lt;clickable portal position&gt;". */
    private static Component describeEntrance(net.minecraft.server.MinecraftServer server,
                                              DungeonInstanceRecord record) {
        // A sub-instance has no entrance at all - its device stands in another dimension - so it
        // reports its parent and its owner instead (action plan 183, B10).
        if (record.isSubInstance()) {
            String owner = "?";
            if (record.ownerPlayerId() != null) {
                net.minecraft.server.level.ServerPlayer player =
                        server.getPlayerList().getPlayer(record.ownerPlayerId());
                owner = player != null
                        ? player.getName().getString()
                        : record.ownerPlayerId().toString().substring(0, 8);
            }
            return Component.translatable("cobblesafari.command.dungeon.list.sub_entry",
                    owner,
                    record.parentInstanceId() == null
                            ? "-"
                            : clickableInstanceId(record.parentInstanceId()));
        }
        if (record.entranceClosed()) {
            return Component.translatable("cobblesafari.command.dungeon.list.entrance_closed");
        }
        PortalSpawnManager.ActivePortal portal = record.portalId() == null
                ? null
                : PortalSpawnManager.getActivePortalById(record.portalId());
        if (portal == null) {
            // Open but untracked: the block exists somewhere the manager has lost sight of.
            return Component.translatable("cobblesafari.command.dungeon.list.entrance_open");
        }
        return Component.translatable("cobblesafari.command.dungeon.list.entrance_open_at",
                clickablePos(portal.dimension().location().toString(), portal.pos()));
    }

    /** {@code /cs dungeon list portal} - the active entrance portals. */
    static int executePortalList(CommandContext<CommandSourceStack> context) {
        List<PortalSpawnManager.ActivePortal> portals = PortalSpawnManager.getActivePortals();

        if (portals.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.dungeon.list.empty"), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.dungeon.list.header",
                portals.size()), false);

        for (PortalSpawnManager.ActivePortal portal : portals) {
            context.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.dungeon.list.entry",
                            portal.dungeonId(),
                            clickablePos(portal.dimension().location().toString(), portal.pos()),
                            portal.dimension().location().toString()),
                    false);
        }

        return 1;
    }

    static int executeForceList(CommandContext<CommandSourceStack> context) {
        int found = PortalSpawnManager.forceScanWorldForPortals(context.getSource().getServer());
        if (found > 0) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.dungeon.list.force_scan", found), false);
        }

        return executePortalList(context);
    }

    static int executeDimensions(CommandContext<CommandSourceStack> context) {
        List<DungeonConfig> dungeons = DungeonDimensions.getAllDungeons();

        if (dungeons.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.dungeon.dimensions.empty"), false);
            return 1;
        }

        context.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.dungeon.dimensions.header"), false);

        for (DungeonConfig dungeon : dungeons) {
            context.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.dungeon.dimensions.entry",
                            dungeon.getId(),
                            dungeon.getDimensionId(),
                            dungeon.getTimerDurationSeconds()),
                    false);
        }

        return 1;
    }
}
