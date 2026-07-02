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

    private static final String DIM_DUNGEON_UNDERGROUND = "cobblesafari:dungeon_underground";
    private static final String DIM_DUNGEON_DISTORTION = "cobblesafari:dungeon_distortion";
    private static final String APP_UNION = "unionApp";
    private static final String APP_WONDER = "wonderApp";
    private static final String APP_GTS = "gtsApp";

    @SerializedName("CONFIG_VERSION")
    private int configVersion = 2;
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

    /** The Item Finder app was removed; drop any leftover entry from an older config. */
    private void migrateRemoveItemFinder() {
        if (phoneApps != null) {
            phoneApps.remove("itemFinderApp");
        }
    }

    private static Map<String, PhoneAppConfig> createDefaultApps() {
        // Only the Chat app is visible by default; the others are unlocked through the "rotom" chat
        // questline (or a consumable disc / admin command). See chat_conversation/rotom.json.
        Map<String, PhoneAppConfig> apps = new LinkedHashMap<>();
        apps.put("chatApp", new PhoneAppConfig(true, new ArrayList<>()));
        apps.put(APP_GTS, new PhoneAppConfig(true, List.of(
                DIM_DUNGEON_UNDERGROUND, DIM_DUNGEON_DISTORTION)));
        apps.put(APP_WONDER, new PhoneAppConfig(true, List.of(
                DIM_DUNGEON_UNDERGROUND, DIM_DUNGEON_DISTORTION)));
        apps.put(APP_UNION, new PhoneAppConfig(true, List.of(
                DIM_DUNGEON_UNDERGROUND, DIM_DUNGEON_DISTORTION)));
        apps.put("skinApp", new PhoneAppConfig(true, new ArrayList<>()));
        apps.put("settingsApp", new PhoneAppConfig(true, new ArrayList<>()));
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
                INSTANCE.migrateRemoveItemFinder();
                INSTANCE.configVersion = 2;
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
        return apps.getOrDefault(appName, new PhoneAppConfig(false, new ArrayList<>()));
    }

    public static boolean isAllowsShinyPhone() {
        if (INSTANCE == null) return true;
        return INSTANCE.allowsShinyPhone;
    }

    public static int getConfigVersion() {
        if (INSTANCE == null) return 2;
        return INSTANCE.configVersion;
    }

    /**
     * Per-app config. {@code enabledByDefault} is the single visibility switch: {@code true} means the
     * app is shown to every player immediately; {@code false} means it stays hidden until the player
     * unlocks it (consumable item, chat questline step, or admin command).
     */
    public static class PhoneAppConfig {
        boolean enabledByDefault;
        List<String> bannedDimensions;

        public PhoneAppConfig() {
            this.enabledByDefault = true;
            this.bannedDimensions = new ArrayList<>();
        }

        public PhoneAppConfig(boolean enabledByDefault, List<String> bannedDimensions) {
            this.enabledByDefault = enabledByDefault;
            this.bannedDimensions = bannedDimensions;
        }

        public boolean isEnabledByDefault() { return enabledByDefault; }
        public List<String> getBannedDimensions() {
            return bannedDimensions != null ? bannedDimensions : new ArrayList<>();
        }
    }
}
