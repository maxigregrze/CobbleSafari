package maxigregrze.cobblesafari.dungeon;

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
import java.util.List;
import java.util.Optional;

public class PortalSpawnConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir()
            .resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("dungeon_spawn_config.json");

    private static final String DUNGEON_UNDERGROUND_ID = "dungeon_underground";
    private static final String DUNGEON_JUMP_ID = "dungeon_jump";

    private static PortalSpawnConfig INSTANCE;

    private int spawnIntervalSeconds = 600;
    private int portalLifetimeMinutes = 30;
    private int notificationRadiusBlocks = 80;
    private int spawnRadiusMin = 32;
    private int spawnRadiusMax = 80;
    private boolean enabled = true;
    private List<DungeonDimensionEntry> dimensions = new ArrayList<>();

    public PortalSpawnConfig() {
        dimensions.add(new DungeonDimensionEntry(DUNGEON_UNDERGROUND_ID, true, 1));
        dimensions.add(new DungeonDimensionEntry(DUNGEON_JUMP_ID, false, 1));
    }

    private static void migrateOldConfigPath() {
        Path oldPath = CONFIG_DIR.getParent().resolve("cobblesafari_dungeon_portals.json");
        Path oldPath2 = CONFIG_DIR.resolve("dungeon_portals.json");
        
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

    public static void load() {
        migrateOldConfigPath();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(reader, PortalSpawnConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new PortalSpawnConfig();
                }
                if (INSTANCE.dimensions == null) {
                    INSTANCE.dimensions = new ArrayList<>();
                }
                addMissingDimensions();
                save();
                CobbleSafari.LOGGER.info("Dungeon portal config loaded from {}", CONFIG_PATH);
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("Failed to load dungeon portal config, using defaults", e);
                INSTANCE = new PortalSpawnConfig();
            }
        } else {
            INSTANCE = new PortalSpawnConfig();
            save();
        }
    }

    private static void addMissingDimensions() {
        if (INSTANCE == null) return;

        boolean modified = false;
        modified |= ensureDimensionConfig(DUNGEON_UNDERGROUND_ID, true, 1);
        modified |= ensureDimensionConfig(DUNGEON_JUMP_ID, false, 1);

        if (modified) {
            save();
        }
    }

    public static boolean ensureDimensionEntry(String dungeonId, boolean defaultEnabled, int defaultWeight) {
        if (INSTANCE == null) {
            load();
        }
        Optional<DungeonDimensionEntry> existing = INSTANCE.dimensions.stream()
                .filter(entry -> entry.getDimensionId().equals(dungeonId))
                .findFirst();
        if (existing.isEmpty()) {
            INSTANCE.dimensions.add(new DungeonDimensionEntry(dungeonId, defaultEnabled, defaultWeight));
            CobbleSafari.LOGGER.info("Added dungeon dimension config: {} (enabled={}, weight={})",
                    dungeonId, defaultEnabled, defaultWeight);
            save();
            return true;
        }
        return false;
    }

    private static boolean ensureDimensionConfig(String dimensionId, boolean defaultEnabled, int defaultWeight) {
        Optional<DungeonDimensionEntry> existing = INSTANCE.dimensions.stream()
                .filter(entry -> entry.getDimensionId().equals(dimensionId))
                .findFirst();

        if (existing.isEmpty()) {
            INSTANCE.dimensions.add(new DungeonDimensionEntry(dimensionId, defaultEnabled, defaultWeight));
            CobbleSafari.LOGGER.info("Added missing dungeon dimension config: {} (enabled={})",
                    dimensionId, defaultEnabled);
            return true;
        }
        return false;
    }

    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new PortalSpawnConfig();
        }
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to create config directory", e);
        }
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
            CobbleSafari.LOGGER.info("Dungeon portal config saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to save dungeon portal config", e);
        }
    }

    public static int getSpawnIntervalTicks() {
        return getInstance().spawnIntervalSeconds * 20;
    }

    public static int getPortalLifetimeTicks() {
        return getInstance().portalLifetimeMinutes * 60 * 20;
    }

    public static int getNotificationRadius() {
        return getInstance().notificationRadiusBlocks;
    }

    public static int getSpawnRadiusMin() {
        return getInstance().spawnRadiusMin;
    }

    public static int getSpawnRadiusMax() {
        return getInstance().spawnRadiusMax;
    }

    public static boolean isEnabled() {
        return getInstance().enabled;
    }

    public static Optional<DungeonDimensionEntry> getDimensionConfig(String dimensionId) {
        if (INSTANCE == null) return Optional.empty();
        return getInstance().dimensions.stream()
                .filter(entry -> entry.getDimensionId().equals(dimensionId))
                .findFirst();
    }

    public static List<DungeonDimensionEntry> getEnabledDimensions() {
        return getInstance().dimensions.stream()
                .filter(DungeonDimensionEntry::isEnabled)
                .toList();
    }

    public static List<DungeonDimensionEntry> getAllDimensionConfigs() {
        return new ArrayList<>(getInstance().dimensions);
    }

    private static PortalSpawnConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }
}
