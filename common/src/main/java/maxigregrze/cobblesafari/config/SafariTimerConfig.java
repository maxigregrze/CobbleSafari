package maxigregrze.cobblesafari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.resources.ResourceLocation;

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
    /** Cached parse of {@link #SAFARI_DIMENSION_ID}; the id is a compile-time constant, so this never changes. */
    private static final ResourceLocation SAFARI_DIMENSION_RL = ResourceLocation.parse(SAFARI_DIMENSION_ID);
    private static final String DUNGEON_JUMP_DIMENSION_ID = "cobblesafari:dungeon_jump";
    private static final String DUNGEON_UNDERGROUND_DIMENSION_ID = "cobblesafari:dungeon_underground";
    private static final String UNION_ROOM_DIMENSION_ID = "cobblesafari:unionroom";

    private static SafariTimerConfig INSTANCE;

    private List<DimensionTimerEntry> dimensions = new ArrayList<>();

    public SafariTimerConfig() {
        dimensions.add(new DimensionTimerEntry(SAFARI_DIMENSION_ID, 900, 0));
        dimensions.add(new DimensionTimerEntry(DUNGEON_JUMP_DIMENSION_ID, 900, 0));
        dimensions.add(new DimensionTimerEntry(DUNGEON_UNDERGROUND_DIMENSION_ID, 900, 0));
        dimensions.add(new DimensionTimerEntry(UNION_ROOM_DIMENSION_ID, 3600, 0));
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
                    CobbleSafari.LOGGER.warn(
                            "CobbleSafari >> dimensional_timer_config.json at {} was not a JSON object; using defaults",
                            CONFIG_PATH);
                    INSTANCE = new SafariTimerConfig();
                }
                CobbleSafari.LOGGER.info("CobbleSafari >> dimensional_timer_config.json loaded successfully from {}", CONFIG_PATH);
            } catch (IOException e) {
                CobbleSafari.LOGGER.error(
                        "CobbleSafari >> Failed to read dimensional_timer_config.json at {} (I/O error). Using defaults; the file on disk was not modified.",
                        CONFIG_PATH,
                        e);
                INSTANCE = new SafariTimerConfig();
            } catch (Exception e) {
                CobbleSafari.LOGGER.error(
                        "CobbleSafari >> Failed to parse dimensional_timer_config.json at {} (invalid JSON). Using defaults; the file on disk was not modified.",
                        CONFIG_PATH,
                        e);
                INSTANCE = new SafariTimerConfig();
            }
        } else {
            CobbleSafari.LOGGER.info(
                    "CobbleSafari >> dimensional_timer_config.json not found at {}, creating default file",
                    CONFIG_PATH);
            INSTANCE = new SafariTimerConfig();
            CobbleSafari.LOGGER.info("CobbleSafari >> Persisting dimensional_timer_config.json after load (first-time default file)");
            save();
        }
    }

    private static void addMissingDimensions() {
        if (INSTANCE == null) return;

        boolean modified = false;

        modified |= ensureDimensionConfig(SAFARI_DIMENSION_ID, 900);
        modified |= ensureDimensionConfig(DUNGEON_JUMP_DIMENSION_ID, 900);
        modified |= ensureDimensionConfig(DUNGEON_UNDERGROUND_DIMENSION_ID, 900);
        modified |= ensureDimensionConfig(UNION_ROOM_DIMENSION_ID, 3600);

        for (DimensionTimerEntry entry : INSTANCE.dimensions) {
            modified |= entry.initializeDefaults();
        }

        if (modified) {
            CobbleSafari.LOGGER.info("CobbleSafari >> Persisting dimensional_timer_config.json after load (added or normalized dimension timer entries)");
            save();
        }
    }

    /** Returns the loaded singleton, loading or creating defaults as needed. Never null. */
    private static SafariTimerConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        if (INSTANCE == null) {
            INSTANCE = new SafariTimerConfig();
        }
        return INSTANCE;
    }

    public static void syncDungeonDimensionTimersFromRegistry() {
        SafariTimerConfig inst = getInstance();
        boolean modified = false;
        modified |= ensureDimensionConfig(SAFARI_DIMENSION_ID, 900);
        modified |= ensureDimensionConfig(DUNGEON_JUMP_DIMENSION_ID, 900);
        modified |= ensureDimensionConfig(DUNGEON_UNDERGROUND_DIMENSION_ID, 900);
        modified |= ensureDimensionConfig(UNION_ROOM_DIMENSION_ID, 3600);
        for (DungeonConfig dungeon : DungeonDimensions.getAllDungeons()) {
            if (!dungeon.isExternallyManaged()) {
                modified |= ensureDimensionConfig(dungeon.getDimensionId(), dungeon.getTimerDurationSeconds());
            }
        }
        for (DimensionTimerEntry entry : inst.dimensions) {
            modified |= entry.initializeDefaults();
        }
        if (modified) {
            CobbleSafari.LOGGER.info("CobbleSafari >> Persisting dimensional_timer_config.json (dungeon registry sync added or normalized entries)");
            save();
        }
    }

    private static boolean ensureDimensionConfig(String dimensionId, int defaultDurationSeconds) {
        return ensureDimensionConfig(dimensionId, defaultDurationSeconds, 0);
    }

    private static boolean ensureDimensionConfig(String dimensionId, int defaultDurationSeconds, int resetHour) {
        SafariTimerConfig inst = getInstance();
        Optional<DimensionTimerEntry> existing = inst.dimensions.stream()
                .filter(entry -> entry.getDimensionId().equals(dimensionId))
                .findFirst();
        if (existing.isEmpty()) {
            inst.dimensions.add(new DimensionTimerEntry(dimensionId, defaultDurationSeconds, resetHour));
            CobbleSafari.LOGGER.info("Added missing dimension config: {} ({} min)", dimensionId, defaultDurationSeconds / 60);
            return true;
        }
        return false;
    }

    public static boolean ensureDimensionEntry(String dimensionId, int defaultDurationSeconds, int resetHour) {
        SafariTimerConfig inst = getInstance();
        Optional<DimensionTimerEntry> existing = inst.dimensions.stream()
                .filter(entry -> entry.getDimensionId().equals(dimensionId))
                .findFirst();
        if (existing.isEmpty()) {
            inst.dimensions.add(new DimensionTimerEntry(dimensionId, defaultDurationSeconds, resetHour));
            CobbleSafari.LOGGER.info("Added dimension timer entry: {} ({} min)", dimensionId, defaultDurationSeconds / 60);
            CobbleSafari.LOGGER.info("CobbleSafari >> Persisting dimensional_timer_config.json (new dimension timer entry)");
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

        CobbleSafari.LOGGER.info("CobbleSafari >> Migrated legacy dimensional_timer_config.json to multi-dimension format");
        CobbleSafari.LOGGER.info("CobbleSafari >> Persisting dimensional_timer_config.json after load (legacy format migration)");
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
            CobbleSafari.LOGGER.info("CobbleSafari >> dimensional_timer_config.json written to {}", CONFIG_PATH);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to save dimensional_timer_config.json", e);
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

    /** Pre-parsed safari dimension id; avoids re-parsing the constant string on hot paths. */
    public static ResourceLocation getSafariDimensionRL() {
        return SAFARI_DIMENSION_RL;
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

    /** @deprecated use {@link #getSafariDimensionId()} instead. */
    @Deprecated
    public static String getDimensionId() {
        return getSafariDimensionId();
    }

}
