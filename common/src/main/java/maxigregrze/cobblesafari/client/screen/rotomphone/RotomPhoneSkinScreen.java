package maxigregrze.cobblesafari.client.screen.rotomphone;

import maxigregrze.cobblesafari.network.RotomPhoneActionPayload;
import maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneClientCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

public class RotomPhoneSkinScreen extends RotomPhoneBaseScreen {

    private static final ResourceLocation BTN_LEFT = loc("rotomphone_gui_buttonleft.png");
    private static final ResourceLocation BTN_RIGHT = loc("rotomphone_gui_buttonright.png");
    private static final int BTN_SIZE = 44;
    private static final int BTN_LEFT_X = 62;
    private static final int BTN_RIGHT_X = 242;
    private static final int BTN_Y = 70;
    private static final int LABEL_X = 174;
    private static final int LABEL_Y = 92;

    private final List<SkinEntry> skinList = new ArrayList<>();
    private int currentIndex = 0;

    public RotomPhoneSkinScreen(String rotomName, boolean shinyStatus, String currentSkin, boolean safetyMode) {
        super(Component.translatable("gui.cobblesafari.rotomphone.app.skin"), rotomName, shinyStatus, currentSkin, safetyMode);
    }

    @Override
    protected void init() {
        super.init();
        skinList.clear();
        skinList.add(new SkinEntry("", Component.translatable("gui.cobblesafari.rotomphone.skin.none")));

        for (RotomPhoneConfigSyncPayload.SkinData sd : RotomPhoneClientCache.getCachedSkins()) {
            if (sd.unlockedForPlayer()) {
                skinList.add(new SkinEntry(sd.id(), Component.literal(sd.displayName())));
            }
        }

        currentIndex = 0;
        String skinKey = currentSkin == null ? "" : currentSkin;
        for (int i = 0; i < skinList.size(); i++) {
            if (skinList.get(i).id.equalsIgnoreCase(skinKey)) {
                currentIndex = i;
                break;
            }
        }
    }

    @Override
    protected void renderPhoneContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        SkinEntry entry = skinList.get(currentIndex);
        graphics.drawCenteredString(this.font, entry.displayName,
                originX + LABEL_X, originY + LABEL_Y, 0xFFFFFFFF);

        int last = skinList.size() - 1;
        boolean canGoLeft = last > 0 && currentIndex > 0;
        boolean canGoRight = last > 0 && currentIndex < last;

        if (canGoLeft) {
            int lx = originX + BTN_LEFT_X;
            int ly = originY + BTN_Y;
            boolean hovered = isInBounds(mouseX, mouseY, lx, ly, BTN_SIZE, BTN_SIZE);
            int tint = hovered ? 0xFFFFFFFF : getTintColor();
            applyTint(graphics, tint);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(BTN_LEFT, lx, ly, 0, 0, BTN_SIZE, BTN_SIZE, BTN_SIZE, BTN_SIZE);
            graphics.setColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }

        if (canGoRight) {
            int rx = originX + BTN_RIGHT_X;
            int ry = originY + BTN_Y;
            boolean hovered = isInBounds(mouseX, mouseY, rx, ry, BTN_SIZE, BTN_SIZE);
            int tint = hovered ? 0xFFFFFFFF : getTintColor();
            applyTint(graphics, tint);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(BTN_RIGHT, rx, ry, 0, 0, BTN_SIZE, BTN_SIZE, BTN_SIZE, BTN_SIZE);
            graphics.setColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
    }

    private void applyTint(GuiGraphics graphics, int tint) {
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        graphics.setColor(r, g, b, 1.0f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int last = skinList.size() - 1;
            boolean canGoLeft = last > 0 && currentIndex > 0;
            boolean canGoRight = last > 0 && currentIndex < last;

            if (canGoLeft && isInBounds(mouseX, mouseY, originX + BTN_LEFT_X, originY + BTN_Y, BTN_SIZE, BTN_SIZE)) {
                currentIndex--;
                applySkin();
                return true;
            }

            if (canGoRight && isInBounds(mouseX, mouseY, originX + BTN_RIGHT_X, originY + BTN_Y, BTN_SIZE, BTN_SIZE)) {
                currentIndex++;
                applySkin();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void applySkin() {
        String skinId = skinList.get(currentIndex).id;
        setCurrentSkin(skinId);
        Services.PLATFORM.sendPayloadToServer(
                new RotomPhoneActionPayload(RotomPhoneActionPayload.ACTION_CHANGE_SKIN, skinId));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onBackButtonClicked();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private record SkinEntry(String id, Component displayName) {}
}
