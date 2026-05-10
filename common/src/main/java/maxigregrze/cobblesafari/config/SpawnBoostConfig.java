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

public class SpawnBoostConfig {
    public static final int CONFIG_VERSION = 4;
    
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("encounter_boost_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static SpawnBoostConfigData data = new SpawnBoostConfigData();
    
    private SpawnBoostConfig() {}
    
    private static void migrateOldConfigPath() {
        Path oldPath = CONFIG_DIR.getParent().resolve("cobblesafari_spawnboost.json");
        Path oldPath2 = CONFIG_DIR.resolve("spawnboost.json");
        
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
                    "CobbleSafari >> encounter_boost_config.json not found at {}, creating default file",
                    CONFIG_PATH);
            CobbleSafari.LOGGER.info("CobbleSafari >> Persisting encounter_boost_config.json after load (first-time default file)");
            save();
            return;
        }

        try (Reader in = Files.newBufferedReader(CONFIG_PATH)) {
            JsonReader reader = new JsonReader(in);
            reader.setLenient(true);
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject defaultJson = GSON.toJsonTree(new SpawnBoostConfigData()).getAsJsonObject();

            int loadedVersion = json.has("CONFIG_VERSION") ? json.get("CONFIG_VERSION").getAsInt() : 0;
            if (loadedVersion < CONFIG_VERSION) {
                CobbleSafari.LOGGER.info("CobbleSafari >> Spawn boost config migrating from v{} to v{}!", loadedVersion, CONFIG_VERSION);
                json.addProperty("CONFIG_VERSION", CONFIG_VERSION);
                needsSave = true;
            }

            JsonElement upgraded = upgrade(json, defaultJson);
            if (!upgraded.equals(json)) {
                CobbleSafari.LOGGER.info("CobbleSafari >> encounter_boost_config.json schema merged with defaults (unknown keys removed or missing keys filled)");
                needsSave = true;
            }

            data = GSON.fromJson(upgraded, SpawnBoostConfigData.class);
            CobbleSafari.LOGGER.info("CobbleSafari >> encounter_boost_config.json loaded successfully from {}", CONFIG_PATH);

        } catch (Exception e) {
            CobbleSafari.LOGGER.error(
                    "CobbleSafari >> Failed to read or parse encounter_boost_config.json at {} (invalid JSON or unexpected structure). Keeping previously loaded values in memory; the file on disk was not overwritten.",
                    CONFIG_PATH,
                    e);
            return;
        }

        if (needsSave) {
            CobbleSafari.LOGGER.info("CobbleSafari >> Persisting encounter_boost_config.json after load (version bump or schema merge)");
            save();
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
            json.addProperty("CONFIG_VERSION", CONFIG_VERSION);
            for (Field field : SpawnBoostConfigData.class.getDeclaredFields()) {
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
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to save encounter_boost_config.json", e);
        }
    }
    
    private static JsonElement upgrade(JsonElement original, JsonElement defaults) {
        if (original.isJsonObject() && defaults.isJsonObject()) {
            JsonObject origObj = original.getAsJsonObject();
            JsonObject defObj = defaults.getAsJsonObject();
            
            List<String> keys = new ArrayList<>(origObj.keySet());
            for (String key : keys) {
                if (!defObj.has(key)) {
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
