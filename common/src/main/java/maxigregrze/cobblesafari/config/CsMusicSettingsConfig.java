package maxigregrze.cobblesafari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Server configuration for global csmusic settings ({@code csmusic_settings.json}).
 * Written only on <b>first creation</b>; any manual edit is respected afterward.
 *
 * <p>Supersedes {@code dimensional_music.json}: when only the legacy file is present it is read
 * once, the fields that still exist are carried over, the new file is written, and the legacy file
 * is renamed to {@code dimensional_music.json.old}. Renaming rather than deleting keeps a
 * legacy {@code dimensions} map readable for whoever still has to port it to definition files.</p>
 */
public final class CsMusicSettingsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path SETTINGS_PATH = CONFIG_DIR.resolve("csmusic_settings.json");
    private static final Path LEGACY_PATH = CONFIG_DIR.resolve("dimensional_music.json");
    private static final Path LEGACY_DONE = CONFIG_DIR.resolve("dimensional_music.json.old");

    /** Keys the current DTO still understands; anything else in the legacy file is reported. */
    private static final Set<String> KNOWN_KEYS = Set.of(
            "enabled", "defaultAreaPriority", "structureCacheTtlTicks",
            "arbitrationIntervalTicks", "repeatBeforeShuffle");

    public static CsMusicSettingsData data = new CsMusicSettingsData();

    private CsMusicSettingsConfig() {}

    public static void load() {
        // The new file always wins: this is also what makes the migration idempotent, since a
        // second boot can no longer reach the legacy branch and overwrite later manual edits.
        if (Files.exists(SETTINGS_PATH)) {
            readInto(SETTINGS_PATH);
            return;
        }
        if (Files.exists(LEGACY_PATH)) {
            migrateFromLegacy();
            return;
        }
        data = new CsMusicSettingsData();
        save();
        CobbleSafari.LOGGER.info("CobbleSafari >> Created default csmusic_settings.json at {}", SETTINGS_PATH);
    }

    private static void readInto(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            CsMusicSettingsData parsed = GSON.fromJson(reader, CsMusicSettingsData.class);
            data = parsed != null ? parsed : new CsMusicSettingsData();
            CobbleSafari.LOGGER.info("CobbleSafari >> csmusic_settings.json loaded from {}", path);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error(
                    "CobbleSafari >> Failed to read csmusic_settings.json at {} - using in-memory defaults, file not overwritten",
                    path, e);
            data = new CsMusicSettingsData();
        }
    }

    /**
     * Reads the legacy file twice: once as the current DTO (Gson silently drops keys that no
     * longer exist, so only live fields are carried over), once as a raw object to name the keys
     * that were dropped. Fields absent from the legacy file keep their initialiser value, so
     * {@code repeatBeforeShuffle} lands at 0 and the migration changes nothing audible.
     */
    private static void migrateFromLegacy() {
        try (Reader reader = Files.newBufferedReader(LEGACY_PATH)) {
            CsMusicSettingsData parsed = GSON.fromJson(reader, CsMusicSettingsData.class);
            data = parsed != null ? parsed : new CsMusicSettingsData();
        } catch (Exception e) {
            CobbleSafari.LOGGER.error(
                    "CobbleSafari >> Failed to read the legacy dimensional_music.json at {} - starting from defaults",
                    LEGACY_PATH, e);
            data = new CsMusicSettingsData();
        }

        reportDroppedKeys();
        save();
        archiveLegacyFile();
        CobbleSafari.LOGGER.info(
                "CobbleSafari >> Migrated dimensional_music.json to csmusic_settings.json at {}", SETTINGS_PATH);
    }

    private static void reportDroppedKeys() {
        List<String> dropped = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(LEGACY_PATH)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return;
            }
            JsonObject json = root.getAsJsonObject();
            for (String key : json.keySet()) {
                if (!KNOWN_KEYS.contains(key)) {
                    dropped.add(key);
                }
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn(
                    "CobbleSafari >> Could not inspect the legacy dimensional_music.json for obsolete keys", e);
            return;
        }
        if (dropped.isEmpty()) {
            return;
        }
        CobbleSafari.LOGGER.warn("CobbleSafari >> dimensional_music.json carried {} obsolete key(s), dropped: {}",
                dropped.size(), String.join(", ", dropped));
        if (dropped.contains("dimensions")) {
            CobbleSafari.LOGGER.warn("CobbleSafari >> the 'dimensions' map no longer drives csmusic. Per-dimension "
                    + "music is now declared by trigger rules in data/<namespace>/csmusic/definition/*.json, "
                    + "using \"when\": {\"dimension\": \"<id>\"}. The old values remain readable in {}", LEGACY_DONE);
        }
    }

    /**
     * Renames rather than deletes. A failure here is harmless - {@link #load} keys idempotency on
     * the presence of the new file, not on the absence of the legacy one.
     */
    private static void archiveLegacyFile() {
        try {
            Files.move(LEGACY_PATH, LEGACY_DONE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn(
                    "CobbleSafari >> Could not rename {} to {} - it is now ignored and can be removed by hand",
                    LEGACY_PATH, LEGACY_DONE, e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(SETTINGS_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to write csmusic_settings.json", e);
        }
    }
}
