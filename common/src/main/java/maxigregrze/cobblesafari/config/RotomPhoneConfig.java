package maxigregrze.cobblesafari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
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

    private static final String ADV_STORY_ROOT = "story/root";
    private static final String DIM_DUNGEON_UNDERGROUND = "cobblesafari:dungeon_underground";
    private static final String DIM_DUNGEON_DISTORTION = "cobblesafari:dungeon_distortion";
    private static final String APP_UNION = "unionApp";
    private static final String APP_WONDER = "wonderApp";
    private static final String APP_GTS = "gtsApp";

    @SerializedName("CONFIG_VERSION")
    private int configVersion = 1;
    private Map<String, PhoneAppConfig> phoneApps = createDefaultApps();
    private boolean allowsShinyPhone = true;

    public RotomPhoneConfig() {
        // Required no-arg constructor for GSON deserialization.
    }

    private void migrateHealToUnion() {
        if (phoneApps == null) {
            return;
        }
        PhoneAppConfig heal = phoneApps.remove("healApp");
        if (heal != null && !phoneApps.containsKey(APP_UNION)) {
            phoneApps.put(APP_UNION, heal);
        }
    }

    private void migratePortalToWonder() {
        if (phoneApps == null) {
            return;
        }
        PhoneAppConfig portal = phoneApps.remove("portalFinderApp");
        if (portal != null && !phoneApps.containsKey(APP_WONDER)) {
            phoneApps.put(APP_WONDER, portal);
        }
    }

    private void migratePcToGts() {
        if (phoneApps == null) {
            return;
        }
        PhoneAppConfig pc = phoneApps.remove("pcApp");
        if (pc != null && !phoneApps.containsKey(APP_GTS)) {
            phoneApps.put(APP_GTS, pc);
        }
    }

    private static Map<String, PhoneAppConfig> createDefaultApps() {
        Map<String, PhoneAppConfig> apps = new LinkedHashMap<>();
        apps.put("chatApp", new PhoneAppConfig(true, true, ADV_STORY_ROOT, new ArrayList<>()));
        apps.put(APP_GTS, new PhoneAppConfig(true, true, ADV_STORY_ROOT, List.of(
                DIM_DUNGEON_UNDERGROUND, DIM_DUNGEON_DISTORTION)));
        apps.put(APP_UNION, new PhoneAppConfig(true, true, ADV_STORY_ROOT, List.of(
                DIM_DUNGEON_UNDERGROUND, DIM_DUNGEON_DISTORTION)));
        apps.put(APP_WONDER, new PhoneAppConfig(true, true, ADV_STORY_ROOT, List.of(
                DIM_DUNGEON_UNDERGROUND, DIM_DUNGEON_DISTORTION)));
        apps.put("itemFinderApp", new PhoneAppConfig(false, false, ADV_STORY_ROOT, List.of(
                DIM_DUNGEON_UNDERGROUND, DIM_DUNGEON_DISTORTION)));
        apps.put("skinApp", new PhoneAppConfig(true, true, ADV_STORY_ROOT, new ArrayList<>()));
        apps.put("settingsApp", new PhoneAppConfig(true, true, ADV_STORY_ROOT, new ArrayList<>()));
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
        return INSTANCE.configVersion;
    }

    public static class PhoneAppConfig {
        boolean enabled;
        boolean unlockedByDefault;
        String unlockingAdvancement;
        List<String> bannedDimensions;

        public PhoneAppConfig() {
            this.enabled = true;
            this.unlockedByDefault = false;
            this.unlockingAdvancement = ADV_STORY_ROOT;
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
