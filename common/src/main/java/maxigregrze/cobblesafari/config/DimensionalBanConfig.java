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

    private static final String KEY_CONFIG_VERSION = "CONFIG_VERSION";
    private static final String KEY_DIMENSIONS = "dimensions";

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
            CobbleSafari.LOGGER.info(
                    "CobbleSafari >> dimensional_restrictions_config.json not found at {}, creating default file",
                    CONFIG_PATH);
            persistAfterLoadNormalization("first-time default file");
            CobbleSafari.LOGGER.info(
                    "CobbleSafari >> dimensional_restrictions_config.json default values written to {}",
                    CONFIG_PATH);
            return;
        }

        try (Reader in = Files.newBufferedReader(CONFIG_PATH)) {
            JsonReader reader = new JsonReader(in);
            reader.setLenient(true);
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject defaultJson = GSON.toJsonTree(new DimensionalBanData()).getAsJsonObject();

            JsonObject defaultDims = defaultJson.getAsJsonObject(KEY_DIMENSIONS);
            JsonObject userDims = json.has(KEY_DIMENSIONS) && json.get(KEY_DIMENSIONS).isJsonObject()
                    ? json.getAsJsonObject(KEY_DIMENSIONS)
                    : new JsonObject();
            JsonElement dimensionsSnapshotBeforeMerge = JsonParser.parseString(GSON.toJson(userDims));
            JsonObject mergedDims = mergeDimensionsPreservingCustomEntries(userDims, defaultDims);
            if (!dimensionsSnapshotBeforeMerge.equals(JsonParser.parseString(GSON.toJson(mergedDims)))) {
                needsSave = true;
            }
            json.add(KEY_DIMENSIONS, mergedDims);

            int loadedVersion = json.has(KEY_CONFIG_VERSION) ? json.get(KEY_CONFIG_VERSION).getAsInt() : 0;
            if (loadedVersion < CONFIG_VERSION) {
                CobbleSafari.LOGGER.info("CobbleSafari >> Dimensional ban config migrating from v{} to v{}!", loadedVersion, CONFIG_VERSION);
                json.addProperty(KEY_CONFIG_VERSION, CONFIG_VERSION);
                needsSave = true;
            }

            List<String> topLevelKeys = new ArrayList<>(json.keySet());
            for (String key : topLevelKeys) {
                if (!key.equals(KEY_DIMENSIONS) && !key.equals(KEY_CONFIG_VERSION)) {
                    json.remove(key);
                    needsSave = true;
                }
            }

            data = GSON.fromJson(json, DimensionalBanData.class);
            CobbleSafari.LOGGER.info("CobbleSafari >> dimensional_restrictions_config.json loaded successfully from {}", CONFIG_PATH);

        } catch (Exception e) {
            CobbleSafari.LOGGER.error(
                    "CobbleSafari >> Failed to read or parse dimensional_restrictions_config.json at {} (invalid JSON or unexpected structure). Using in-memory defaults; the file on disk was not overwritten.",
                    CONFIG_PATH,
                    e);
            return;
        }

        if (needsSave) {
            persistAfterLoadNormalization("version bump, schema merge, or restored custom dimension entries");
        }
    }

    private static void persistAfterLoadNormalization(String reason) {
        CobbleSafari.LOGGER.info(
                "CobbleSafari >> Persisting dimensional_restrictions_config.json after load ({})",
                reason);
        save();
    }

    private static JsonObject mergeDimensionsPreservingCustomEntries(JsonObject userDims, JsonObject defaultDims) {
        JsonObject userWorking = userDims == null || userDims.size() == 0
                ? new JsonObject()
                : JsonParser.parseString(GSON.toJson(userDims)).getAsJsonObject();
        JsonObject emptyRestrictionTemplate = GSON.toJsonTree(new DimensionalBanData.DimensionRestrictions()).getAsJsonObject();
        JsonObject result = new JsonObject();
        for (String key : userWorking.keySet()) {
            JsonElement userVal = userWorking.get(key);
            JsonElement template = defaultDims.has(key) ? defaultDims.get(key) : emptyRestrictionTemplate;
            result.add(key, upgrade(userVal, template));
        }
        for (Map.Entry<String, JsonElement> entry : defaultDims.entrySet()) {
            if (!result.has(entry.getKey())) {
                result.add(entry.getKey(), entry.getValue());
            }
        }
        return result;
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

    private static JsonElement upgrade(JsonElement original, JsonElement defaults) {
        if (original.isJsonObject() && defaults.isJsonObject()) {
            JsonObject origObj = original.getAsJsonObject();
            JsonObject defObj = defaults.getAsJsonObject();

            List<String> keys = new ArrayList<>(origObj.keySet());
            for (String key : keys) {
                if (!defObj.has(key) && !key.equals(KEY_CONFIG_VERSION)) {
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
