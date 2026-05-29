package maxigregrze.cobblesafari.client.screen.rotomphone;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.network.EmptyPhoneConfirmPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class EmptyPhoneConfirmScreen extends Screen {

    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 120;
    private static final int BTN_WIDTH = 80;
    private static final int BTN_HEIGHT = 20;

    private final String rotomName;
    private final int rotomLevel;
    private final boolean rotomIsShiny;

    private int guiLeft;
    private int guiTop;

    public EmptyPhoneConfirmScreen(String rotomName, int rotomLevel, boolean rotomIsShiny) {
        super(Component.translatable("gui.cobblesafari.rotomphone.empty_confirm.title"));
        this.rotomName = rotomName;
        this.rotomLevel = rotomLevel;
        this.rotomIsShiny = rotomIsShiny;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally blank: this confirm screen draws its own background in render().
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xCC000000);

        graphics.drawCenteredString(this.font, this.title,
                guiLeft + GUI_WIDTH / 2, guiTop + 8, 0xFFFFFF);

        Component desc = Component.translatable("gui.cobblesafari.rotomphone.empty_confirm.desc",
                rotomName, rotomLevel);
        graphics.drawCenteredString(this.font, desc,
                guiLeft + GUI_WIDTH / 2, guiTop + 30, 0xCCCCCC);

        if (rotomIsShiny) {
            Component shinyText = Component.translatable("gui.cobblesafari.rotomphone.empty_confirm.shiny");
            graphics.drawCenteredString(this.font, shinyText,
                    guiLeft + GUI_WIDTH / 2, guiTop + 45, 0xFFFFC0);
        }

        int confirmX = guiLeft + GUI_WIDTH / 2 - BTN_WIDTH - 5;
        int cancelX = guiLeft + GUI_WIDTH / 2 + 5;
        int btnY = guiTop + GUI_HEIGHT - BTN_HEIGHT - 10;

        boolean confirmHovered = isInBounds(mouseX, mouseY, confirmX, btnY, BTN_WIDTH, BTN_HEIGHT);
        boolean cancelHovered = isInBounds(mouseX, mouseY, cancelX, btnY, BTN_WIDTH, BTN_HEIGHT);

        graphics.fill(confirmX, btnY, confirmX + BTN_WIDTH, btnY + BTN_HEIGHT,
                confirmHovered ? 0x8055FF55 : 0x4055FF55);
        graphics.fill(cancelX, btnY, cancelX + BTN_WIDTH, btnY + BTN_HEIGHT,
                cancelHovered ? 0x80FF5555 : 0x40FF5555);

        Component confirmText = Component.translatable("gui.cobblesafari.rotomphone.confirm");
        Component cancelText = Component.translatable("gui.cobblesafari.rotomphone.cancel");
        graphics.drawCenteredString(this.font, confirmText,
                confirmX + BTN_WIDTH / 2, btnY + (BTN_HEIGHT - 8) / 2, 0xFFFFFF);
        graphics.drawCenteredString(this.font, cancelText,
                cancelX + BTN_WIDTH / 2, btnY + (BTN_HEIGHT - 8) / 2, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int confirmX = guiLeft + GUI_WIDTH / 2 - BTN_WIDTH - 5;
            int cancelX = guiLeft + GUI_WIDTH / 2 + 5;
            int btnY = guiTop + GUI_HEIGHT - BTN_HEIGHT - 10;

            if (isInBounds((int) mouseX, (int) mouseY, confirmX, btnY, BTN_WIDTH, BTN_HEIGHT)) {
                Services.PLATFORM.sendPayloadToServer(new EmptyPhoneConfirmPayload(true));
                this.onClose();
                return true;
            }
            if (isInBounds((int) mouseX, (int) mouseY, cancelX, btnY, BTN_WIDTH, BTN_HEIGHT)) {
                Services.PLATFORM.sendPayloadToServer(new EmptyPhoneConfirmPayload(false));
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean isInBounds(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
