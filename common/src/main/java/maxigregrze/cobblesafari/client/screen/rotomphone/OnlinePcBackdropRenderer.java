package maxigregrze.cobblesafari.client.screen.rotomphone;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class OnlinePcBackdropRenderer {

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(
            CobbleSafari.MOD_ID, "textures/gui/rotomphone/gui_onlinepc.png");

    private OnlinePcBackdropRenderer() {}

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        int originX = (screenWidth - RotomPhoneBaseScreen.MAIN_W) / 2;
        int originY = (screenHeight - RotomPhoneBaseScreen.MAIN_H) / 2;
        graphics.blit(TEX, originX, originY, 0, 0,
                RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.MAIN_H,
                RotomPhoneBaseScreen.MAIN_W, RotomPhoneBaseScreen.MAIN_H);
    }
}
