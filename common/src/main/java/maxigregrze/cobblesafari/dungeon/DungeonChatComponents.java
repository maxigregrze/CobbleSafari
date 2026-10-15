package maxigregrze.cobblesafari.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Clickable chat fragments shared by the dungeon commands and the portal notification
 * (action plan 188, P2).
 */
public final class DungeonChatComponents {

    /** Permission level that may use the teleport link (vanilla's /tp level). */
    private static final int TELEPORT_PERMISSION_LEVEL = 2;

    private DungeonChatComponents() {}

    /** A teleport link for operators, a copy-to-clipboard link for everyone else. */
    public static Component coordinates(ServerPlayer viewer, ResourceKey<Level> dimension, BlockPos pos) {
        return viewer.hasPermissions(TELEPORT_PERMISSION_LEVEL)
                ? teleportLink(dimension.location().toString(), pos)
                : copyLink(pos);
    }

    /**
     * A position the viewer can click to travel to. Runs {@code /execute in <dim> run tp @s ...}
     * <b>as the player</b>, so it is gated by their own permissions: someone allowed to use
     * CobbleSafari commands but not teleportation gets vanilla's "unknown command" reply rather
     * than a silent no-op or an escalation.
     */
    public static Component teleportLink(String dimensionId, BlockPos pos) {
        String command = String.format("/execute in %s run tp @s %d %d %d",
                dimensionId, pos.getX(), pos.getY(), pos.getZ());
        return Component.literal(format(pos)).withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("cobblesafari.chat.click_teleport"))));
    }

    /** The position as plain "x y z" text that is copied to the clipboard on click. */
    public static Component copyLink(BlockPos pos) {
        return copyText(format(pos));
    }

    /** Any text that is copied to the clipboard on click. */
    public static Component copyText(String text) {
        return Component.literal(text).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("cobblesafari.chat.click_copy"))));
    }

    private static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
