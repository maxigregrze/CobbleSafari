package maxigregrze.cobblesafari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DimensionalBanConfig {
    public static final int CONFIG_VERSION = 1;

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
        boolean needsSave = false;

        if (!Files.exists(CONFIG_PATH)) {
            save();
            CobbleSafari.LOGGER.info("CobbleSafari >> No dimensional ban config found, writing defaults!");
            return;
        }

        try (Reader in = Files.newBufferedReader(CONFIG_PATH)) {
            JsonReader reader = new JsonReader(in);
            reader.setLenient(true);
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject defaultJson = GSON.toJsonTree(new DimensionalBanData()).getAsJsonObject();

            int loadedVersion = json.has("CONFIG_VERSION") ? json.get("CONFIG_VERSION").getAsInt() : 0;
            if (loadedVersion < CONFIG_VERSION) {
                CobbleSafari.LOGGER.info("CobbleSafari >> Dimensional ban config migrating from v{} to v{}!", loadedVersion, CONFIG_VERSION);
                json.addProperty("CONFIG_VERSION", CONFIG_VERSION);
                needsSave = true;
            }

            JsonElement upgraded = upgrade(json, defaultJson);
            if (!upgraded.equals(json)) {
                CobbleSafari.LOGGER.info("CobbleSafari >> Dimensional ban config upgraded!");
                needsSave = true;
            }

            data = GSON.fromJson(upgraded, DimensionalBanData.class);

        } catch (Exception e) {
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to load dimensional ban config, using defaults!", e);
            return;
        }

        if (needsSave) save();
        CobbleSafari.LOGGER.info("CobbleSafari >> Dimensional ban config loaded!");
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("Failed to create config directory", e);
        }
        try (Writer out = Files.newBufferedWriter(CONFIG_PATH)) {
            JsonObject json = new JsonObject();
            json.addProperty("CONFIG_VERSION", CONFIG_VERSION);
            for (Field field : DimensionalBanData.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                try {
                    Object value = field.get(data);
                    JsonElement jsonVal = GSON.toJsonTree(value);
                    json.add(field.getName(), jsonVal);
                } catch (IllegalAccessException e) {
                    CobbleSafari.LOGGER.error("CobbleSafari >> Failed to access config field: {}", field.getName(), e);
                }
            }
            GSON.toJson(json, out);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to save dimensional ban config!", e);
        }
    }

    public static void ensureDimensionEntry(String dimensionId,
                                              DimensionalBanData.DimensionRestrictions restrictions) {
        if (!data.dimensions.containsKey(dimensionId)) {
            data.dimensions.put(dimensionId, restrictions);
            save();
        }
    }

    private static JsonElement upgrade(JsonElement original, JsonElement defaults) {
        if (original.isJsonObject() && defaults.isJsonObject()) {
            JsonObject origObj = original.getAsJsonObject();
            JsonObject defObj = defaults.getAsJsonObject();

            List<String> keys = new ArrayList<>(origObj.keySet());
            for (String key : keys) {
                if (!defObj.has(key) && !key.equals("CONFIG_VERSION")) {
                    origObj.remove(key);
                }
            }

            for (Map.Entry<String, JsonElement> entry : defObj.entrySet()) {
                String key = entry.getKey();
                JsonElement defVal = entry.getValue();
                if (origObj.has(key)) {
                    origObj.add(key, upgrade(origObj.get(key), defVal));
                } else {
                    origObj.add(key, defVal);
                }
            }
            return origObj;
        }
        return original;
    }
}
