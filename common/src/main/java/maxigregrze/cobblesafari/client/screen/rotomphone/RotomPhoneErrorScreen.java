package maxigregrze.cobblesafari.client.screen.rotomphone;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RotomPhoneErrorScreen extends RotomPhoneBaseScreen {

    private static final ResourceLocation ERROR_TEX = loc("rotomphone_gui_error.png");
    private static final int ERROR_W = 120;
    private static final int ERROR_H = 120;
    private static final int ERROR_X = 114;
    private static final int ERROR_Y = 32;
    private long openedAt;

    public RotomPhoneErrorScreen(String rotomName, boolean shinyStatus, String currentSkin, boolean safetyMode) {
        super(Component.translatable("gui.cobblesafari.rotomphone.error"), rotomName, shinyStatus, currentSkin, safetyMode);
    }

    @Override
    protected void init() {
        super.init();
        openedAt = System.currentTimeMillis();
    }

    @Override
    protected void renderPhoneContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long elapsed = System.currentTimeMillis() - openedAt;
        long cycleTicks = (elapsed / 50) % 20;
        boolean visible = cycleTicks < 10;

        if (visible) {
            int ex = originX + ERROR_X;
            int ey = originY + ERROR_Y;
            graphics.blit(ERROR_TEX, ex, ey, 0, 0, ERROR_W, ERROR_H, ERROR_W, ERROR_H);
        }
    }

    @Override
    protected void onBackButtonClicked() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new RotomPhoneMenuScreen(rotomName, shinyStatus, currentSkin, safetyMode));
        }
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
