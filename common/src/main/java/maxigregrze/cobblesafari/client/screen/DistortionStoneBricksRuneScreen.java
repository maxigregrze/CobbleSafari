package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.network.SaveRuneTextPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class DistortionStoneBricksRuneScreen extends Screen {

    private final BlockPos blockPos;
    private final String initialText;
    private EditBox textBox;

    public DistortionStoneBricksRuneScreen(BlockPos blockPos, String initialText) {
        super(Component.translatable("gui.cobblesafari.distortion_rune.title"));
        this.blockPos = blockPos;
        this.initialText = initialText == null ? "" : initialText;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.textBox = new EditBox(this.font, centerX - 120, centerY - 28, 240, 20, Component.translatable("gui.cobblesafari.distortion_rune.input"));
        this.textBox.setMaxLength(32);
        this.textBox.setValue(this.initialText);
        this.addRenderableWidget(this.textBox);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.distortion_rune.save"), button -> {
            Services.PLATFORM.sendPayloadToServer(new SaveRuneTextPayload(this.blockPos, this.textBox.getValue()));
            this.onClose();
        }).bounds(centerX - 120, centerY + 4, 116, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.distortion_rune.cancel"), button -> this.onClose())
                .bounds(centerX + 4, centerY + 4, 116, 20)
                .build());

        this.setInitialFocus(this.textBox);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.textBox != null && this.textBox.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.textBox != null && this.textBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 48, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("gui.cobblesafari.distortion_rune.input"), this.width / 2 - 120, this.height / 2 - 40, 0xA0A0A0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
