package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.manager.TimerManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SafariTimerCommand {

    private SafariTimerCommand() {}

    static int executeAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        int ticks = seconds * 20;

        PlayerTimerData data = TimerManager.getOrCreateData(target);
        data.setRemainingTicks(data.getRemainingTicks() + ticks);
        TimerManager.savePlayerData(target, data);

        if (TimerManager.isInSafariDimension(target)) {
            TimerManager.syncToClient(target, data);
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.add.success",
                seconds,
                target.getName().getString(),
                data.getFormattedTime()
        ), true);

        return 1;
    }

    static int executeRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        int ticks = seconds * 20;

        PlayerTimerData data = TimerManager.getOrCreateData(target);
        int newTicks = Math.max(0, data.getRemainingTicks() - ticks);
        data.setRemainingTicks(newTicks);
        TimerManager.savePlayerData(target, data);

        if (TimerManager.isInSafariDimension(target)) {
            TimerManager.syncToClient(target, data);
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.remove.success",
                seconds,
                target.getName().getString(),
                data.getFormattedTime()
        ), true);

        return 1;
    }

    static int executeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        int ticks = seconds * 20;

        PlayerTimerData data = TimerManager.getOrCreateData(target);
        data.setRemainingTicks(ticks);
        TimerManager.savePlayerData(target, data);

        if (TimerManager.isInSafariDimension(target)) {
            TimerManager.syncToClient(target, data);
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.set.success",
                target.getName().getString(),
                data.getFormattedTime()
        ), true);

        return 1;
    }

    static int executeGet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        PlayerTimerData data = TimerManager.getOrCreateData(target);

        context.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.get.success",
                target.getName().getString(),
                data.getFormattedTime()
        ), true);

        return 1;
    }

    static int executeToggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        TimerManager.toggleBypass(target, null);

        PlayerTimerData data = TimerManager.getOrCreateData(target);
        boolean bypassed = data.isTimerBypassed();

        String key = bypassed
                ? "cobblesafari.command.toggle.enabled"
                : "cobblesafari.command.toggle.disabled";

        context.getSource().sendSuccess(() -> Component.translatable(
                key,
                target.getName().getString()
        ), true);

        return 1;
    }

    static int executeToggleWithState(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        boolean state = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "state");

        TimerManager.toggleBypass(target, state);

        String key = state
                ? "cobblesafari.command.toggle.enabled"
                : "cobblesafari.command.toggle.disabled";

        context.getSource().sendSuccess(() -> Component.translatable(
                key,
                target.getName().getString()
        ), true);

        return 1;
    }
}
