package maxigregrze.cobblesafari.compat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class CobbleDollarHelper {

    private static final String MOD_ID = "cobbledollars";

    private CobbleDollarHelper() {}

    public static boolean isAvailable() {
        return Services.PLATFORM.isModLoaded(MOD_ID);
    }

    public static boolean hasBalance(ServerPlayer player, int amount) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        CommandSourceStack source = server.createCommandSourceStack()
                .withSuppressedOutput()
                .withPermission(4);

        String command = "cobbledollars query " + player.getName().getString();
        try {
            int balance = server.getCommands().getDispatcher().execute(command, source);
            CobbleSafari.LOGGER.debug("Player {} has {} CobbleDollars (needs {})",
                    player.getName().getString(), balance, amount);
            return balance >= amount;
        } catch (CommandSyntaxException e) {
            CobbleSafari.LOGGER.debug("CobbleDollars query command failed for player {}: {}",
                    player.getName().getString(), e.getMessage());
            return false;
        }
    }

    public static boolean tryRemove(ServerPlayer player, int amount) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        if (!hasBalance(player, amount)) {
            CobbleSafari.LOGGER.debug("Player {} has insufficient balance for {} CobbleDollars",
                    player.getName().getString(), amount);
            return false;
        }

        CommandSourceStack source = server.createCommandSourceStack()
                .withSuppressedOutput()
                .withPermission(4);

        String command = "cobbledollars remove " + player.getName().getString() + " " + amount;
        try {
            int result = server.getCommands().getDispatcher().execute(command, source);
            if (result > 0) {
                CobbleSafari.LOGGER.info("Removed {} CobbleDollars from player {}", amount, player.getName().getString());
                return true;
            }
        } catch (CommandSyntaxException e) {
            CobbleSafari.LOGGER.debug("CobbleDollars remove command failed for player {}: {}",
                    player.getName().getString(), e.getMessage());
        }
        return false;
    }

    public static void give(ServerPlayer player, int amount) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        CommandSourceStack source = server.createCommandSourceStack()
                .withSuppressedOutput()
                .withPermission(4);

        String command = "cobbledollars give " + player.getName().getString() + " " + amount;
        try {
            int result = server.getCommands().getDispatcher().execute(command, source);
            if (result > 0) {
                CobbleSafari.LOGGER.info("Refunded {} CobbleDollars to player {}", amount, player.getName().getString());
            } else {
                CobbleSafari.LOGGER.error("Failed to refund {} CobbleDollars to player {}", amount, player.getName().getString());
            }
        } catch (CommandSyntaxException e) {
            CobbleSafari.LOGGER.error("CobbleDollars give command failed for player {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }
}
