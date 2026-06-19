package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.config.RotomPhoneConfig;
import maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RotomPhoneConfigSync {

    private RotomPhoneConfigSync() {}

    public static void syncToPlayer(ServerPlayer player) {
        List<RotomPhoneConfigSyncPayload.AppData> apps = new ArrayList<>();
        for (Map.Entry<String, RotomPhoneConfig.PhoneAppConfig> entry : RotomPhoneConfig.getPhoneApps().entrySet()) {
            RotomPhoneConfig.PhoneAppConfig cfg = entry.getValue();
            boolean unlocked = isAppUnlockedByPlayer(player, cfg);
            apps.add(new RotomPhoneConfigSyncPayload.AppData(
                    entry.getKey(), cfg.isEnabled(), cfg.isUnlockedByDefault(),
                    cfg.getUnlockingAdvancement(), cfg.getBannedDimensions(), unlocked));
        }

        List<RotomPhoneConfigSyncPayload.SkinData> skins = new ArrayList<>();
        for (RotomPhoneSkinDefinition skin : RotomPhoneSkinRegistry.getAllSkins()) {
            boolean unlocked = RotomPhoneSkinRegistry.isUnlockedByPlayer(player, skin);
            skins.add(new RotomPhoneConfigSyncPayload.SkinData(
                    skin.getId(), skin.getDisplayName(), skin.getColor(),
                    skin.hasCustomScreen(), skin.isUnlockedFromStart(),
                    skin.getUnlockingAdvancement(), skin.hasShinyVariant(), unlocked));
        }

        Services.PLATFORM.sendPayloadToPlayer(player,
                new RotomPhoneConfigSyncPayload(apps, skins));
    }

    /**
     * Whether {@code player} has unlocked an app. Mirrors
     * {@link RotomPhoneSkinRegistry#isUnlockedByPlayer}: unlocked if flagged as default, otherwise
     * gated by completion of the configured advancement.
     */
    public static boolean isAppUnlockedByPlayer(ServerPlayer player, RotomPhoneConfig.PhoneAppConfig cfg) {
        if (cfg == null) {
            return false;
        }
        if (cfg.isUnlockedByDefault()) {
            return true;
        }
        String advancementPath = cfg.getUnlockingAdvancement();
        if (advancementPath == null || advancementPath.isEmpty()) {
            return false;
        }
        ResourceLocation advId = ResourceLocation.tryParse(advancementPath);
        if (advId == null) {
            return false;
        }
        AdvancementHolder holder = player.server.getAdvancements().get(advId);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
