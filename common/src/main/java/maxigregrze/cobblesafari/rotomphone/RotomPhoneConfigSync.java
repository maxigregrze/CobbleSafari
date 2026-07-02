package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.config.RotomPhoneConfig;
import maxigregrze.cobblesafari.data.RotomPhoneUnlockSavedData;
import maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RotomPhoneConfigSync {

    private RotomPhoneConfigSync() {}

    public static void syncToPlayer(ServerPlayer player) {
        RotomPhoneUnlockSavedData store = RotomPhoneUnlockSavedData.get(player.server);

        List<RotomPhoneConfigSyncPayload.AppData> apps = new ArrayList<>();
        for (Map.Entry<String, RotomPhoneConfig.PhoneAppConfig> entry : RotomPhoneConfig.getPhoneApps().entrySet()) {
            RotomPhoneConfig.PhoneAppConfig cfg = entry.getValue();
            boolean unlocked = isAppUnlockedByPlayer(player, entry.getKey(), cfg, store);
            apps.add(new RotomPhoneConfigSyncPayload.AppData(
                    entry.getKey(), cfg.getBannedDimensions(), unlocked));
        }

        List<RotomPhoneConfigSyncPayload.SkinData> skins = new ArrayList<>();
        for (RotomPhoneSkinDefinition skin : RotomPhoneSkinRegistry.getAllSkins()) {
            boolean unlocked = RotomPhoneSkinRegistry.isUnlockedByPlayer(player, skin);
            skins.add(new RotomPhoneConfigSyncPayload.SkinData(
                    skin.getId(), skin.getDisplayName(), skin.getColor(),
                    skin.hasCustomScreen(), skin.isUnlockedFromStart(),
                    skin.hasShinyVariant(), unlocked));
        }

        Services.PLATFORM.sendPayloadToPlayer(player,
                new RotomPhoneConfigSyncPayload(apps, skins));
    }

    /**
     * Whether {@code player} can see an app: visible by default ({@code enabledByDefault}) or unlocked
     * for this player through a consumable item / chat questline step / admin command (persisted in
     * {@link RotomPhoneUnlockSavedData}).
     */
    public static boolean isAppUnlockedByPlayer(ServerPlayer player, String appId,
                                                RotomPhoneConfig.PhoneAppConfig cfg,
                                                RotomPhoneUnlockSavedData store) {
        if (cfg == null) {
            return false;
        }
        if (cfg.isEnabledByDefault()) {
            return true;
        }
        return store != null && store.isAppUnlocked(player.getUUID(), appId);
    }
}
