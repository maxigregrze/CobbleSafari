package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload;

import java.util.ArrayList;
import java.util.List;

public class RotomPhoneClientCache {

    private static List<RotomPhoneConfigSyncPayload.AppData> cachedApps = new ArrayList<>();
    private static List<RotomPhoneConfigSyncPayload.SkinData> cachedSkins = new ArrayList<>();

    private RotomPhoneClientCache() {}

    public static void applySyncData(RotomPhoneConfigSyncPayload payload) {
        cachedApps = new ArrayList<>(payload.apps());
        cachedSkins = new ArrayList<>(payload.skins());
    }

    public static List<RotomPhoneConfigSyncPayload.AppData> getCachedApps() {
        return cachedApps;
    }

    public static List<RotomPhoneConfigSyncPayload.SkinData> getCachedSkins() {
        return cachedSkins;
    }
}
