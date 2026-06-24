package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.network.SaveRuneTextPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class DistortionStoneBricksRuneScreen extends CobbleSafariConfigScreen {

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
        int left = this.panelLeft();

        this.textBox = makeEditBox(left, this.contentTopY(), PANEL_WIDTH, 32, this.initialText);

        int duoY = this.bottomRowY();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.distortion_rune.save"), button -> {
            Services.PLATFORM.sendPayloadToServer(new SaveRuneTextPayload(this.blockPos, this.textBox.getValue()));
            this.onClose();
        }).bounds(left, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.distortion_rune.cancel"), button -> this.onClose())
                .bounds(left + COLUMN_OFFSET, duoY, COLUMN_WIDTH, BUTTON_HEIGHT)
                .build());

        this.focusScroll(this.textBox);
    }

    @Override
    protected EditBox[] editBoxes() {
        return new EditBox[]{this.textBox};
    }

    @Override
    protected void renderScrollContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        drawFieldLabel(guiGraphics, this.textBox, "gui.cobblesafari.distortion_rune.input");
    }
}
