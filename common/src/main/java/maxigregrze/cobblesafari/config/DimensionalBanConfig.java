package maxigregrze.cobblesafari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Server configuration for dimensional restrictions.
 * Manual edits are respected after first load.
 */
public class DimensionalBanConfig {
    public static final int CONFIG_VERSION = 1;

    private static final String KEY_CONFIG_VERSION = "CONFIG_VERSION";

    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("dimensional_restrictions_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static DimensionalBanData data = new DimensionalBanData();
    public static DimensionalBanData clientData = null;

    private DimensionalBanConfig() {}

    /**
     * Returns the effective ban data for the current side.
     * On the client, returns the server-synced data if available, otherwise falls back to defaults.
     * On the server, always returns the loaded config data.
     */
    public static DimensionalBanData getEffectiveData() {
        return clientData != null ? clientData : data;
    }

    /**
     * Called on the client when receiving a sync payload from the server.
     */
    public static void applyClientSync(Map<String, DimensionalBanData.DimensionRestrictions> dimensions) {
        DimensionalBanData synced = new DimensionalBanData();
        synced.dimensions = dimensions;
        clientData = synced;
        CobbleSafari.LOGGER.info("CobbleSafari >> Dimensional ban config synced from server ({} dimensions)", dimensions.size());
    }

    /**
     * Called on the client when disconnecting from a server.
     */
    public static void clearClientData() {
        clientData = null;
    }

    private static void migrateOldConfigPath() {
        Path oldPath = CONFIG_DIR.getParent().resolve("cobblesafari_dimensionalban.json");
        Path oldPath2 = CONFIG_DIR.resolve("dimensionalban.json");

        if (Files.exists(oldPath) && !Files.exists(CONFIG_PATH)) {
            try {
                Files.createDirectories(CONFIG_DIR);
                Files.move(oldPath, CONFIG_PATH);
                CobbleSafari.LOGGER.info("Migrated config from {} to {}", oldPath, CONFIG_PATH);
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("Failed to migrate config file", e);
            }
        } else if (Files.exists(oldPath2) && !Files.exists(CONFIG_PATH)) {
            try {
                Files.move(oldPath2, CONFIG_PATH);
                CobbleSafari.LOGGER.info("Migrated config from {} to {}", oldPath2, CONFIG_PATH);
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("Failed to migrate config file", e);
            }
        }
    }

    public static void load() {
        migrateOldConfigPath();

        if (!Files.exists(CONFIG_PATH)) {
            data = new DimensionalBanData();
            CobbleSafari.LOGGER.info(
                    "CobbleSafari >> dimensional_restrictions_config.json not found at {}, creating default file",
                    CONFIG_PATH);
            save();
            CobbleSafari.LOGGER.info(
                    "CobbleSafari >> dimensional_restrictions_config.json default values written to {}",
                    CONFIG_PATH);
            return;
        }

        try (Reader in = Files.newBufferedReader(CONFIG_PATH)) {
            DimensionalBanData parsed = GSON.fromJson(in, DimensionalBanData.class);
            data = parsed != null ? parsed : new DimensionalBanData();
            ensureDataDefaults(data);
            CobbleSafari.LOGGER.info("CobbleSafari >> dimensional_restrictions_config.json loaded successfully from {}", CONFIG_PATH);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error(
                    "CobbleSafari >> Failed to read or parse dimensional_restrictions_config.json at {} (invalid JSON or unexpected structure). Using in-memory defaults; the file on disk was not overwritten.",
                    CONFIG_PATH,
                    e);
            data = new DimensionalBanData();
        }
    }

    private static void ensureDataDefaults(DimensionalBanData loaded) {
        if (loaded.dimensions == null) {
            loaded.dimensions = new HashMap<>();
        }
        for (DimensionalBanData.DimensionRestrictions restrictions : loaded.dimensions.values()) {
            if (restrictions.bannedItems == null) {
                restrictions.bannedItems = new ArrayList<>();
            }
            if (restrictions.bannedBlocks == null) {
                restrictions.bannedBlocks = new ArrayList<>();
            }
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("Failed to create config directory", e);
        }
        try (Writer out = Files.newBufferedWriter(CONFIG_PATH)) {
            JsonObject json = new JsonObject();
            json.addProperty(KEY_CONFIG_VERSION, CONFIG_VERSION);
            for (Field field : DimensionalBanData.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                writeFieldTo(json, field);
            }
            GSON.toJson(json, out);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to save dimensional_restrictions_config.json", e);
        }
    }

    private static void writeFieldTo(JsonObject json, Field field) {
        field.setAccessible(true);
        try {
            json.add(field.getName(), GSON.toJsonTree(field.get(data)));
        } catch (IllegalAccessException e) {
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to access config field: {}", field.getName(), e);
        }
    }

    public static void ensureDimensionEntry(String dimensionId,
                                              DimensionalBanData.DimensionRestrictions restrictions) {
        if (!data.dimensions.containsKey(dimensionId)) {
            data.dimensions.put(dimensionId, restrictions);
            save();
        }
    }
}
