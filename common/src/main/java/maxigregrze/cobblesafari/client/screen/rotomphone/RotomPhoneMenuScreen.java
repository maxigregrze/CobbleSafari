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

public class RotomPhoneMenuScreen extends RotomPhoneBaseScreen {

    private static final int ICON_SIZE = 44;

    private static final int[][] SLOT_POSITIONS = {
            {62, 40}, {122, 40}, {182, 40}, {242, 40},
            {62, 100}, {122, 100}, {182, 100}, {242, 100}
    };

    private static final String[] APP_IDS = {
            "chatApp", "gtsApp", "wonderApp", "unionApp",
            "skinApp", "settingsApp"
    };

    private final List<String> visibleApps = new ArrayList<>();

    public RotomPhoneMenuScreen(String rotomName, boolean shinyStatus, String currentSkin, boolean safetyMode, boolean rotoGlide) {
        super(Component.translatable("gui.cobblesafari.rotomphone.menu"), rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide);
    }

    @Override
    protected void init() {
        super.init();
        visibleApps.clear();

        List<RotomPhoneConfigSyncPayload.AppData> cachedApps = RotomPhoneClientCache.getCachedApps();
        for (String appId : APP_IDS) {
            RotomPhoneConfigSyncPayload.AppData appData = null;
            for (RotomPhoneConfigSyncPayload.AppData a : cachedApps) {
                if (a.name().equals(appId)) { appData = a; break; }
            }
            // An app is visible when it is enabled by default or has been unlocked for this player
            // (consumable item / chat questline step / admin command). Computed server-side.
            if (appData == null || !appData.unlockedForPlayer()) continue;
            visibleApps.add(appId);
        }
    }

    @Override
    protected boolean showBackButton() {
        return false;
    }

    @Override
    protected void renderPhoneContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (int i = 0; i < visibleApps.size() && i < SLOT_POSITIONS.length; i++) {
            String appId = visibleApps.get(i);
            int ix = originX + SLOT_POSITIONS[i][0];
            int iy = originY + SLOT_POSITIONS[i][1];

            ResourceLocation iconTex = loc("rotomphone_gui_appicon_" + appId.toLowerCase() + ".png");
            boolean hovered = isInBounds(mouseX, mouseY, ix, iy, ICON_SIZE, ICON_SIZE);

            int tint = hovered ? 0xFFFFFFFF : getTintColor();
            float r = ((tint >> 16) & 0xFF) / 255f;
            float g = ((tint >> 8) & 0xFF) / 255f;
            float b = (tint & 0xFF) / 255f;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.setColor(r, g, b, 1.0f);
            graphics.blit(iconTex, ix, iy, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < visibleApps.size() && i < SLOT_POSITIONS.length; i++) {
                int ix = originX + SLOT_POSITIONS[i][0];
                int iy = originY + SLOT_POSITIONS[i][1];

                if (isInBounds((int) mouseX, (int) mouseY, ix, iy, ICON_SIZE, ICON_SIZE)) {
                    openApp(visibleApps.get(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openApp(String appId) {
        if (this.minecraft == null) return;

        RotomPhoneConfigSyncPayload.AppData appData = null;
        for (RotomPhoneConfigSyncPayload.AppData a : RotomPhoneClientCache.getCachedApps()) {
            if (a.name().equals(appId)) { appData = a; break; }
        }
        // Defense in depth: a locked app should never be opened even if its slot was clicked
        // (the menu already hides locked apps, but guard the action path regardless).
        if (appData != null && !appData.unlockedForPlayer()) {
            return;
        }
        if (appData != null && this.minecraft.player != null) {
            String dimKey = this.minecraft.player.level().dimension().location().toString();
            if (appData.bannedDimensions().contains(dimKey)) {
                this.minecraft.setScreen(new RotomPhoneErrorScreen(rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide));
                return;
            }
        }

        switch (appId) {
            case "chatApp" -> this.minecraft.setScreen(new RotomPhoneChatScreen(rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide));
            case "gtsApp" -> this.minecraft.setScreen(
                    new RotomPhoneGTSScreen(rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide));
            case "unionApp" -> this.minecraft.setScreen(new RotomPhoneUnionScreen(rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide));
            case "wonderApp" -> this.minecraft.setScreen(new RotomPhoneWonderScreen(rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide));
            case "skinApp" -> this.minecraft.setScreen(new RotomPhoneSkinScreen(rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide));
            case "settingsApp" -> this.minecraft.setScreen(new RotomPhoneSettingsScreen(rotomName, shinyStatus, currentSkin, safetyMode, rotoGlide));
            default -> { /* unknown app id: do nothing */ }
        }
    }

    @Override
    public void onClose() {
        Services.PLATFORM.sendPayloadToServer(new RotomPhoneActionPayload(RotomPhoneActionPayload.ACTION_CLOSE, ""));
        super.onClose();
    }
}
