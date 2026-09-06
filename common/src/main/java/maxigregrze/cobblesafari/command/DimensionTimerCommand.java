package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.data.TimerScope;
import maxigregrze.cobblesafari.manager.TimerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * {@code /cobblesafari timer dimension …} and {@code /cobblesafari timer session …}.
 * <p>
 * Since action plan 181 (V7) these target a timer the player <b>holds</b>, not the one where they
 * happen to stand. The distinction is not cosmetic: it is what keeps death recovery working. A
 * player who died in a dungeon has been teleported out, but still holds that instance's session at
 * zero credit - so crediting it and walking back through the portal returns them to the same run.
 * <p>
 * The command can credit a session; it can never create one. An operator therefore cannot stock the
 * wallet of a dungeon a player has never entered (finding S1 stays closed).
 */
public class DimensionTimerCommand {

    private DimensionTimerCommand() {}

    private static final String ARG_DIMENSION = "dimension";
    private static final String ARG_PLAYER = "player";
    private static final String ARG_SECONDS = "seconds";
    private static final String ARG_SCOPE = "scope";
    private static final String MSG_NOT_CONFIGURED = "cobblesafari.command.dimension.not_configured";
    private static final String MSG_NO_SESSION = "cobblesafari.command.timer.no_session";
    private static final String MSG_AMBIGUOUS = "cobblesafari.command.timer.ambiguous_session";

