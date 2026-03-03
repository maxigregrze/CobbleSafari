package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.entity.HikerEntity;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class CobbleSafariCommand {

    private static final List<String> SUMMON_TYPES = List.of("underground_small", "underground_large", "underground_treasure");
    private static final List<String> SUMMON_TEMPLATE_TYPES = List.of("underground_small_template", "underground_large_template", "underground_treasure_template");
    private static final SuggestionProvider<CommandSourceStack> SUMMON_TYPE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(SUMMON_TYPES, builder);
    private static final SuggestionProvider<CommandSourceStack> SUMMON_TEMPLATE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(SUMMON_TEMPLATE_TYPES, builder);
    private static final SuggestionProvider<CommandSourceStack> DIMENSION_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(SafariTimerConfig.getConfiguredDimensionIds(), builder);

    private CobbleSafariCommand() {}

    public static void register() {
        // Registration is done by loaders (Fabric/NeoForge) via registerCommands()
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
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
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(SUMMON_TYPE_SUGGESTIONS)
                                        .executes(CobbleSafariCommand::executeSummon)))
                        .then(Commands.literal("summon_template")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(SUMMON_TEMPLATE_SUGGESTIONS)
                                        .executes(CobbleSafariCommand::executeSummonTemplate)))
                        .then(Commands.literal("timer")
                                .then(Commands.literal("safari")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                                                .executes(CobbleSafariCommand::executeTimerSafariAdd))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                                                .executes(CobbleSafariCommand::executeTimerSafariRemove))))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                                                .executes(CobbleSafariCommand::executeTimerSafariSet))))
                                        .then(Commands.literal("get")
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(CobbleSafariCommand::executeTimerSafariGet)))
                                        .then(Commands.literal("toggle")
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(CobbleSafariCommand::executeTimerSafariToggle)
                                                        .then(Commands.argument("state", BoolArgumentType.bool())
                                                                .executes(CobbleSafariCommand::executeTimerSafariToggleWithState)))))
                                .then(Commands.literal("dimension")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                                                        .suggests(DIMENSION_SUGGESTIONS)
                                                                        .executes(CobbleSafariCommand::executeTimerDimensionAdd)))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                                                        .suggests(DIMENSION_SUGGESTIONS)
                                                                        .executes(CobbleSafariCommand::executeTimerDimensionRemove)))))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                                                        .suggests(DIMENSION_SUGGESTIONS)
                                                                        .executes(CobbleSafariCommand::executeTimerDimensionSet)))))
                                        .then(Commands.literal("get")
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
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
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(CobbleSafariCommand::executeDungeonSpawnForce)))
                                        .then(Commands.argument("player", EntityArgument.player())
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
        String type = StringArgumentType.getString(context, "type");
        if (!SUMMON_TYPES.contains(type.toLowerCase())) {
            context.getSource().sendFailure(
                    Component.translatable("cobblesafari.command.underground.invalid_type", type));
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        HikerEntity hiker = new HikerEntity(ModEntities.HIKER, level);
        hiker.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
        hiker.initTradesForType(toInternalTradeType(type.toLowerCase()));
        level.addFreshEntity(hiker);
        context.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.underground.summon.success", type),
                true);
        return 1;
    }
    
    private static int executeSummonTemplate(CommandContext<CommandSourceStack> context) {
        String type = StringArgumentType.getString(context, "type");
        if (!SUMMON_TEMPLATE_TYPES.contains(type.toLowerCase())) {
            context.getSource().sendFailure(
                    Component.translatable("cobblesafari.command.underground.invalid_type", type));
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        HikerEntity hiker = new HikerEntity(ModEntities.HIKER, level);
        hiker.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
        String internalType = toInternalTradeType(type.toLowerCase().replace("_template", ""));
        hiker.setTradeType(internalType);
        level.addFreshEntity(hiker);
        context.getSource().sendSuccess(
                () -> Component.literal("Summoned a Hiker template (type: " + internalType + ") - trades will be randomized when placed in structures"),
                true);
        return 1;
    }

    private static String toInternalTradeType(String type) {
        return switch (type) {
            case "underground_small" -> "small";
            case "underground_large" -> "large";
            case "underground_treasure" -> "treasure";
            default -> "small";
        };
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
