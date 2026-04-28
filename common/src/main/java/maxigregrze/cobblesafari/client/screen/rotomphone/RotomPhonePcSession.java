package maxigregrze.cobblesafari.client.screen.rotomphone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class RotomPhonePcSession {

    private static boolean active;
    private static String rotomName;
    private static boolean shinyStatus;
    private static String currentSkin;
    private static boolean safetyMode;

    private RotomPhonePcSession() {}

    public static void activateFromPhoneMenu(String rotomName, boolean shiny, String skin, boolean safety) {
        RotomPhonePcSession.active = true;
        RotomPhonePcSession.rotomName = rotomName;
        RotomPhonePcSession.shinyStatus = shiny;
        RotomPhonePcSession.currentSkin = skin != null ? skin : "";
        RotomPhonePcSession.safetyMode = safety;
    }

    public static boolean isActive() {
        return active;
    }

    public static void clear() {
        active = false;
    }

    public static void returnToMenu() {
        clear();
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new RotomPhoneMenuScreen(rotomName, shinyStatus, currentSkin, safetyMode));
    }

    public static void renderBackdropIfNeeded(Screen screen, net.minecraft.client.gui.GuiGraphics graphics) {
        if (!active || !isLikelyCobblemonPcScreen(screen)) return;
        RotomPhoneBackdropRenderer.renderFullPhone(graphics, screen.width, screen.height, shinyStatus, currentSkin);
    }

    public static boolean handleEscapeOnPc(Screen screen, int keyCode) {
        if (!active || keyCode != 256 || !isLikelyCobblemonPcScreen(screen)) return false;
        returnToMenu();
        return true;
    }

    public static void tickCleanup(Minecraft mc) {
        if (!active) return;
        if (mc.screen == null) {
            clear();
            return;
        }
        if (isLikelyCobblemonPcScreen(mc.screen)) return;
        if (mc.screen instanceof RotomPhoneBaseScreen) return;
        clear();
    }

    public static boolean isLikelyCobblemonPcScreen(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?>)) return false;
        String name = screen.getClass().getName().toLowerCase();
        return name.contains("cobblemon") && name.contains("pc");
    }
}
