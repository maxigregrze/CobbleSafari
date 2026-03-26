package maxigregrze.cobblesafari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.platform.Services;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SafariTimerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir()
            .resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("dimensional_timer_config.json");

    private static final String SAFARI_DIMENSION_ID = "cobblesafari:domedimension";
    private static final String DUNGEON_JUMP_DIMENSION_ID = "cobblesafari:dungeon_jump";
    private static final String DUNGEON_UNDERGROUND_DIMENSION_ID = "cobblesafari:dungeon_underground";

    private static SafariTimerConfig INSTANCE;

    private List<DimensionTimerEntry> dimensions = new ArrayList<>();

    public SafariTimerConfig() {
        dimensions.add(new DimensionTimerEntry(SAFARI_DIMENSION_ID, 900, 0));
        dimensions.add(new DimensionTimerEntry(DUNGEON_JUMP_DIMENSION_ID, 900, 0));
        dimensions.add(new DimensionTimerEntry(DUNGEON_UNDERGROUND_DIMENSION_ID, 900, 0));
    }

    public static void load() {
        migrateOldConfigPath();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has("dimensions")) {
                        INSTANCE = GSON.fromJson(jsonElement, SafariTimerConfig.class);
                        addMissingDimensions();
                    } else {
                        INSTANCE = migrateOldConfig(jsonObject);
                    }
                } else {
                    INSTANCE = new SafariTimerConfig();
                }
                CobbleSafari.LOGGER.info("Configuration loaded from {}", CONFIG_PATH);
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("Failed to load config, using defaults", e);
                INSTANCE = new SafariTimerConfig();
            }
        } else {
            INSTANCE = new SafariTimerConfig();
            save();
        }
    }

    private static void addMissingDimensions() {
        if (INSTANCE == null) return;

        boolean modified = false;

        modified |= ensureDimensionConfig(SAFARI_DIMENSION_ID, 900);
        modified |= ensureDimensionConfig(DUNGEON_JUMP_DIMENSION_ID, 900);
        modified |= ensureDimensionConfig(DUNGEON_UNDERGROUND_DIMENSION_ID, 900);

        for (DimensionTimerEntry entry : INSTANCE.dimensions) {
            modified |= entry.initializeDefaults();
        }

        if (modified) {
            save();
        }
    }

    public static void syncDungeonDimensionTimersFromRegistry() {
        if (INSTANCE == null) {
            load();
        }
        boolean modified = false;
        modified |= ensureDimensionConfig(SAFARI_DIMENSION_ID, 900);
        modified |= ensureDimensionConfig(DUNGEON_JUMP_DIMENSION_ID, 900);
        modified |= ensureDimensionConfig(DUNGEON_UNDERGROUND_DIMENSION_ID, 900);
        for (DungeonConfig dungeon : DungeonDimensions.getAllDungeons()) {
            if (!dungeon.isExternallyManaged()) {
                modified |= ensureDimensionConfig(dungeon.getDimensionId(), dungeon.getTimerDurationSeconds());
            }
        }
        for (DimensionTimerEntry entry : INSTANCE.dimensions) {
            modified |= entry.initializeDefaults();
        }
        if (modified) {
            save();
        }
    }

    private static boolean ensureDimensionConfig(String dimensionId, int defaultDurationSeconds) {
        return ensureDimensionConfig(dimensionId, defaultDurationSeconds, 0);
    }

    private static boolean ensureDimensionConfig(String dimensionId, int defaultDurationSeconds, int resetHour) {
        Optional<DimensionTimerEntry> existing = INSTANCE.dimensions.stream()
                .filter(entry -> entry.getDimensionId().equals(dimensionId))
                .findFirst();
        if (existing.isEmpty()) {
            INSTANCE.dimensions.add(new DimensionTimerEntry(dimensionId, defaultDurationSeconds, resetHour));
            CobbleSafari.LOGGER.info("Added missing dimension config: {} ({} min)", dimensionId, defaultDurationSeconds / 60);
            return true;
        }
        return false;
    }

    public static boolean ensureDimensionEntry(String dimensionId, int defaultDurationSeconds, int resetHour) {
        if (INSTANCE == null) {
            load();
        }
        Optional<DimensionTimerEntry> existing = INSTANCE.dimensions.stream()
                .filter(entry -> entry.getDimensionId().equals(dimensionId))
                .findFirst();
        if (existing.isEmpty()) {
            INSTANCE.dimensions.add(new DimensionTimerEntry(dimensionId, defaultDurationSeconds, resetHour));
            CobbleSafari.LOGGER.info("Added dimension timer entry: {} ({} min)", dimensionId, defaultDurationSeconds / 60);
            save();
            return true;
        }
        return false;
    }

    private static SafariTimerConfig migrateOldConfig(JsonObject oldConfig) {
        SafariTimerConfig newConfig = new SafariTimerConfig();
        newConfig.dimensions.clear();

        int timerDurationSeconds = oldConfig.has("timerDurationSeconds") 
                ? oldConfig.get("timerDurationSeconds").getAsInt() : 300;
        int resetHour = oldConfig.has("resetHour") 
                ? oldConfig.get("resetHour").getAsInt() : 0;
        String dimensionId = oldConfig.has("dimensionId") 
                ? oldConfig.get("dimensionId").getAsString() : SAFARI_DIMENSION_ID;

        newConfig.dimensions.add(new DimensionTimerEntry(dimensionId, timerDurationSeconds, resetHour));

        CobbleSafari.LOGGER.info("Migrated old config format to new multi-dimension format");
        save();

        return newConfig;
    }

    private static void migrateOldConfigPath() {
        Path oldPath = CONFIG_DIR.getParent().resolve("cobblesafari_timer.json");
        Path oldPath2 = CONFIG_DIR.resolve("timer.json");
        
        if (Files.exists(oldPath) && !Files.exists(CONFIG_PATH)) {
            try {
                Files.createDirectories(CONFIG_DIR);
                Files.move(oldPath, CONFIG_PATH);
                CobbleSafari.LOGGER.info("Migrated config from {} to {}", oldPath, CONFIG_PATH);
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("Failed to migrate config file", e);
            }
        } else if (Files.exists(oldPath2) && !Files.exists(CONFIG_PATH)) {
            try {
                Files.move(oldPath2, CONFIG_PATH);
                CobbleSafari.LOGGER.info("Migrated config from {} to {}", oldPath2, CONFIG_PATH);
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("Failed to migrate config file", e);
            }
        }
    }

    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new SafariTimerConfig();
        }
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to create config directory", e);
        }
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
            CobbleSafari.LOGGER.info("Configuration saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to save config", e);
        }
    }

    public static Optional<DimensionTimerEntry> getDimensionConfig(String dimensionId) {
        if (INSTANCE == null) return Optional.empty();
        return INSTANCE.dimensions.stream()
                .filter(entry -> entry.getDimensionId().equals(dimensionId))
                .findFirst();
    }

    public static List<String> getConfiguredDimensionIds() {
        if (INSTANCE == null) return List.of();
        return INSTANCE.dimensions.stream()
                .map(DimensionTimerEntry::getDimensionId)
                .toList();
    }

    public static boolean hasDimensionTimer(String dimensionId) {
        return getDimensionConfig(dimensionId).isPresent();
    }

    public static String getSafariDimensionId() {
        return SAFARI_DIMENSION_ID;
    }

    public static int getTimerDurationSeconds() {
        return getDimensionConfig(SAFARI_DIMENSION_ID)
                .map(DimensionTimerEntry::getTimerDurationSeconds)
                .orElse(300);
    }

    public static int getTimerDurationTicks() {
        return getTimerDurationSeconds() * 20;
    }

    public static int getResetHour() {
        return getDimensionConfig(SAFARI_DIMENSION_ID)
                .map(DimensionTimerEntry::getResetHour)
                .orElse(0);
    }

    public static String getDimensionId() {
        return SAFARI_DIMENSION_ID;
    }

}
