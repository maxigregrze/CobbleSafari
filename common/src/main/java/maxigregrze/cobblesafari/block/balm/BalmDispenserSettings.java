package maxigregrze.cobblesafari.block.balm;

import maxigregrze.cobblesafari.config.CsBossSettings;

public final class BalmDispenserSettings {

    private BalmDispenserSettings() {}

    public static int getRechargeSeconds() {
        return CsBossSettings.get().getBalmDispenserRechargeSeconds();
    }
}
