package maxigregrze.cobblesafari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
    public static final int CONFIG_VERSION = 3;

    /**
     * Version that relifts the {@code cobblesafari:dungeon_jump} restrictions. The entry predates the
     * Bell Tower dungeon and shipped with permissive values that described an empty dimension, not a
     * server owner's choice; see {@link #migrateDungeonJumpRestrictions}.
     */
    private static final int VERSION_DUNGEON_JUMP_RESTRICTIONS = 2;

    /**
     * Version that creates the entries for the two dimensions of action plan 183 (S20). Keyed on
     * the version stamp and never on the values, exactly like the palier above: a server owner who
     * later loosens either dimension keeps that choice across every subsequent start.
     */
    private static final int VERSION_ULTRA_DIMENSIONS = 3;

    private static final String KEY_CONFIG_VERSION = "CONFIG_VERSION";
    private static final String KEY_DIMENSIONS = "dimensions";
    private static final String DUNGEON_JUMP_DIMENSION_ID = "cobblesafari:dungeon_jump";
    private static final String[] ULTRA_DIMENSION_IDS = {
            "cobblesafari:dungeon_megalopolis", "cobblesafari:dungeon_ultraspace" };

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
            JsonObject root = JsonParser.parseReader(in).getAsJsonObject();
            DimensionalBanData parsed = GSON.fromJson(root, DimensionalBanData.class);
            data = parsed != null ? parsed : new DimensionalBanData();
            ensureDataDefaults(data);
            int loadedVersion = readStoredVersion(root);
            boolean mountingMigrated = migrateForbidMounting(root, data);
            boolean jumpMigrated = migrateDungeonJumpRestrictions(loadedVersion, data);
            boolean ultraMigrated = migrateUltraDimensions(loadedVersion, data);
            CobbleSafari.LOGGER.info("CobbleSafari >> dimensional_restrictions_config.json loaded successfully from {}", CONFIG_PATH);
            if (mountingMigrated || jumpMigrated || ultraMigrated) {
                save();
                if (ultraMigrated) {
                    CobbleSafari.LOGGER.info(
                            "CobbleSafari >> dimensional_restrictions_config.json migrated from v{} to v{}: entries created for the Ultra dimensions",
                            loadedVersion, CONFIG_VERSION);
                }
                if (mountingMigrated) {
                    CobbleSafari.LOGGER.info(
                            "CobbleSafari >> dimensional_restrictions_config.json migrated: forbidMounting defaults written (true for cobblesafari:dungeon_distortion)");
                }
                if (jumpMigrated) {
                    CobbleSafari.LOGGER.info(
                            "CobbleSafari >> dimensional_restrictions_config.json migrated from v{} to v{}: {} restrictions reset to defaults",
                            loadedVersion, CONFIG_VERSION, DUNGEON_JUMP_DIMENSION_ID);
                }
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.error(
                    "CobbleSafari >> Failed to read or parse dimensional_restrictions_config.json at {} (invalid JSON or unexpected structure). Using in-memory defaults; the file on disk was not overwritten.",
                    CONFIG_PATH,
                    e);
            data = new DimensionalBanData();
        }
    }

    /**
     * File predating the forbidMounting field: if at least one dimension entry does not declare
     * the key, forces true for the distortion (when its key is absent) and reports that a rewrite
     * of the file is needed to materialize the defaults. Entries that declare the key keep their
     * value untouched, so manual edits stay respected.
     */
    private static boolean migrateForbidMounting(JsonObject root, DimensionalBanData loaded) {
        JsonObject dims = root.has(KEY_DIMENSIONS) && root.get(KEY_DIMENSIONS).isJsonObject()
                ? root.getAsJsonObject(KEY_DIMENSIONS) : null;
        if (dims == null) {
            return false;
        }
        boolean migrationNeeded = false;
        for (String dimensionId : loaded.dimensions.keySet()) {
            if (dims.has(dimensionId) && dims.get(dimensionId).isJsonObject()
                    && !dims.getAsJsonObject(dimensionId).has("forbidMounting")) {
                migrationNeeded = true;
                if ("cobblesafari:dungeon_distortion".equals(dimensionId)) {
                    loaded.dimensions.get(dimensionId).forbidMounting = true;
                }
            }
        }
        return migrationNeeded;
    }

    /**
     * One-shot creation of the {@code dungeon_megalopolis} and {@code dungeon_ultraspace} entries
     * (action plan 183, section 11.9, finding S20).
     *
     * <p>Only <b>missing</b> entries are written. An owner who already added one by hand - or who
     * ran a build where it existed - keeps it untouched, so this can never overwrite a decision.
     * The version stamp is what stops it running twice, which is what lets a later loosening of
     * either dimension survive.</p>
     */
    private static boolean migrateUltraDimensions(int loadedVersion, DimensionalBanData loaded) {
        if (loadedVersion >= VERSION_ULTRA_DIMENSIONS) {
            return false;
        }
        DimensionalBanData defaults = new DimensionalBanData();
        boolean changed = false;
        for (String dimensionId : ULTRA_DIMENSION_IDS) {
            if (loaded.dimensions.containsKey(dimensionId)) {
                continue;
            }
            DimensionalBanData.DimensionRestrictions preset = defaults.dimensions.get(dimensionId);
            if (preset != null) {
                loaded.dimensions.put(dimensionId, preset);
                changed = true;
            }
        }
        return changed;
    }

    /** @return the version stamped in the file, or 0 when absent or unreadable. */
    private static int readStoredVersion(JsonObject root) {
        if (!root.has(KEY_CONFIG_VERSION) || !root.get(KEY_CONFIG_VERSION).isJsonPrimitive()) {
            return 0;
        }
        try {
            return root.get(KEY_CONFIG_VERSION).getAsInt();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * One-shot relift of the {@code cobblesafari:dungeon_jump} restrictions (action plan 175, § 7.3).
     * The entry has existed since long before the dungeon did, and shipped with
     * {@code allowBlockBreaking = true} and no {@code forbidMounting} - values that described an empty
     * dimension. Left alone they would let a Bell Tower run be walked straight past, so they are
     * overwritten once with the current defaults.
     *
     * <p>Keyed on the file version and never on the values themselves: once the file is stamped, a
     * server owner who loosens the dungeon again keeps that choice across every later start.
     *
     * <p>Only the five flags are rewritten. {@code bannedItems} and {@code bannedBlocks} are left
     * untouched, since anything in them was necessarily put there by hand.
     *
     * <p>{@code allowRotomSafety} (action plan 176) rides along on this same step rather than getting a
     * version of its own: the palier below was never published - 0.3.4 still writes
     * {@code CONFIG_VERSION = 1} - so every file in the wild passes through here anyway.
     */
    private static boolean migrateDungeonJumpRestrictions(int loadedVersion, DimensionalBanData loaded) {
        if (loadedVersion >= VERSION_DUNGEON_JUMP_RESTRICTIONS) {
            return false;
        }
        DimensionalBanData.DimensionRestrictions defaults =
                new DimensionalBanData().dimensions.get(DUNGEON_JUMP_DIMENSION_ID);
        if (defaults == null) {
            return false;
        }
        DimensionalBanData.DimensionRestrictions current = loaded.dimensions.get(DUNGEON_JUMP_DIMENSION_ID);
        if (current == null) {
            loaded.dimensions.put(DUNGEON_JUMP_DIMENSION_ID, defaults);
            return true;
        }
        current.allowBattle = defaults.allowBattle;
        current.allowBlockBreaking = defaults.allowBlockBreaking;
        current.allowBlockPlacing = defaults.allowBlockPlacing;
        current.forbidMounting = defaults.forbidMounting;
        current.allowRotomSafety = defaults.allowRotomSafety;
        return true;
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
