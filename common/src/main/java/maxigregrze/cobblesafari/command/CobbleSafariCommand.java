package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import maxigregrze.cobblesafari.cstrader.logic.CsTraderDefinition;
import maxigregrze.cobblesafari.cstrader.logic.CsTraderRegistry;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.entity.CsTraderEntity;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CobbleSafariCommand {

    private static final String ARG_NAME = "name";
    private static final String ARG_VARIANT = "variant";
    private static final String ARG_PLAYER = "player";
    private static final String ARG_SECONDS = "seconds";
    private static final String ARG_DIMENSION = "dimension";
    private static final SuggestionProvider<CommandSourceStack> SUMMON_NAME_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(CsTraderRegistry.getTraderNames(), builder);
    private static final SuggestionProvider<CommandSourceStack> SUMMON_VARIANT_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(getVariantSuggestions(context), builder);
    private static final SuggestionProvider<CommandSourceStack> DIMENSION_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(SafariTimerConfig.getConfiguredDimensionIds(), builder);
    private static final SuggestionProvider<CommandSourceStack> DUNGEON_SPAWN_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(DungeonDimensions.getRegisteredDungeonIdsSorted(), builder);

    private CobbleSafariCommand() {}

    public static void register() {
        // Registration is done by loaders (Fabric/NeoForge) via registerCommands()
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        Objects.requireNonNull(registryAccess);
        dispatcher.register(
                Commands.literal("cobblesafari")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(4))
                                .then(Commands.literal("safari")
                                        .executes(CobbleSafariCommand::executeResetSafari))
                                .then(Commands.literal("dungeon")
                                        .executes(CobbleSafariCommand::executeResetDungeon)))
                        .then(Commands.literal("summon")
                                .then(Commands.argument(ARG_NAME, StringArgumentType.word())
                                        .suggests(SUMMON_NAME_SUGGESTIONS)
                                        .then(Commands.argument(ARG_VARIANT, StringArgumentType.word())
                                                .suggests(SUMMON_VARIANT_SUGGESTIONS)
                                                .executes(CobbleSafariCommand::executeSummon))))
                        .then(Commands.literal("summon_template")
                                .then(Commands.argument(ARG_NAME, StringArgumentType.word())
                                        .suggests(SUMMON_NAME_SUGGESTIONS)
                                        .then(Commands.argument(ARG_VARIANT, StringArgumentType.word())
                                                .suggests(SUMMON_VARIANT_SUGGESTIONS)
                                                .executes(CobbleSafariCommand::executeSummonTemplate))))
                        .then(Commands.literal("timer")
                                .then(Commands.literal("safari")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                        .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(1))
                                                                .executes(CobbleSafariCommand::executeTimerSafariAdd))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                        .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(1))
                                                                .executes(CobbleSafariCommand::executeTimerSafariRemove))))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                        .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(0))
                                                                .executes(CobbleSafariCommand::executeTimerSafariSet))))
                                        .then(Commands.literal("get")
                                                .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                        .executes(CobbleSafariCommand::executeTimerSafariGet)))
                                        .then(Commands.literal("toggle")
                                                .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                        .executes(CobbleSafariCommand::executeTimerSafariToggle)
                                                        .then(Commands.argument("state", BoolArgumentType.bool())
                                                                .executes(CobbleSafariCommand::executeTimerSafariToggleWithState)))))
                                .then(Commands.literal(ARG_DIMENSION)
                                        .then(Commands.literal("add")
                                                .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                        .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(1))
                                                                .then(Commands.argument(ARG_DIMENSION, StringArgumentType.greedyString())
                                                                        .suggests(DIMENSION_SUGGESTIONS)
                                                                        .executes(CobbleSafariCommand::executeTimerDimensionAdd)))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                        .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(1))
                                                                .then(Commands.argument(ARG_DIMENSION, StringArgumentType.greedyString())
                                                                        .suggests(DIMENSION_SUGGESTIONS)
                                                                        .executes(CobbleSafariCommand::executeTimerDimensionRemove)))))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                        .then(Commands.argument(ARG_SECONDS, IntegerArgumentType.integer(0))
                                                                .then(Commands.argument(ARG_DIMENSION, StringArgumentType.greedyString())
                                                                        .suggests(DIMENSION_SUGGESTIONS)
                                                                        .executes(CobbleSafariCommand::executeTimerDimensionSet)))))
                                        .then(Commands.literal("get")
                                                .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                        .then(Commands.argument(ARG_DIMENSION, StringArgumentType.greedyString())
                                                                .suggests(DIMENSION_SUGGESTIONS)
                                                                .executes(CobbleSafariCommand::executeTimerDimensionGet))))))
                        .then(Commands.literal("refresh")
                                .requires(source -> source.hasPermission(4))
                                .executes(CobbleSafariCommand::executeRefresh))
                        .then(Commands.literal("dungeon")
                                .then(Commands.literal("spawn")
                                        .executes(CobbleSafariCommand::executeDungeonSpawnSelf)
                                        .then(Commands.literal("force")
                                                .executes(CobbleSafariCommand::executeDungeonSpawnForceSelf)
                                                .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                        .executes(CobbleSafariCommand::executeDungeonSpawnForce)))
                                        .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                .executes(CobbleSafariCommand::executeDungeonSpawn)))
                                .then(Commands.literal("list")
                                        .executes(CobbleSafariCommand::executeDungeonList)
                                        .then(Commands.literal("force")
                                                .executes(CobbleSafariCommand::executeDungeonForceList)))
                                .then(Commands.literal("dimensions")
                                        .executes(CobbleSafariCommand::executeDungeonDimensions)))
        );
    }

    private static int executeResetSafari(CommandContext<CommandSourceStack> context) {
        return ResetCommand.executeSafariReset(context);
    }

    private static int executeResetDungeon(CommandContext<CommandSourceStack> context) {
        return ResetCommand.executeDungeonReset(context);
    }

    private static int executeRefresh(CommandContext<CommandSourceStack> context) {
        return ResetCommand.executeRefresh(context);
    }

    private static int executeSummon(CommandContext<CommandSourceStack> context) {
        String traderName = StringArgumentType.getString(context, ARG_NAME);
        String variant = StringArgumentType.getString(context, ARG_VARIANT);
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        CsTraderDefinition trader = CsTraderRegistry.getTrader(traderName);
        if (trader == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.cstrader.invalid_name", traderName));
            return 0;
        }
        if (trader.resolveVariant(variant) == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.cstrader.invalid_variant", variant, traderName));
            return 0;
        }
        CsTraderEntity traderEntity = new CsTraderEntity(ModEntities.CSTRADER_NPC, level);
        traderEntity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
        traderEntity.setTraderName(traderName);
        traderEntity.initTradesForType(variant);
        level.addFreshEntity(traderEntity);
        context.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.cstrader.summon.success", traderName, variant),
                true);
        return 1;
    }
    
    private static int executeSummonTemplate(CommandContext<CommandSourceStack> context) {
        String traderName = StringArgumentType.getString(context, ARG_NAME);
        String variant = StringArgumentType.getString(context, ARG_VARIANT);
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        CsTraderDefinition trader = CsTraderRegistry.getTrader(traderName);
        if (trader == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.cstrader.invalid_name", traderName));
            return 0;
        }
        if (trader.resolveVariant(variant) == null) {
            context.getSource().sendFailure(Component.translatable("cobblesafari.command.cstrader.invalid_variant", variant, traderName));
            return 0;
        }
        CsTraderEntity traderEntity = new CsTraderEntity(ModEntities.CSTRADER_NPC, level);
        traderEntity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
        traderEntity.setTraderName(traderName);
        traderEntity.setTradeType(variant);
        level.addFreshEntity(traderEntity);
        context.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.cstrader.template.success", traderName, variant),
                true);
        return 1;
    }

    private static List<String> getVariantSuggestions(CommandContext<CommandSourceStack> context) {
        String traderName = StringArgumentType.getString(context, ARG_NAME);
        var trader = CsTraderRegistry.getTrader(traderName);
        if (trader == null) return List.of();
        List<String> variants = new ArrayList<>();
        trader.getVariants().forEach(v -> variants.add(v.getId()));
        return variants;
    }

    private static int executeTimerSafariAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return SafariTimerCommand.executeAdd(context);
    }

    private static int executeTimerSafariRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return SafariTimerCommand.executeRemove(context);
    }

    private static int executeTimerSafariSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return SafariTimerCommand.executeSet(context);
    }

    private static int executeTimerSafariGet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return SafariTimerCommand.executeGet(context);
    }

    private static int executeTimerSafariToggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return SafariTimerCommand.executeToggle(context);
    }

    private static int executeTimerSafariToggleWithState(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return SafariTimerCommand.executeToggleWithState(context);
    }

    private static int executeTimerDimensionAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return DimensionTimerCommand.executeAdd(context);
    }

    private static int executeTimerDimensionRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return DimensionTimerCommand.executeRemove(context);
    }

    private static int executeTimerDimensionSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return DimensionTimerCommand.executeSet(context);
    }

    private static int executeTimerDimensionGet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return DimensionTimerCommand.executeGet(context);
    }

    private static int executeDungeonSpawn(CommandContext<CommandSourceStack> context) {
        return DungeonCommand.executeSpawn(context);
    }

    private static int executeDungeonSpawnWithDungeon(CommandContext<CommandSourceStack> context) {
        return DungeonCommand.executeSpawnWithDungeon(context);
    }

    private static int executeDungeonSpawnSelf(CommandContext<CommandSourceStack> context) {
        return DungeonCommand.executeSpawnSelf(context);
    }

    private static int executeDungeonSpawnForce(CommandContext<CommandSourceStack> context) {
        return DungeonCommand.executeSpawnForce(context);
    }

    private static int executeDungeonSpawnForceWithDungeon(CommandContext<CommandSourceStack> context) {
        return DungeonCommand.executeSpawnForceWithDungeon(context);
    }

    private static int executeDungeonSpawnForceSelf(CommandContext<CommandSourceStack> context) {
        return DungeonCommand.executeSpawnForceSelf(context);
    }

    private static int executeDungeonList(CommandContext<CommandSourceStack> context) {
        return DungeonCommand.executeList(context);
    }

    private static int executeDungeonForceList(CommandContext<CommandSourceStack> context) {
        return DungeonCommand.executeForceList(context);
    }

    private static int executeDungeonDimensions(CommandContext<CommandSourceStack> context) {
        return DungeonCommand.executeDimensions(context);
    }

}
