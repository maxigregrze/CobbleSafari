package maxigregrze.cobblesafari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RotomPhoneConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir()
            .resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("rotomphone_config.json");

    private static RotomPhoneConfig INSTANCE;

    private int CONFIG_VERSION = 1;
    private Map<String, PhoneAppConfig> phoneApps = createDefaultApps();
    private boolean allowsShinyPhone = true;

    public RotomPhoneConfig() {
    }

    private void migrateHealToUnion() {
        if (phoneApps == null) {
            return;
        }
        PhoneAppConfig heal = phoneApps.remove("healApp");
        if (heal != null && !phoneApps.containsKey("unionApp")) {
            phoneApps.put("unionApp", heal);
        }
    }

    private void migratePortalToWonder() {
        if (phoneApps == null) {
            return;
        }
        PhoneAppConfig portal = phoneApps.remove("portalFinderApp");
        if (portal != null && !phoneApps.containsKey("wonderApp")) {
            phoneApps.put("wonderApp", portal);
        }
    }

    private void migratePcToGts() {
        if (phoneApps == null) {
            return;
        }
        PhoneAppConfig pc = phoneApps.remove("pcApp");
        if (pc != null && !phoneApps.containsKey("gtsApp")) {
            phoneApps.put("gtsApp", pc);
        }
    }

    private static Map<String, PhoneAppConfig> createDefaultApps() {
        Map<String, PhoneAppConfig> apps = new LinkedHashMap<>();
        apps.put("chatApp", new PhoneAppConfig(true, true, "story/root", new ArrayList<>()));
        apps.put("gtsApp", new PhoneAppConfig(true, false, "story/root", List.of(
                "cobblesafari:dungeon_underground", "cobblesafari:dungeon_distortion")));
        apps.put("unionApp", new PhoneAppConfig(true, false, "story/root", List.of(
                "cobblesafari:dungeon_underground", "cobblesafari:dungeon_distortion")));
        apps.put("wonderApp", new PhoneAppConfig(true, false, "story/root", List.of(
                "cobblesafari:dungeon_underground", "cobblesafari:dungeon_distortion")));
        apps.put("itemFinderApp", new PhoneAppConfig(true, false, "story/root", List.of(
                "cobblesafari:dungeon_underground", "cobblesafari:dungeon_distortion")));
        apps.put("skinApp", new PhoneAppConfig(true, false, "story/root", new ArrayList<>()));
        apps.put("settingsApp", new PhoneAppConfig(true, false, "story/root", new ArrayList<>()));
        return apps;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(reader, RotomPhoneConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new RotomPhoneConfig();
                }
                INSTANCE.migrateHealToUnion();
                INSTANCE.migratePortalToWonder();
                INSTANCE.migratePcToGts();
                CobbleSafari.LOGGER.info("Rotom phone config loaded from {}", CONFIG_PATH);
                save();
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("Failed to load rotom phone config, using defaults", e);
                INSTANCE = new RotomPhoneConfig();
            }
        } else {
            INSTANCE = new RotomPhoneConfig();
            save();
        }
    }

    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new RotomPhoneConfig();
        }
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to create config directory", e);
        }
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to save rotom phone config", e);
        }
    }

    public static Map<String, PhoneAppConfig> getPhoneApps() {
        if (INSTANCE == null) return createDefaultApps();
        return INSTANCE.phoneApps != null ? INSTANCE.phoneApps : createDefaultApps();
    }

    public static PhoneAppConfig getAppConfig(String appName) {
        Map<String, PhoneAppConfig> apps = getPhoneApps();
        return apps.getOrDefault(appName, new PhoneAppConfig(false, false, "", new ArrayList<>()));
    }

    public static boolean isAllowsShinyPhone() {
        if (INSTANCE == null) return true;
        return INSTANCE.allowsShinyPhone;
    }

    public static int getConfigVersion() {
        if (INSTANCE == null) return 1;
        return INSTANCE.CONFIG_VERSION;
    }

    public static class PhoneAppConfig {
        boolean enabled;
        boolean unlockedByDefault;
        String unlockingAdvancement;
        List<String> bannedDimensions;

        public PhoneAppConfig() {
            this.enabled = true;
            this.unlockedByDefault = false;
            this.unlockingAdvancement = "story/root";
            this.bannedDimensions = new ArrayList<>();
        }

        public PhoneAppConfig(boolean enabled, boolean unlockedByDefault,
                              String unlockingAdvancement, List<String> bannedDimensions) {
            this.enabled = enabled;
            this.unlockedByDefault = unlockedByDefault;
            this.unlockingAdvancement = unlockingAdvancement;
            this.bannedDimensions = bannedDimensions;
        }

        public boolean isEnabled() { return enabled; }
        public boolean isUnlockedByDefault() { return unlockedByDefault; }
        public String getUnlockingAdvancement() { return unlockingAdvancement; }
        public List<String> getBannedDimensions() {
            return bannedDimensions != null ? bannedDimensions : new ArrayList<>();
        }
    }
}
