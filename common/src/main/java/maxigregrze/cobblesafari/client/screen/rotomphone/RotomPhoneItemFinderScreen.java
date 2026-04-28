package maxigregrze.cobblesafari.client.screen.rotomphone;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class RotomPhoneItemFinderScreen extends RotomPhoneBaseScreen {

    public RotomPhoneItemFinderScreen(String rotomName, boolean shinyStatus, String currentSkin, boolean safetyMode) {
        super(Component.translatable("gui.cobblesafari.rotomphone.app.item_finder"), rotomName, shinyStatus, currentSkin, safetyMode);
    }

    @Override
    protected void renderPhoneContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(this.font, Component.literal("Item Finder App"),
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
