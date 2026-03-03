package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import maxigregrze.cobblesafari.entity.HikerEntity;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class UndergroundCommand {

    private static final List<String> TRADE_TYPES = List.of("small", "large", "treasure");

    private UndergroundCommand() {}

    static int executeSummon(CommandContext<CommandSourceStack> context) {
        String type = StringArgumentType.getString(context, "type");

        if (!TRADE_TYPES.contains(type.toLowerCase())) {
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
        hiker.initTradesForType(type.toLowerCase());
        level.addFreshEntity(hiker);

        context.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.underground.summon.success", type),
                true);
        return 1;
    }
}
