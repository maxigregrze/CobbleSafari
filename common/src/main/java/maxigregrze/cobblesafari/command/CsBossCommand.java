package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.BossBattleManager;
import maxigregrze.cobblesafari.csboss.CsBossDefinition;
import maxigregrze.cobblesafari.csboss.CsBossRegistry;
import maxigregrze.cobblesafari.csboss.ParticipantState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Commandes admin du Boss Battle System. Permission niveau 2.
 */
public final class CsBossCommand {

    private static final String ARG_PAGE = "page";
    private static final String ARG_SESSION = "sessionId";
    private static final String ARG_BOSS = "csbossId";

    private static final int SESSIONS_PER_PAGE = 5;
    private static final int BOSSES_PER_PAGE = 10;

    private CsBossCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("csboss")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("list")
                        .executes(ctx -> listSessions(ctx, 1))
                        .then(Commands.argument(ARG_PAGE, IntegerArgumentType.integer(1))
                                .executes(ctx -> listSessions(ctx, IntegerArgumentType.getInteger(ctx, ARG_PAGE)))))
                .then(Commands.literal("forcewin")
                        .then(Commands.argument(ARG_SESSION, IntegerArgumentType.integer(1))
                                .executes(ctx -> forceWin(ctx, IntegerArgumentType.getInteger(ctx, ARG_SESSION)))))
                .then(Commands.literal("stop")
                        .then(Commands.argument(ARG_SESSION, IntegerArgumentType.integer(1))
                                .executes(ctx -> stop(ctx, IntegerArgumentType.getInteger(ctx, ARG_SESSION)))))
                .then(Commands.literal("status")
                        .then(Commands.argument(ARG_SESSION, IntegerArgumentType.integer(1))
                                .executes(ctx -> status(ctx, IntegerArgumentType.getInteger(ctx, ARG_SESSION)))))
                .then(Commands.literal("bosses")
                        .then(Commands.literal("list")
                                .executes(ctx -> listBosses(ctx, 1))
                                .then(Commands.argument(ARG_PAGE, IntegerArgumentType.integer(1))
                                        .executes(ctx -> listBosses(ctx, IntegerArgumentType.getInteger(ctx, ARG_PAGE)))))
                        .then(Commands.literal("info")
                                .then(Commands.argument(ARG_BOSS, StringArgumentType.word())
                                        .executes(ctx -> bossInfo(ctx, StringArgumentType.getString(ctx, ARG_BOSS))))));
    }

    private static int listSessions(CommandContext<CommandSourceStack> ctx, int page) {
        List<BossBattleSession> all = BossBattleManager.sessions();
        if (all.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.csboss.list_empty"), false);
            return 0;
        }
        int from = (page - 1) * SESSIONS_PER_PAGE;
        int to = Math.min(from + SESSIONS_PER_PAGE, all.size());
        if (from >= all.size()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.csboss.list_empty"), false);
            return 0;
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csboss.sessions.header", page), false);
        for (int i = from; i < to; i++) {
            BossBattleSession s = all.get(i);
            long alive = s.getParticipants().values().stream().filter(p -> !p.discarded).count();
            final int sessionId = s.getId();
            final String bossId = s.getDefinition().bossId();
            final long aliveCount = alive;
            final int remainingSeconds = s.getRemaining() / 20;
            final String dimension = s.getDimension().location().toString();
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.csboss.sessions.entry",
                            sessionId,
                            bossId,
                            aliveCount,
                            remainingSeconds,
                            dimension),
                    false);
        }
        return 1;
    }

    private static int forceWin(CommandContext<CommandSourceStack> ctx, int id) {
        boolean ok = BossBattleManager.forceWin(ctx.getSource().getServer(), id);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        ok
                                ? "cobblesafari.command.csboss.forcewin.success"
                                : "cobblesafari.command.csboss.session.not_found",
                        id),
                true);
        return ok ? 1 : 0;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx, int id) {
        boolean ok = BossBattleManager.forceStop(ctx.getSource().getServer(), id);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        ok
                                ? "cobblesafari.command.csboss.stop.success"
                                : "cobblesafari.command.csboss.session.not_found",
                        id),
                true);
        return ok ? 1 : 0;
    }

    private static int status(CommandContext<CommandSourceStack> ctx, int id) {
        BossBattleSession s = BossBattleManager.session(id);
        if (s == null) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.csboss.session.not_found", id), false);
            return 0;
        }
        final int sessionId = s.getId();
        final String bossId = s.getDefinition().bossId();
        final int remainingSeconds = s.getRemaining() / 20;
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "cobblesafari.command.csboss.status.header", sessionId, bossId, remainingSeconds),
                false);
        long alive = 0;
        for (Map.Entry<UUID, ParticipantState> e : s.getParticipants().entrySet()) {
            ParticipantState st = e.getValue();
            if (!st.discarded) {
                alive++;
            }
            ServerPlayer p = ctx.getSource().getServer().getPlayerList().getPlayer(e.getKey());
            String name = p != null ? p.getGameProfile().getName() : e.getKey().toString();
            Component state = participantStateLabel(st);
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.csboss.status.participant", name, state),
                    false);
        }
        final long aliveCount = alive;
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csboss.status.alive", aliveCount), false);
        return 1;
    }

    private static Component participantStateLabel(ParticipantState st) {
        if (st.discarded) {
            return Component.translatable("cobblesafari.command.csboss.status.participant.out");
        }
        if (st.inRadius) {
            return Component.translatable("cobblesafari.command.csboss.status.participant.in_arena");
        }
        return Component.translatable("cobblesafari.command.csboss.status.participant.out_of_range");
    }

    private static int listBosses(CommandContext<CommandSourceStack> ctx, int page) {
        List<String> ids = CsBossRegistry.allIds();
        if (ids.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.csboss.bosses.none_loaded"), false);
            return 0;
        }
        int from = (page - 1) * BOSSES_PER_PAGE;
        int to = Math.min(from + BOSSES_PER_PAGE, ids.size());
        if (from >= ids.size()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.csboss.bosses.page_empty", page), false);
            return 0;
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csboss.bosses.header", page), false);
        for (int i = from; i < to; i++) {
            String id = ids.get(i);
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.csboss.bosses.entry", id), false);
        }
        return 1;
    }

    private static int bossInfo(CommandContext<CommandSourceStack> ctx, String id) {
        CsBossDefinition def = CsBossRegistry.get(id).orElse(null);
        if (def == null) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.csboss.bosses.not_found", id), false);
            return 0;
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csboss.bosses.info.header", def.bossId()), false);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "cobblesafari.command.csboss.bosses.info.display_name", def.effectiveDisplayName()),
                false);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csboss.bosses.info.species", def.specie()), false);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "cobblesafari.command.csboss.bosses.info.minion", def.effectiveMinionSpecie()),
                false);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csboss.bosses.info.tags", def.tags()), false);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "cobblesafari.command.csboss.bosses.info.duration",
                        def.minimumDuration(),
                        def.maximumDuration()),
                false);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "cobblesafari.command.csboss.bosses.info.size", def.size(), def.isStatic()),
                false);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "cobblesafari.command.csboss.bosses.info.cooldown",
                        def.moveCooldownMin(),
                        def.moveCooldownMax()),
                false);
        Component moveSet = def.moveSet().isEmpty()
                ? Component.translatable("cobblesafari.command.csboss.bosses.info.move_set_defaults")
                : Component.literal(def.moveSet().toString());
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csboss.bosses.info.move_set", moveSet), false);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csboss.bosses.info.rewards", def.rewards()), false);
        if (def.uniqueReward() != null) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.csboss.bosses.info.rewards_unique", def.uniqueReward()),
                    false);
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "cobblesafari.command.csboss.bosses.info.bar", def.healthStyle(), def.healthColor()),
                false);
        if (def.hasSecondPhase()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.csboss.bosses.info.second_phase",
                            def.secondPhase(),
                            def.giveRewardsBeforeSecondPhase()),
                    false);
        }
        return 1;
    }
}
