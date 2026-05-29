package maxigregrze.cobblesafari.client.screen.rotomphone;

import maxigregrze.cobblesafari.network.RotomPhoneActionPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class RotomPhoneSettingsScreen extends RotomPhoneBaseScreen {

    private static final int LABEL_X = 174;
    private static final int TITLE_Y = 56;
    private static final int SAFETY_TOGGLE_Y = 78;
    private static final int ROTO_TOGGLE_Y = 100;

    public RotomPhoneSettingsScreen(String rotomName, boolean shinyStatus, String currentSkin, boolean safetyMode, boolean rotoGlide) {
        super(Component.translatable("gui.cobblesafari.rotomphone.app.settings"), rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide);
    }

    @Override
    protected void renderPhoneContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(this.font, Component.translatable("gui.cobblesafari.rotomphone.settings.title"),
                originX + LABEL_X, originY + TITLE_Y, 0xFF000000);

        Component safetyLabel = Component.translatable("gui.cobblesafari.rotomphone.settings.safety_toggle");
        int sx = originX + LABEL_X;
        int sy = originY + SAFETY_TOGGLE_Y;
        int sw = this.font.width(safetyLabel);
        boolean safetyHovered = isInBounds(mouseX, mouseY, sx - sw / 2, sy, sw, this.font.lineHeight);
        int safetyActiveColor = safetyMode ? 0xFF55FF55 : 0xFFFF5555;
        int safetyColor = safetyHovered ? 0xFFFFFFFF : safetyActiveColor;
        graphics.drawCenteredString(this.font, safetyLabel, sx, sy, safetyColor);

        Component rotoLabel = Component.translatable("gui.cobblesafari.rotomphone.settings.roto_glide_toggle");
        int rx = originX + LABEL_X;
        int ry = originY + ROTO_TOGGLE_Y;
        int rw = this.font.width(rotoLabel);
        boolean rotoHovered = isInBounds(mouseX, mouseY, rx - rw / 2, ry, rw, this.font.lineHeight);
        int rotoActiveColor = rotoGlide ? 0xFF55AAFF : 0xFFFF5555;
        int rotoColor = rotoHovered ? 0xFFFFFFFF : rotoActiveColor;
        graphics.drawCenteredString(this.font, rotoLabel, rx, ry, rotoColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Component safetyLabel = Component.translatable("gui.cobblesafari.rotomphone.settings.safety_toggle");
            int sx = originX + LABEL_X;
            int sy = originY + SAFETY_TOGGLE_Y;
            int sw = this.font.width(safetyLabel);
            if (isInBounds(mouseX, mouseY, sx - sw / 2, sy, sw, this.font.lineHeight)) {
                setSafetyMode(!safetyMode);
                Services.PLATFORM.sendPayloadToServer(
                        new RotomPhoneActionPayload(RotomPhoneActionPayload.ACTION_TOGGLE_SAFETY, ""));
                return true;
            }
            Component rotoLabel = Component.translatable("gui.cobblesafari.rotomphone.settings.roto_glide_toggle");
            int rx = originX + LABEL_X;
            int ry = originY + ROTO_TOGGLE_Y;
            int rw = this.font.width(rotoLabel);
            if (isInBounds(mouseX, mouseY, rx - rw / 2, ry, rw, this.font.lineHeight)) {
                setRotoGlide(!rotoGlide);
                Services.PLATFORM.sendPayloadToServer(
                        new RotomPhoneActionPayload(RotomPhoneActionPayload.ACTION_TOGGLE_ROTO_GLIDE, ""));
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
