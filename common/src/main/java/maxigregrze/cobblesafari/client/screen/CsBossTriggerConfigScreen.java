package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.block.csboss.AnchorVariant;
import maxigregrze.cobblesafari.network.OpenCsBossTriggerConfigPayload;
import maxigregrze.cobblesafari.network.SaveCsBossTriggerConfigPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Creative "command-block-like" arena trigger GUI.
 */
public class CsBossTriggerConfigScreen extends CobbleSafariConfigScreen {

    private final OpenCsBossTriggerConfigPayload initial;

    private EditBox bossRefBox;
    private EditBox costItemBox;
    private EditBox playerRadiusBox;
    private EditBox blockRadiusBox;
    private AnchorVariant variant;
    private int variantRowY;
    private Button variantPrevButton;
    private Button variantNextButton;

    public CsBossTriggerConfigScreen(OpenCsBossTriggerConfigPayload initial) {
        super(Component.translatable("gui.cobblesafari.csboss.title"));
        this.initial = initial;
        this.variant = AnchorVariant.byName(initial.variant());
    }

    @Override
    protected void init() {
        int left = this.panelLeft();

        this.bossRefBox = makeEditBox(left, 0, PANEL_WIDTH, 256, this.initial.bossRef());
        this.costItemBox = makeEditBox(left, 0, PANEL_WIDTH, 256, this.initial.costItemId());
        this.playerRadiusBox = makeEditBox(left, 0, COLUMN_WIDTH, 256, Integer.toString(this.initial.playerRadius()));
        this.blockRadiusBox = makeEditBox(left, 0, COLUMN_WIDTH, 256, Integer.toString(this.initial.blockRadius()));

        this.variantPrevButton = addScroll(Button.builder(Component.literal("< "), b -> this.variant = this.variant.prev())
                .bounds(left, 0, 20, BUTTON_HEIGHT).build());
        this.variantNextButton = addScroll(Button.builder(Component.literal(" >"), b -> this.variant = this.variant.next())
                .bounds(left + PANEL_WIDTH - 20, 0, 20, BUTTON_HEIGHT).build());

        int duoY = this.bottomRowY();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.csboss.save"), b -> {
            Services.PLATFORM.sendPayloadToServer(new SaveCsBossTriggerConfigPayload(
                    this.initial.pos(),
                    this.bossRefBox.getValue(),
                    this.costItemBox.getValue(),
                    this.playerRadiusBox.getValue(),
                    this.blockRadiusBox.getValue(),
                    this.variant.getSerializedName()
            ));
            this.onClose();
        }).bounds(left, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.csboss.cancel"),
                b -> this.onClose()).bounds(left + COLUMN_OFFSET, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        relayout();
        this.focusScroll(this.bossRefBox);
    }

    private void relayout() {
        int left = this.panelLeft();
        int y = this.contentTopY();
        int stride = this.fieldStride();
        this.bossRefBox.setPosition(left, y);
        y += stride;
        this.costItemBox.setPosition(left, y);
        y += stride;
        this.playerRadiusBox.setPosition(left, y);
        this.blockRadiusBox.setPosition(left + COLUMN_OFFSET, y);
        y += stride;
        this.variantRowY = y;
        this.variantPrevButton.setPosition(left, y);
        this.variantNextButton.setPosition(left + PANEL_WIDTH - 20, y);
    }

    @Override
    protected EditBox[] editBoxes() {
        return new EditBox[]{this.bossRefBox, this.costItemBox, this.playerRadiusBox, this.blockRadiusBox};
    }

    private void label(GuiGraphics g, EditBox box, String key) {
        g.drawString(this.font, Component.translatable(key), box.getX(), labelBaselineY(box), LABEL_COLOR, false);
    }

    @Override
    protected void renderScrollContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        label(g, this.bossRefBox, "gui.cobblesafari.csboss.boss_ref");
        label(g, this.costItemBox, "gui.cobblesafari.csboss.cost_item");
        label(g, this.playerRadiusBox, "gui.cobblesafari.csboss.player_radius");
        label(g, this.blockRadiusBox, "gui.cobblesafari.csboss.block_radius");
        int left = this.panelLeft();
        g.drawString(this.font, Component.translatable("gui.cobblesafari.csboss.variant"), left,
                this.variantRowY - LABEL_TO_FIELD_GAP - this.font.lineHeight, LABEL_COLOR, false);
        g.drawCenteredString(this.font, Component.translatable("gui.cobblesafari.csboss.variant." + this.variant.getSerializedName()),
                left + PANEL_HALF_WIDTH, this.variantRowY + (BUTTON_HEIGHT - this.font.lineHeight) / 2, TITLE_COLOR);
    }
}
