package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.context.CommandContext;
import maxigregrze.cobblesafari.warpride.WarpRideManager;
import maxigregrze.cobblesafari.warpride.WarpRideSession;
import maxigregrze.cobblesafari.warpride.network.WarpRidePayloads;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Ultra Warp Ride debug launcher. <b>Temporary, and must be removed</b> once the wormhole device
 * is the normal way in: it opens a ride outside any dungeon, bypassing the once-per-run entry, the
 * parent clock and the credit floor. Removal also takes the {@code warpride} subtree in
 * {@code CobbleSafariCommand}, the {@code cobblesafari.command.warpride.*} lang keys and
 * {@code WarpRideManager.openDebugSession}.
 */
public final class WarpRideCommand {

    private WarpRideCommand() {}

    static int executeStart(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        if (target == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.player_only"));
            return 0;
        }

        WarpRideManager.OpenResult result = WarpRideManager.openDebugSession(target);
        switch (result) {
            case ALREADY_RIDING -> {
                context.getSource().sendFailure(Component.translatable(
                        "cobblesafari.command.warpride.already_riding", target.getName().getString()));
                return 0;
            }
            case NO_DESTINATION -> {
                context.getSource().sendFailure(
                        Component.translatable("cobblesafari.command.warpride.bad_difficulty"));
                return 0;
            }
            default -> {
                context.getSource().sendSuccess(() -> Component.translatable(
                        "cobblesafari.command.warpride.started", target.getName().getString()), true);
                return 1;
            }
        }
    }

    static int executeStartSelf(CommandContext<CommandSourceStack> context) {
        return executeStart(context, context.getSource().getPlayer());
    }

    static int executeStartOther(CommandContext<CommandSourceStack> context) {
        try {
            return executeStart(context, EntityArgument.getPlayer(context, "player"));
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.player_only"));
            return 0;
        }
    }

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
