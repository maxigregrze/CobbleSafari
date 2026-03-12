package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import maxigregrze.cobblesafari.cftrader.logic.CfTraderRegistry;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.entity.CfTraderEntity;
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
import java.util.Locale;
import java.util.Objects;

public class CobbleSafariCommand {

    private static final String ARG_TYPE = "type";
    private static final String ARG_VARIANT = "variant";
    private static final String ARG_PLAYER = "player";
    private static final String ARG_SECONDS = "seconds";
    private static final String ARG_DIMENSION = "dimension";
    private static final String VARIANT_SMALL = "small_sphere";
    private static final String VARIANT_LARGE = "large_sphere";
    private static final String VARIANT_TREASURES = "treasures";

    private static final List<String> SUMMON_TYPES = List.of("underground_small", "underground_large", "underground_treasure");
    private static final List<String> SUMMON_TEMPLATE_TYPES = List.of("underground_small_template", "underground_large_template", "underground_treasure_template");
    private static final SuggestionProvider<CommandSourceStack> SUMMON_TYPE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(getDynamicSummonSuggestions(false), builder);
    private static final SuggestionProvider<CommandSourceStack> SUMMON_TEMPLATE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(getDynamicSummonSuggestions(true), builder);
    private static final SuggestionProvider<CommandSourceStack> SUMMON_VARIANT_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(getVariantSuggestions(context, false), builder);
    private static final SuggestionProvider<CommandSourceStack> SUMMON_TEMPLATE_VARIANT_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(getVariantSuggestions(context, true), builder);
    private static final SuggestionProvider<CommandSourceStack> DIMENSION_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(SafariTimerConfig.getConfiguredDimensionIds(), builder);

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
                                .then(Commands.argument(ARG_TYPE, StringArgumentType.word())
                                        .suggests(SUMMON_TYPE_SUGGESTIONS)
                                        .executes(CobbleSafariCommand::executeSummon)
                                        .then(Commands.argument(ARG_VARIANT, StringArgumentType.word())
                                                .suggests(SUMMON_VARIANT_SUGGESTIONS)
                                                .executes(CobbleSafariCommand::executeSummon))))
                        .then(Commands.literal("summon_template")
                                .then(Commands.argument(ARG_TYPE, StringArgumentType.word())
                                        .suggests(SUMMON_TEMPLATE_SUGGESTIONS)
                                        .executes(CobbleSafariCommand::executeSummonTemplate)
                                        .then(Commands.argument(ARG_VARIANT, StringArgumentType.word())
                                                .suggests(SUMMON_TEMPLATE_VARIANT_SUGGESTIONS)
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
        String type = StringArgumentType.getString(context, ARG_TYPE);
        String variant = context.getNodes().stream().anyMatch(n -> ARG_VARIANT.equals(n.getNode().getName()))
                ? StringArgumentType.getString(context, ARG_VARIANT)
                : null;
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        CfTraderEntity traderEntity = new CfTraderEntity(ModEntities.CFTRADER_NPC, level);
        traderEntity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
        String traderName = resolveTraderName(type, false);
        if (traderName == null) {
            context.getSource().sendFailure(
                    Component.translatable("cobblesafari.command.underground.invalid_type", type));
            return 0;
        }
        traderEntity.setTraderName(traderName);
        String resolvedVariantRaw = variant;
        if (resolvedVariantRaw == null || resolvedVariantRaw.isBlank()) {
            resolvedVariantRaw = defaultVariantForType(type, traderName);
        }
        final String resolvedVariant = resolvedVariantRaw;
        traderEntity.initTradesForType(resolvedVariant);
        level.addFreshEntity(traderEntity);
        context.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.underground.summon.success", type),
                true);
        return 1;
    }
    
    private static int executeSummonTemplate(CommandContext<CommandSourceStack> context) {
        String type = StringArgumentType.getString(context, ARG_TYPE);
        String variant = context.getNodes().stream().anyMatch(n -> ARG_VARIANT.equals(n.getNode().getName()))
                ? StringArgumentType.getString(context, ARG_VARIANT)
                : null;
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        CfTraderEntity traderEntity = new CfTraderEntity(ModEntities.CFTRADER_NPC, level);
        traderEntity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
        String traderName = resolveTraderName(type, true);
        if (traderName == null) {
            context.getSource().sendFailure(
                    Component.translatable("cobblesafari.command.underground.invalid_type", type));
            return 0;
        }
        traderEntity.setTraderName(traderName);
        String resolvedVariantRaw = variant;
        if (resolvedVariantRaw == null || resolvedVariantRaw.isBlank()) {
            resolvedVariantRaw = defaultVariantForType(type, traderName);
        }
        final String resolvedVariant = resolvedVariantRaw;
        traderEntity.setTradeType(resolvedVariant);
        level.addFreshEntity(traderEntity);
        context.getSource().sendSuccess(
                () -> Component.literal("Summoned a CFTrader template (trader: " + traderName + ", variant: " + resolvedVariant + ") - trades will be randomized when placed in structures"),
                true);
        return 1;
    }

    private static String resolveTraderName(String rawType, boolean templateMode) {
        if (rawType == null || rawType.isBlank()) return null;
        String normalized = rawType.toLowerCase(Locale.ROOT);
        if (templateMode) {
            normalized = normalized.replace("_template", "");
        }
        if (SUMMON_TYPES.contains(normalized)) {
            return "hiker";
        }
        if (CfTraderRegistry.getTrader(normalized) != null) {
            return normalized;
        }
        return null;
    }

    private static String defaultVariantForType(String rawType, String traderName) {
        String normalized = rawType.toLowerCase(Locale.ROOT).replace("_template", "");
        return switch (normalized) {
            case "underground_small", "small" -> VARIANT_SMALL;
            case "underground_large", "large" -> VARIANT_LARGE;
            case "underground_treasure", "treasure" -> VARIANT_TREASURES;
            default -> CfTraderRegistry.getDefaultVariantId(traderName);
        };
    }

    private static List<String> getDynamicSummonSuggestions(boolean templateMode) {
        List<String> suggestions = new ArrayList<>();
        suggestions.addAll(templateMode ? SUMMON_TEMPLATE_TYPES : SUMMON_TYPES);
        suggestions.addAll(CfTraderRegistry.getTraderNames());
        return suggestions;
    }

    private static List<String> getVariantSuggestions(CommandContext<CommandSourceStack> context, boolean templateMode) {
        String type = StringArgumentType.getString(context, ARG_TYPE);
        String traderName = resolveTraderName(type, templateMode);
        if (traderName == null) return List.of();
        var trader = CfTraderRegistry.getTrader(traderName);
        if (trader == null) return List.of(VARIANT_SMALL, VARIANT_LARGE, VARIANT_TREASURES);
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

    private static int executeDungeonSpawnSelf(CommandContext<CommandSourceStack> context) {
        return DungeonCommand.executeSpawnSelf(context);
    }

    private static int executeDungeonSpawnForce(CommandContext<CommandSourceStack> context) {
        return DungeonCommand.executeSpawnForce(context);
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
