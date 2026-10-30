package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.context.CommandContext;
import maxigregrze.cobblesafari.data.WarpRideTutorialSavedData;
import maxigregrze.cobblesafari.warpride.WarpRideManager;
import maxigregrze.cobblesafari.warpride.WarpRideSession;
import maxigregrze.cobblesafari.warpride.network.WarpRidePayloads;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Operator tools for the Ultra Warp Ride: stop a ride, list the live ones, reset the tutorial.
 *
 * <p>The debug launcher that opened parentless rides is gone (action plan 200, section 11): the
 * mini-game is only reached through a wormhole device now, and nothing here can start a ride.
 */
public final class WarpRideCommand {

    private WarpRideCommand() {}

    static int executeStop(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        if (target == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.player_only"));
            return 0;
        }
        MinecraftServer server = context.getSource().getServer();
        if (!WarpRideManager.isRiding(target.getUUID())) {
            context.getSource().sendFailure(Component.translatable(
                    "cobblesafari.command.warpride.no_session", target.getName().getString()));
            return 0;
        }
        WarpRideManager.endSession(server, target.getUUID(), WarpRidePayloads.END_REASON_ABORTED);
        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.warpride.stopped", target.getName().getString()), true);
        return 1;
    }

    static int executeStopSelf(CommandContext<CommandSourceStack> context) {
        return executeStop(context, context.getSource().getPlayer());
    }

    static int executeStopOther(CommandContext<CommandSourceStack> context) {
        try {
            return executeStop(context, EntityArgument.getPlayer(context, "player"));
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.player_only"));
            return 0;
        }
    }

    static int executeTutorialReset(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        if (target == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.player_only"));
            return 0;
        }
        WarpRideTutorialSavedData.get(context.getSource().getServer()).reset(target.getUUID());
        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.warpride.tutorial_reset", target.getName().getString()), true);
        return 1;
    }

    static int executeTutorialResetSelf(CommandContext<CommandSourceStack> context) {
        return executeTutorialReset(context, context.getSource().getPlayer());
    }

    static int executeTutorialResetOther(CommandContext<CommandSourceStack> context) {
        try {
            return executeTutorialReset(context, EntityArgument.getPlayer(context, "player"));
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.player_only"));
            return 0;
        }
    }

    static int executeStatus(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        int count = WarpRideManager.activeCount();
        context.getSource().sendSuccess(
                () -> Component.literal("[WarpRide] " + count + " active session(s)"), false);
        for (WarpRideSession session : WarpRideManager.allSessions()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(session.getOwnerId());
            String name = owner == null ? session.getOwnerId().toString() : owner.getName().getString();
            String line = String.format("  %s — %s — %.0f ly — %.0f%% energy — %.1f ly/s%s",
                    name,
                    session.getPhase().name(),
                    session.getOdometer(),
                    session.getEnergy(),
                    session.getSpeed(),
                    session.isDrifting() ? " — DRIFTING" : "");
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return count;
    }
}
