package maxigregrze.cobblesafari.client.screen.rotomphone;

import maxigregrze.cobblesafari.network.RotomPhoneActionPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class RotomPhoneSettingsScreen extends RotomPhoneBaseScreen {

    private static final int LABEL_X = 174;
    private static final int LABEL_Y = 92;
    private static final int TOGGLE_X = 174;
    private static final int TOGGLE_Y = 125;

    public RotomPhoneSettingsScreen(String rotomName, boolean shinyStatus, String currentSkin, boolean safetyMode) {
        super(Component.translatable("gui.cobblesafari.rotomphone.app.settings"), rotomName, shinyStatus, currentSkin, safetyMode);
    }

    @Override
    protected void renderPhoneContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(this.font, Component.literal("Settings App"),
                originX + LABEL_X, originY + LABEL_Y, 0xFF000000);

        Component toggleText = Component.literal("toggle");
        int tx = originX + TOGGLE_X;
        int ty = originY + TOGGLE_Y;
        int tw = this.font.width(toggleText);
        boolean hovered = isInBounds(mouseX, mouseY, tx - tw / 2, ty, tw, this.font.lineHeight);
        int color = hovered ? 0xFFFFFFFF : (safetyMode ? 0xFF55FF55 : 0xFFFF5555);
        graphics.drawCenteredString(this.font, toggleText, tx, ty, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Component toggleText = Component.literal("toggle");
            int tx = originX + TOGGLE_X;
            int ty = originY + TOGGLE_Y;
            int tw = this.font.width(toggleText);
            if (isInBounds(mouseX, mouseY, tx - tw / 2, ty, tw, this.font.lineHeight)) {
                setSafetyMode(!safetyMode);
                Services.PLATFORM.sendPayloadToServer(
                        new RotomPhoneActionPayload(RotomPhoneActionPayload.ACTION_TOGGLE_SAFETY, ""));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
