package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.config.RotomPhoneConfig;
import maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload;
import maxigregrze.cobblesafari.platform.Services;
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
            apps.add(new RotomPhoneConfigSyncPayload.AppData(
                    entry.getKey(), cfg.isEnabled(), cfg.isUnlockedByDefault(),
                    cfg.getUnlockingAdvancement(), cfg.getBannedDimensions()));
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
}
