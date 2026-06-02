package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.network.OpenCsBossTriggerConfigPayload;
import maxigregrze.cobblesafari.network.SaveCsBossTriggerConfigPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * GUI créative « command-block-like » du trigger d'arène (plan 100 § 3.3).
 */
public class CsBossTriggerConfigScreen extends Screen {

    private static final int LABEL_TO_FIELD_GAP = 4;
    private static final int AFTER_FIELD_GAP = 12;
    private static final int EDIT_HEIGHT = 20;
    private static final int CONTENT_TOP_Y = 48;

    private final OpenCsBossTriggerConfigPayload initial;

    private EditBox bossRefBox;
    private EditBox costItemBox;
    private EditBox playerRadiusBox;
    private EditBox blockRadiusBox;

    public CsBossTriggerConfigScreen(OpenCsBossTriggerConfigPayload initial) {
        super(Component.translatable("gui.cobblesafari.csboss.title"));
        this.initial = initial;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 154;

        this.bossRefBox = makeBox(left, 256, this.initial.bossRef());
        this.costItemBox = makeBox(left, 256, this.initial.costItemId());
        this.playerRadiusBox = makeBox(left, 150, Integer.toString(this.initial.playerRadius()));
        this.blockRadiusBox = makeBox(left, 150, Integer.toString(this.initial.blockRadius()));

        int duoY = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.csboss.save"), b -> {
            Services.PLATFORM.sendPayloadToServer(new SaveCsBossTriggerConfigPayload(
                    this.initial.pos(),
                    this.bossRefBox.getValue(),
                    this.costItemBox.getValue(),
                    this.playerRadiusBox.getValue(),
                    this.blockRadiusBox.getValue()
            ));
            this.onClose();
        }).bounds(left, duoY, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.csboss.cancel"),
                b -> this.onClose()).bounds(left + 158, duoY, 150, 20).build());

        relayout();
        this.setInitialFocus(this.bossRefBox);
    }

    private EditBox makeBox(int left, int width, String value) {
        EditBox box = new EditBox(this.font, left, 0, width, EDIT_HEIGHT, Component.empty());
        box.setMaxLength(256);
        box.setValue(value);
        this.addRenderableWidget(box);
        return box;
    }

    private int stride() {
        return this.font.lineHeight + LABEL_TO_FIELD_GAP + EDIT_HEIGHT + AFTER_FIELD_GAP;
    }

    private void relayout() {
        int left = this.width / 2 - 154;
        int y = CONTENT_TOP_Y;
        this.bossRefBox.setPosition(left, y);
        y += stride();
        this.costItemBox.setPosition(left, y);
        y += stride();
        this.playerRadiusBox.setPosition(left, y);
        this.blockRadiusBox.setPosition(left + 158, y);
    }

    private EditBox[] boxes() {
        return new EditBox[]{this.bossRefBox, this.costItemBox, this.playerRadiusBox, this.blockRadiusBox};
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox box : boxes()) {
            if (box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox box : boxes()) {
            if (box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void label(GuiGraphics g, EditBox box, String key) {
        g.drawString(this.font, Component.translatable(key), box.getX(),
                box.getY() - LABEL_TO_FIELD_GAP - this.font.lineHeight, 0xA0A0A0, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        label(g, this.bossRefBox, "gui.cobblesafari.csboss.boss_ref");
        label(g, this.costItemBox, "gui.cobblesafari.csboss.cost_item");
        label(g, this.playerRadiusBox, "gui.cobblesafari.csboss.player_radius");
        label(g, this.blockRadiusBox, "gui.cobblesafari.csboss.block_radius");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
