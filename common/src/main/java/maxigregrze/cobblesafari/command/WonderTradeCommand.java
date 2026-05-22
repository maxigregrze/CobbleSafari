package maxigregrze.cobblesafari.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import maxigregrze.cobblesafari.config.WonderTradeSettings;
import maxigregrze.cobblesafari.wondertrade.WonderTradeEventRegistry;
import maxigregrze.cobblesafari.wondertrade.WonderTradeService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class WonderTradeCommand {
    private static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_DURATION = "duration";
    private static final String ARG_PLAYER = "player";
    private static final String ARG_SLOT = "slot";
    private static final String ARG_PLAYER_NAME = "playerName";
    private static final String ARG_AMOUNT = "amount";

    private static final DynamicCommandExceptionType UNKNOWN_PLAYER_NAME =
            new DynamicCommandExceptionType(
                    obj -> Component.translatable("cobblesafari.command.wondertrade.ticket_player_not_found", obj));

    private static final SimpleCommandExceptionType TICKETS_UNLIMITED_MODE =
            new SimpleCommandExceptionType(Component.translatable("cobblesafari.command.wondertrade.ticket_unlimited_mode"));

    private static final SuggestionProvider<CommandSourceStack> EVENT_ID_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    new ArrayList<>(WonderTradeEventRegistry.getEventIds()), builder);

    private WonderTradeCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("wondertrade")
                .then(Commands.literal("event")
                        .then(Commands.literal("start")
                                .then(Commands.argument(ARG_EVENT_ID, StringArgumentType.word())
                                        .suggests(EVENT_ID_SUGGESTIONS)
                                        .executes(WonderTradeCommand::eventStartNoDuration)
                                        .then(Commands.argument(ARG_DURATION, IntegerArgumentType.integer(0))
                                                .executes(WonderTradeCommand::eventStartWithDuration))))
                        .then(Commands.literal("list").executes(WonderTradeCommand::eventList))
                        .then(Commands.literal("stop").executes(WonderTradeCommand::eventStop)))
                .then(Commands.literal("resetpool")
                        .executes(ctx -> resetPool(ctx, false))
                        .then(Commands.literal("onlygenerated").executes(ctx -> resetPool(ctx, true))))
                .then(Commands.literal("ticket")
                        .then(Commands.literal("add")
                                .then(Commands.argument(ARG_PLAYER_NAME, StringArgumentType.word())
                                        .then(Commands.argument(ARG_AMOUNT, IntegerArgumentType.integer(1))
                                                .executes(WonderTradeCommand::ticketAdd))))
                        .then(Commands.literal("set")
                                .then(Commands.argument(ARG_PLAYER_NAME, StringArgumentType.word())
                                        .then(Commands.argument(ARG_AMOUNT, IntegerArgumentType.integer(0))
                                                .executes(WonderTradeCommand::ticketSet))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument(ARG_PLAYER_NAME, StringArgumentType.word())
                                        .then(Commands.argument(ARG_AMOUNT, IntegerArgumentType.integer(1))
                                                .executes(WonderTradeCommand::ticketRemove))))
                        .then(Commands.literal("get")
                                .then(Commands.argument(ARG_PLAYER_NAME, StringArgumentType.word())
                                        .executes(WonderTradeCommand::ticketGet))))
                .then(Commands.literal("test-trade")
                        .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                .then(Commands.argument(ARG_SLOT, IntegerArgumentType.integer(1, 6))
                                        .executes(WonderTradeCommand::testTrade))));
    }

    private static int resetPool(CommandContext<CommandSourceStack> ctx, boolean onlyGenerated) {
        WonderTradeService.PoolResetSummary summary =
                WonderTradeService.resetPoolAndRefill(ctx.getSource().getServer(), onlyGenerated);
        if (onlyGenerated) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.wondertrade.resetpool_only_generated",
                            summary.removedEntries(),
                            summary.poolSizeAfter()),
                    true);
        } else {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.wondertrade.resetpool_all",
                            summary.removedEntries(),
                            summary.poolSizeAfter()),
                    true);
        }
        return 1;
    }

    private static UUID resolvePlayerUuid(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        MinecraftServer server = ctx.getSource().getServer();
        String name = StringArgumentType.getString(ctx, ARG_PLAYER_NAME);
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            return online.getUUID();
        }
        Optional<GameProfile> cached = server.getProfileCache().get(name);
        if (cached.isPresent() && cached.get().getId() != null) {
            return cached.get().getId();
        }
        throw UNKNOWN_PLAYER_NAME.create(name);
    }

    private static void ensureTicketsApplicable() throws CommandSyntaxException {
        if (WonderTradeSettings.get().isUnlimitedDailyTrades()) {
            throw TICKETS_UNLIMITED_MODE.create();
        }
    }

    private static int ticketAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ensureTicketsApplicable();
        UUID id = resolvePlayerUuid(ctx);
        int amount = IntegerArgumentType.getInteger(ctx, ARG_AMOUNT);
        WonderTradeService.addPlayerTickets(ctx.getSource().getServer(), id, amount);
        int after = WonderTradeService.getEffectiveTicketCount(ctx.getSource().getServer(), id);
        String name = StringArgumentType.getString(ctx, ARG_PLAYER_NAME);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.wondertrade.ticket_add", name, amount, after),
                true);
        return 1;
    }

    private static int ticketSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ensureTicketsApplicable();
        UUID id = resolvePlayerUuid(ctx);
        int amount = IntegerArgumentType.getInteger(ctx, ARG_AMOUNT);
        WonderTradeService.setPlayerTickets(ctx.getSource().getServer(), id, amount);
        String name = StringArgumentType.getString(ctx, ARG_PLAYER_NAME);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.wondertrade.ticket_set", name, amount),
                true);
        return 1;
    }

    private static int ticketRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ensureTicketsApplicable();
        UUID id = resolvePlayerUuid(ctx);
        int amount = IntegerArgumentType.getInteger(ctx, ARG_AMOUNT);
        WonderTradeService.removePlayerTickets(ctx.getSource().getServer(), id, amount);
        int after = WonderTradeService.getEffectiveTicketCount(ctx.getSource().getServer(), id);
        String name = StringArgumentType.getString(ctx, ARG_PLAYER_NAME);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.wondertrade.ticket_remove", name, amount, after),
                true);
        return 1;
    }

    private static int ticketGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        UUID id = resolvePlayerUuid(ctx);
        String name = StringArgumentType.getString(ctx, ARG_PLAYER_NAME);
        int n = WonderTradeService.getEffectiveTicketCount(ctx.getSource().getServer(), id);
        if (n == Integer.MAX_VALUE) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.wondertrade.ticket_get_unlimited", name),
                    false);
        } else {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.wondertrade.ticket_get", name, n),
                    false);
        }
        return 1;
    }

    private static int eventStartNoDuration(CommandContext<CommandSourceStack> ctx) {
        String eventId = StringArgumentType.getString(ctx, ARG_EVENT_ID);
        if (!WonderTradeService.startEvent(ctx.getSource().getServer(), eventId, null)) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.wondertrade.event_unknown", eventId));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.wondertrade.event_started", eventId), true);
        return 1;
    }

    private static int eventStartWithDuration(CommandContext<CommandSourceStack> ctx) {
        String eventId = StringArgumentType.getString(ctx, ARG_EVENT_ID);
        int duration = IntegerArgumentType.getInteger(ctx, ARG_DURATION);
        if (!WonderTradeService.startEvent(ctx.getSource().getServer(), eventId, duration)) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.wondertrade.event_unknown", eventId));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.wondertrade.event_started_duration", eventId, duration), true);
        return 1;
    }

    private static int eventList(CommandContext<CommandSourceStack> ctx) {
        List<String> ids = new ArrayList<>(WonderTradeEventRegistry.getEventIds());
        ids.sort(String::compareToIgnoreCase);
        if (ids.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.wondertrade.event_list_empty"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.wondertrade.event_list", String.join(", ", ids)), false);
        }
        return 1;
    }

    private static int eventStop(CommandContext<CommandSourceStack> ctx) {
        WonderTradeService.stopEvent(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.wondertrade.event_stopped"), true);
        return 1;
    }

    private static int testTrade(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_PLAYER);
        int operatorSlot = IntegerArgumentType.getInteger(ctx, ARG_SLOT);
        int internalSlot = operatorSlot - 1;
        WonderTradeService.TradeResult r = WonderTradeService.tryTrade(target, internalSlot).result();
        return switch (r) {
            case SUCCESS -> {
                ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.wondertrade.test_trade_success", target.getName().getString(), operatorSlot), true);
                yield 1;
            }
            case EMPTY_SLOT -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.wondertrade.empty_slot", operatorSlot));
                yield 0;
            }
            case POOL_EMPTY -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.wondertrade.pool_empty"));
                yield 0;
            }
            case NO_CREDITS -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.wondertrade.no_credits"));
                yield 0;
            }
            case BANNED_DEPOSIT, BANNED_HELD_ITEM, LEVEL_OUT_OF_BOUNDS, EV_OVERFLOW -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.wondertrade.rejected_deposit"));
                yield 0;
            }
            case ERROR -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.wondertrade.error"));
                yield 0;
            }
        };
    }
}
