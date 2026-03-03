package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.CommandDispatcher;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.manager.TimerManager;
import maxigregrze.cobblesafari.teleporter.TeleporterTickHandler;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class SafariExitCommand {

    private SafariExitCommand() {}

    public static void register() {
        // Registration is done by loaders (Fabric/NeoForge) via registerCommands()
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
                Commands.literal("safariexit")
                        .executes(context -> executeSafariExit(context.getSource()))
        );
    }

    private static int executeSafariExit(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        String safariDimensionId = SafariTimerConfig.getSafariDimensionId();
        ResourceLocation safariDimension = ResourceLocation.parse(safariDimensionId);
        ResourceLocation playerDimension = player.level().dimension().location();

        if (!playerDimension.equals(safariDimension)) {
            source.sendFailure(Component.translatable("cobblesafari.command.safariexit.not_in_safari"));
            return 0;
        }

        PlayerTimerData timerData = TimerManager.getOrCreateData(player, safariDimensionId);
        BlockPos originPos = timerData.getOriginPos();
        ResourceKey<Level> originDimension = timerData.getOriginDimension();

        if (originPos == null || originDimension == null) {
            source.sendFailure(Component.translatable("cobblesafari.command.safariexit.no_origin"));
            return 0;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return 0;

        ServerLevel targetLevel = server.getLevel(originDimension);
        if (targetLevel == null) {
            source.sendFailure(Component.translatable("cobblesafari.command.safariexit.invalid_origin"));
            return 0;
        }

        player.teleportTo(
                targetLevel,
                originPos.getX() + 0.5,
                originPos.getY(),
                originPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );

        TeleporterTickHandler.setTeleportCooldown(player);

        source.sendSuccess(() -> Component.translatable("cobblesafari.command.safariexit.success"), false);
        CobbleSafari.LOGGER.info("Player {} exited safari using /safariexit command", player.getName().getString());

        return 1;
    }
}
