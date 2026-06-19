package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.network.OpenCsBossMimicConfigPayload;
import maxigregrze.cobblesafari.network.SaveCsBossMimicConfigPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Creative "command-block-like" mimic GUI: one block-id field + a "reverse state"
 * toggle button.
 */
public class CsBossMimicConfigScreen extends Screen {

    private static final int LABEL_TO_FIELD_GAP = 4;
    private static final int AFTER_FIELD_GAP = 12;
    private static final int EDIT_HEIGHT = 20;
    private static final int CONTENT_TOP_Y = 48;
    private static final int ROW_HEIGHT = 20;

    private final OpenCsBossMimicConfigPayload initial;

    private EditBox blockIdBox;
    private boolean reverse;
    private int reverseRowY;
    private Button reverseButton;

    public CsBossMimicConfigScreen(OpenCsBossMimicConfigPayload initial) {
        super(Component.translatable("gui.cobblesafari.csboss.mimic.title"));
        this.initial = initial;
        this.reverse = initial.reverse();
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 154;

        this.blockIdBox = new EditBox(this.font, left, CONTENT_TOP_Y, 308, EDIT_HEIGHT, Component.empty());
        this.blockIdBox.setMaxLength(256);
        this.blockIdBox.setValue(this.initial.mimicBlockId());
        this.addRenderableWidget(this.blockIdBox);

        this.reverseRowY = CONTENT_TOP_Y + this.font.lineHeight + LABEL_TO_FIELD_GAP + EDIT_HEIGHT + AFTER_FIELD_GAP;
        this.reverseButton = Button.builder(reverseLabel(), b -> {
            this.reverse = !this.reverse;
            b.setMessage(reverseLabel());
        }).bounds(left, this.reverseRowY, 308, ROW_HEIGHT).build();
        this.addRenderableWidget(this.reverseButton);

        int duoY = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.csboss.save"), b -> {
            Services.PLATFORM.sendPayloadToServer(new SaveCsBossMimicConfigPayload(
                    this.initial.pos(),
                    this.blockIdBox.getValue(),
                    this.reverse
            ));
            this.onClose();
        }).bounds(left, duoY, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.csboss.cancel"),
                b -> this.onClose()).bounds(left + 158, duoY, 150, 20).build());

        this.setInitialFocus(this.blockIdBox);
    }

    private Component reverseLabel() {
        Component onOff = this.reverse ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
        return Component.translatable("gui.cobblesafari.csboss.mimic.reverse", onOff);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.blockIdBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.blockIdBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("gui.cobblesafari.csboss.mimic.block_id"),
                this.blockIdBox.getX(),
                this.blockIdBox.getY() - LABEL_TO_FIELD_GAP - this.font.lineHeight, 0xA0A0A0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
