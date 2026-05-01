package maxigregrze.cobblesafari.client.screen.rotomphone;

import com.mojang.blaze3d.systems.RenderSystem;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneClientCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class RotomPhoneBaseScreen extends Screen {

    private static final String TEX_PATH = "textures/gui/rotomphone/";

    public static final int MAIN_W = 348;
    public static final int MAIN_H = 184;
    public static final int BOTTOM_H = 64;
    public static final int TOP_H = 132;
    public static final int SKIN_H = 380;

    protected static final int BACK_BTN_W = 12;
    protected static final int BACK_BTN_H = 62;
    protected static final int BACK_BTN_X = 321;
    protected static final int BACK_BTN_Y = 61;

    protected final String rotomName;
    protected boolean shinyStatus;
    protected String currentSkin;
    protected boolean safetyMode;
    protected boolean rotoGlide;

    protected int originX;
    protected int originY;

    protected RotomPhoneBaseScreen(Component title, String rotomName, boolean shinyStatus,
                                    String currentSkin, boolean safetyMode, boolean rotoGlide) {
        super(title);
        this.rotomName = rotomName;
        this.shinyStatus = shinyStatus;
        this.currentSkin = currentSkin;
        this.safetyMode = safetyMode;
        this.rotoGlide = rotoGlide;
    }

    public void setCurrentSkin(String skin) {
        this.currentSkin = skin;
    }

    public void setSafetyMode(boolean mode) {
        this.safetyMode = mode;
    }

    public void setRotoGlide(boolean enabled) {
        this.rotoGlide = enabled;
    }

    @Override
    protected void init() {
        super.init();
        this.originX = (this.width - MAIN_W) / 2;
        this.originY = (this.height - MAIN_H) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RotomPhoneBackdropRenderer.renderFullPhone(graphics, width, height, shinyStatus, currentSkin);

        super.render(graphics, mouseX, mouseY, partialTick);

        renderPhoneContent(graphics, mouseX, mouseY, partialTick);
        renderBackButton(graphics, mouseX, mouseY);
    }

    private void renderBackButton(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!showBackButton()) return;

        int bx = originX + BACK_BTN_X;
        int by = originY + BACK_BTN_Y;
        boolean hovered = isInBounds(mouseX, mouseY, bx, by, BACK_BTN_W, BACK_BTN_H);

        ResourceLocation tex = loc("rotomphone_gui_backbutton.png");

        int tint = hovered ? 0xFFFFFFFF : getTintColor();
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(r, g, b, 1.0f);
        graphics.blit(tex, bx, by, 0, 0, BACK_BTN_W, BACK_BTN_H, BACK_BTN_W, BACK_BTN_H);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    protected boolean showBackButton() {
        return true;
    }

    protected int getTintColor() {
        RotomPhoneConfigSyncPayload.SkinData skinData = getCurrentSkinData();
        if (skinData != null && skinData.hasCustomScreen()) {
            try {
                return 0xFF000000 | Integer.parseInt(skinData.color(), 16);
            } catch (NumberFormatException ignored) {}
        }
        return shinyStatus ? 0xFFffffc0 : 0xFF7affff;
    }

    protected RotomPhoneConfigSyncPayload.SkinData getCurrentSkinData() {
        if (currentSkin == null || currentSkin.isEmpty()) return null;
        for (RotomPhoneConfigSyncPayload.SkinData s : RotomPhoneClientCache.getCachedSkins()) {
            if (s.id().equalsIgnoreCase(currentSkin)) return s;
        }
        return null;
    }

    protected abstract void renderPhoneContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && showBackButton()) {
            int bx = originX + BACK_BTN_X;
            int by = originY + BACK_BTN_Y;
            if (isInBounds((int) mouseX, (int) mouseY, bx, by, BACK_BTN_W, BACK_BTN_H)) {
                onBackButtonClicked();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected void onBackButtonClicked() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new RotomPhoneMenuScreen(rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide));
        }
    }

    protected static ResourceLocation loc(String filename) {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, TEX_PATH + filename);
    }

    protected static boolean isInBounds(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    protected static boolean isInBounds(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
