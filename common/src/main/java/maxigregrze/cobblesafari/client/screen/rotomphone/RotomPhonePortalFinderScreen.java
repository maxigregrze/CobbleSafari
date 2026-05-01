package maxigregrze.cobblesafari.client.screen.rotomphone;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class RotomPhonePortalFinderScreen extends RotomPhoneBaseScreen {

    public RotomPhonePortalFinderScreen(String rotomName, boolean shinyStatus, String currentSkin, boolean safetyMode, boolean rotoGlide) {
        super(Component.translatable("gui.cobblesafari.rotomphone.app.portal_finder"), rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide);
    }

    @Override
    protected void renderPhoneContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(this.font, Component.literal("Portal Finder App"),
                originX + 174, originY + 92, 0xFF000000);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onBackButtonClicked();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
