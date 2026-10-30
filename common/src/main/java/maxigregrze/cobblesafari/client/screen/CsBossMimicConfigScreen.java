package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlock;
import maxigregrze.cobblesafari.network.OpenCsBossMimicConfigPayload;
import maxigregrze.cobblesafari.network.SaveCsBossMimicConfigPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Creative "command-block-like" mimic GUI: the block-id field, a mode button cycling
 * hide in battle / show in battle / swap, and - in swap mode only - the block id copied during a fight.
 */
public class CsBossMimicConfigScreen extends CobbleSafariConfigScreen {

    private final OpenCsBossMimicConfigPayload initial;

    private EditBox blockIdBox;
    private EditBox battleBlockIdBox;
    private CsBossMimicBlock.Mode mode;

    public CsBossMimicConfigScreen(OpenCsBossMimicConfigPayload initial) {
        super(Component.translatable("gui.cobblesafari.csboss.mimic.title"));
        this.initial = initial;
        this.mode = CsBossMimicBlock.Mode.byIndex(initial.mode());
    }

    @Override
    protected void init() {
        int left = this.panelLeft();
        int labelSpace = this.font.lineHeight + LABEL_TO_FIELD_GAP;

        this.blockIdBox = makeEditBox(left, this.contentTopY(), PANEL_WIDTH, 256, this.initial.mimicBlockId());

        int modeRowY = this.contentTopY() + labelSpace + EDIT_HEIGHT + AFTER_FIELD_GAP;
        addScroll(Button.builder(modeLabel(), b -> {
            this.mode = CsBossMimicBlock.Mode.byIndex(this.mode.ordinal() + 1);
            b.setMessage(modeLabel());
            this.battleBlockIdBox.setVisible(this.mode == CsBossMimicBlock.Mode.SWAP);
        }).bounds(left, modeRowY, PANEL_WIDTH, BUTTON_HEIGHT).build());

        int battleRowY = modeRowY + BUTTON_HEIGHT + AFTER_FIELD_GAP + labelSpace;
        this.battleBlockIdBox = makeEditBox(left, battleRowY, PANEL_WIDTH, 256, this.initial.battleMimicBlockId());
        this.battleBlockIdBox.setVisible(this.mode == CsBossMimicBlock.Mode.SWAP);

        int duoY = this.bottomRowY();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.csboss.save"), b -> {
            Services.PLATFORM.sendPayloadToServer(new SaveCsBossMimicConfigPayload(
                    this.initial.pos(),
                    this.blockIdBox.getValue(),
                    this.battleBlockIdBox.getValue(),
                    this.mode.ordinal()
            ));
            this.onClose();
        }).bounds(left, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.csboss.cancel"),
                b -> this.onClose()).bounds(left + COLUMN_OFFSET, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.focusScroll(this.blockIdBox);
    }

    private Component modeLabel() {
        return Component.translatable("gui.cobblesafari.csboss.mimic.mode",
                Component.translatable("gui.cobblesafari.csboss.mimic.mode." + this.mode.name().toLowerCase(Locale.ROOT)));
    }

    @Override
    protected EditBox[] editBoxes() {
        return new EditBox[]{this.blockIdBox, this.battleBlockIdBox};
    }

    @Override
    protected void renderScrollContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        drawFieldLabel(g, this.blockIdBox, "gui.cobblesafari.csboss.mimic.block_id");
        drawFieldLabel(g, this.battleBlockIdBox, "gui.cobblesafari.csboss.mimic.battle_block_id");
    }
}
