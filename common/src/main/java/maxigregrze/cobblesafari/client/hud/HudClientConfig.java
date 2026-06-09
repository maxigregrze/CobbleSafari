package maxigregrze.cobblesafari.client.hud;

import maxigregrze.cobblesafari.client.objectives.ObjectivesHudController;
import maxigregrze.cobblesafari.config.HudConfig;

/** Client-side entry point for reloading {@code hud_config.json}. */
public final class HudClientConfig {

    private HudClientConfig() {}

    public static void reload() {
        HudConfig.load();
        ObjectivesHudController.applyHudConfigDefaults();
    }
}
