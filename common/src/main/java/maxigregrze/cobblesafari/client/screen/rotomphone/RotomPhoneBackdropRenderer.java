package maxigregrze.cobblesafari.client.screen.rotomphone;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneClientCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class RotomPhoneBackdropRenderer {

    private static final String TEX_PATH = "textures/gui/rotomphone/";

    private RotomPhoneBackdropRenderer() {}

    public static void renderFullPhone(GuiGraphics graphics, int screenWidth, int screenHeight,
                                       boolean shinyStatus, String currentSkin) {
        int originX = (screenWidth - RotomPhoneBaseScreen.MAIN_W) / 2;
        int originY = (screenHeight - RotomPhoneBaseScreen.MAIN_H) / 2;

        ResourceLocation top = shinyStatus
                ? loc("gui_rotomphone_top-s.png")
                : loc("gui_rotomphone_top.png");
        graphics.blit(top, originX, originY - RotomPhoneBaseScreen.TOP_H, 0, 0,
                RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.TOP_H,
                RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.TOP_H);

        ResourceLocation main = shinyStatus
                ? loc("gui_rotomphone_main-s.png")
                : loc("gui_rotomphone_main.png");
        graphics.blit(main, originX, originY, 0, 0,
                RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.MAIN_H,
                RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.MAIN_H);

        ResourceLocation bottom = shinyStatus
                ? loc("gui_rotomphone_bottom-s.png")
                : loc("gui_rotomphone_bottom.png");
        graphics.blit(bottom, originX, originY + RotomPhoneBaseScreen.MAIN_H, 0, 0,
                RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.BOTTOM_H,
                RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.BOTTOM_H);

        RotomPhoneConfigSyncPayload.SkinData skinData = skinDataFor(currentSkin);
        ResourceLocation screenTex;
        if (skinData != null && skinData.hasCustomScreen() && currentSkin != null && !currentSkin.isEmpty()) {
            screenTex = loc("gui_rotomphone_screen_skin_" + currentSkin + ".png");
        } else if (shinyStatus) {
            screenTex = loc("gui_rotomphone_screen-s.png");
        } else {
            screenTex = loc("gui_rotomphone_screen.png");
        }
        graphics.blit(screenTex, originX, originY, 0, 0,
                RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.MAIN_H,
                RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.MAIN_H);

        if (currentSkin != null && !currentSkin.isEmpty()) {
            boolean useShinyVariant = shinyStatus && skinData != null && skinData.hasShinyVariant();
            ResourceLocation skinTex = useShinyVariant
                    ? loc("gui_rotomphone_skin_" + currentSkin + "-s.png")
                    : loc("gui_rotomphone_skin_" + currentSkin + ".png");
            graphics.blit(skinTex, originX, originY - RotomPhoneBaseScreen.TOP_H, 0, 0,
                    RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.SKIN_H,
                    RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.SKIN_H);
        }
    }

    private static RotomPhoneConfigSyncPayload.SkinData skinDataFor(String currentSkin) {
        if (currentSkin == null || currentSkin.isEmpty()) return null;
        for (RotomPhoneConfigSyncPayload.SkinData s : RotomPhoneClientCache.getCachedSkins()) {
            if (s.id().equalsIgnoreCase(currentSkin)) return s;
        }
        return null;
    }

    private static ResourceLocation loc(String filename) {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, TEX_PATH + filename);
    }
}
