package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.manager.TimerManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DimensionTimerCommand {

    private DimensionTimerCommand() {}

    static int executeAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String dimensionId = StringArgumentType.getString(context, "dimension").trim();
        
        if (!SafariTimerConfig.hasDimensionTimer(dimensionId)) {
            context.getSource().sendFailure(Component.translatable(
                    "cobblesafari.command.dimension.not_configured", dimensionId));
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        int ticks = seconds * 20;

        PlayerTimerData data = TimerManager.getOrCreateData(target, dimensionId);
        data.setRemainingTicks(data.getRemainingTicks() + ticks);
        TimerManager.savePlayerData(target, data);

        String currentDimension = target.level().dimension().location().toString();
        if (currentDimension.equals(dimensionId)) {
            TimerManager.syncToClient(target, data);
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.dimension.add.success",
                seconds,
                target.getName().getString(),
                dimensionId,
                data.getFormattedTime()
        ), true);

        return 1;
    }

    static int executeRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String dimensionId = StringArgumentType.getString(context, "dimension").trim();
        
        if (!SafariTimerConfig.hasDimensionTimer(dimensionId)) {
            context.getSource().sendFailure(Component.translatable(
                    "cobblesafari.command.dimension.not_configured", dimensionId));
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        int ticks = seconds * 20;

        PlayerTimerData data = TimerManager.getOrCreateData(target, dimensionId);
        int newTicks = Math.max(0, data.getRemainingTicks() - ticks);
        data.setRemainingTicks(newTicks);
        TimerManager.savePlayerData(target, data);

        String currentDimension = target.level().dimension().location().toString();
        if (currentDimension.equals(dimensionId)) {
            TimerManager.syncToClient(target, data);
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.dimension.remove.success",
                seconds,
                target.getName().getString(),
                dimensionId,
                data.getFormattedTime()
        ), true);

        return 1;
    }

    static int executeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String dimensionId = StringArgumentType.getString(context, "dimension").trim();
        
        if (!SafariTimerConfig.hasDimensionTimer(dimensionId)) {
            context.getSource().sendFailure(Component.translatable(
                    "cobblesafari.command.dimension.not_configured", dimensionId));
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        int ticks = seconds * 20;

        PlayerTimerData data = TimerManager.getOrCreateData(target, dimensionId);
        data.setRemainingTicks(ticks);
        TimerManager.savePlayerData(target, data);

        String currentDimension = target.level().dimension().location().toString();
        if (currentDimension.equals(dimensionId)) {
            TimerManager.syncToClient(target, data);
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.dimension.set.success",
                target.getName().getString(),
                dimensionId,
                data.getFormattedTime()
        ), true);

        return 1;
    }

    static int executeGet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String dimensionId = StringArgumentType.getString(context, "dimension").trim();
        
        if (!SafariTimerConfig.hasDimensionTimer(dimensionId)) {
            context.getSource().sendFailure(Component.translatable(
                    "cobblesafari.command.dimension.not_configured", dimensionId));
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        PlayerTimerData data = TimerManager.getOrCreateData(target, dimensionId);

        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.dimension.get.success",
                target.getName().getString(),
                dimensionId,
                data.getFormattedTime()
        ), true);

        return 1;
    }
}
