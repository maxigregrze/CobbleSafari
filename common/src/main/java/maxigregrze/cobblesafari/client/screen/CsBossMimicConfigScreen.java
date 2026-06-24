package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.network.OpenCsBossMimicConfigPayload;
import maxigregrze.cobblesafari.network.SaveCsBossMimicConfigPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Creative "command-block-like" mimic GUI: one block-id field + a "reverse state"
 * toggle button.
 */
public class CsBossMimicConfigScreen extends CobbleSafariConfigScreen {

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
        int left = this.panelLeft();

        this.blockIdBox = makeEditBox(left, this.contentTopY(), PANEL_WIDTH, 256, this.initial.mimicBlockId());

        this.reverseRowY = this.contentTopY() + this.font.lineHeight + LABEL_TO_FIELD_GAP + EDIT_HEIGHT + AFTER_FIELD_GAP;
        this.reverseButton = addScroll(Button.builder(reverseLabel(), b -> {
            this.reverse = !this.reverse;
            b.setMessage(reverseLabel());
        }).bounds(left, this.reverseRowY, PANEL_WIDTH, BUTTON_HEIGHT).build());

        int duoY = this.bottomRowY();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.csboss.save"), b -> {
            Services.PLATFORM.sendPayloadToServer(new SaveCsBossMimicConfigPayload(
                    this.initial.pos(),
                    this.blockIdBox.getValue(),
                    this.reverse
            ));
            this.onClose();
        }).bounds(left, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.csboss.cancel"),
                b -> this.onClose()).bounds(left + COLUMN_OFFSET, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.focusScroll(this.blockIdBox);
    }

    private Component reverseLabel() {
        Component onOff = this.reverse ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
        return Component.translatable("gui.cobblesafari.csboss.mimic.reverse", onOff);
    }

    @Override
    protected EditBox[] editBoxes() {
        return new EditBox[]{this.blockIdBox};
    }

    @Override
    protected void renderScrollContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        drawFieldLabel(g, this.blockIdBox, "gui.cobblesafari.csboss.mimic.block_id");
    }
}