    /**
     * Completes the {@code scope} argument with the ids the target player actually holds.
     * <p>
     * A scope id is an instance UUID, not a dimension id - without completion there is no way for an
     * operator to discover that. Reading the earlier {@code player} argument here is safe because
     * {@link CobbleSafariCommand} instantiates the tree per alias rather than redirecting; see the
     * note on {@code rootTree}.
     */
    public static final SuggestionProvider<CommandSourceStack> SCOPE_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, ARG_PLAYER);
            return SharedSuggestionProvider.suggest(
                    TimerManager.allLiveSessions(target).stream()
                            .map(PlayerTimerData::getScopeId)
                            .toList(),
                    builder);
        } catch (Exception e) {
            return builder.buildFuture();
        }
    };

    /**
     * A scope id the operator can click to copy, so it can be pasted straight into
     * {@code timer session add|set}. The model is otherwise invisible in game.
     */
    private static Component clickableScope(String scopeId) {
        return Component.literal(scopeId).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, scopeId))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("cobblesafari.command.timer.session.copy_hint"))));
    }

    /** The record a dimension-scoped command should act on, or {@code null} after reporting why not. */
    private static PlayerTimerData resolveTarget(CommandContext<CommandSourceStack> context,
                                                 ServerPlayer target, String dimensionId) {
        if (!TimerManager.isInstancedDimension(dimensionId)) {
            return TimerManager.getOrCreateAccount(target, dimensionId);
        }

        List<PlayerTimerData> live = TimerManager.liveSessions(target, dimensionId);
        if (live.size() == 1) {
            return live.get(0);
        }
        if (live.isEmpty()) {
            context.getSource().sendFailure(Component.translatable(
                    MSG_NO_SESSION, target.getName().getString(), dimensionId));
            return null;
        }
        // More than one visit to the same dungeon is live: refuse and list, rather than guess.
        // Each id is clickable so the operator can copy one into the explicit form.
        context.getSource().sendFailure(Component.translatable(
                MSG_AMBIGUOUS, target.getName().getString(), dimensionId, live.size()));
        for (PlayerTimerData d : live) {
            context.getSource().sendFailure(Component.translatable(
                    "cobblesafari.command.timer.session.candidate",
                    clickableScope(d.getScopeId()), d.getFormattedTime()));
        }
        return null;
    }

    /** Shared body: validate, resolve, mutate the credit, save, sync, report. */
    private static int applyToScope(CommandContext<CommandSourceStack> context, String successKey,
                                    boolean withSeconds, IntUnaryOperator newTicks) throws CommandSyntaxException {
        String dimensionId = StringArgumentType.getString(context, ARG_DIMENSION).trim();

        if (!SafariTimerConfig.hasDimensionTimer(dimensionId)) {
            context.getSource().sendFailure(Component.translatable(MSG_NOT_CONFIGURED, dimensionId));
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(context, ARG_PLAYER);
        PlayerTimerData data = resolveTarget(context, target, dimensionId);
        if (data == null) {
            return 0;
        }

        if (newTicks != null) {
            data.setRemainingTicks(Math.max(0, newTicks.applyAsInt(data.getRemainingTicks())));
            TimerManager.savePlayerData(target, data);
            syncIfCurrent(target, data);
        }

        int seconds = withSeconds ? IntegerArgumentType.getInteger(context, ARG_SECONDS) : 0;
        context.getSource().sendSuccess(() -> withSeconds
                ? Component.translatable(successKey, seconds, target.getName().getString(),
                        dimensionId, data.getFormattedTime())
                : Component.translatable(successKey, target.getName().getString(),
                        dimensionId, data.getFormattedTime()), true);
        return 1;
    }

    /** Pushes the new value to the HUD only when the player is actually looking at this timer. */
    private static void syncIfCurrent(ServerPlayer target, PlayerTimerData data) {
        TimerScope current = TimerManager.currentScope(target);
        if (current != null && current.key().equals(data.scope().key())) {
            TimerManager.syncToClient(target, data);
        }
    }

    static int executeAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int ticks = IntegerArgumentType.getInteger(context, ARG_SECONDS) * 20;
        return applyToScope(context, "cobblesafari.command.dimension.add.success", true,
                remaining -> remaining + ticks);
    }

    static int executeRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int ticks = IntegerArgumentType.getInteger(context, ARG_SECONDS) * 20;
        return applyToScope(context, "cobblesafari.command.dimension.remove.success", true,
                remaining -> remaining - ticks);
    }

    static int executeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int ticks = IntegerArgumentType.getInteger(context, ARG_SECONDS) * 20;
        return applyToScope(context, "cobblesafari.command.dimension.set.success", false,
                remaining -> ticks);
    }

    static int executeGet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return applyToScope(context, "cobblesafari.command.dimension.get.success", false, null);
    }

    // --- Explicit-scope forms, for when a player holds two live sessions of one dungeon ----------

    private static PlayerTimerData resolveExplicitScope(CommandContext<CommandSourceStack> context,
                                                        ServerPlayer target, String scopeId) {
        PlayerTimerData match = TimerManager.allLiveSessions(target).stream()
                .filter(d -> scopeId.equals(d.getScopeId()))
                .findFirst()
                .orElse(null);
        if (match == null) {
            context.getSource().sendFailure(Component.translatable(
                    "cobblesafari.command.timer.session.unknown_scope",
                    scopeId, target.getName().getString()));
        }
        return match;
    }

    private static int applyToExplicitScope(CommandContext<CommandSourceStack> context, String successKey,
                                            IntUnaryOperator newTicks) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, ARG_PLAYER);
        String scopeId = StringArgumentType.getString(context, ARG_SCOPE).trim();
        int seconds = IntegerArgumentType.getInteger(context, ARG_SECONDS);

        PlayerTimerData data = resolveExplicitScope(context, target, scopeId);
        if (data == null) {
            return 0;
        }

        data.setRemainingTicks(Math.max(0, newTicks.applyAsInt(data.getRemainingTicks())));
        TimerManager.savePlayerData(target, data);
        syncIfCurrent(target, data);

        context.getSource().sendSuccess(() -> Component.translatable(successKey, seconds,
                target.getName().getString(), data.getDimensionId(), data.getFormattedTime()), true);
        return 1;
    }

    static int executeSessionAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int ticks = IntegerArgumentType.getInteger(context, ARG_SECONDS) * 20;
        return applyToExplicitScope(context, "cobblesafari.command.dimension.add.success",
                remaining -> remaining + ticks);
    }

    static int executeSessionSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int ticks = IntegerArgumentType.getInteger(context, ARG_SECONDS) * 20;
        return applyToExplicitScope(context, "cobblesafari.command.dimension.set_session.success",
                remaining -> ticks);
    }

    /**
     * {@code timer session list <player>} - read-only. The session model is otherwise invisible in
     * game; this is what makes the explicit forms above usable, so every id it prints is clickable
     * and copies itself to the clipboard.
     */
    static int executeSessionList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, ARG_PLAYER);
        List<PlayerTimerData> live = TimerManager.allLiveSessions(target);

        if (live.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable(
                    "cobblesafari.command.timer.session.none", target.getName().getString()), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.timer.session.header",
                target.getName().getString(), live.size()), false);

        for (PlayerTimerData d : live) {
            String origin = d.getOriginPos() == null
                    ? "-"
                    : d.getOriginPos().toShortString()
                            + (d.getOriginDimension() == null ? "" : " @ " + d.getOriginDimension().location());
            context.getSource().sendSuccess(() -> Component.translatable(
                    "cobblesafari.command.timer.session.entry",
                    d.getDimensionId(), clickableScope(d.getScopeId()), d.getFormattedTime(), origin), false);
        }
        return 1;
    }
}
