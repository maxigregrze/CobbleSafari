package maxigregrze.cobblesafari.dungeon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PortalSpawnConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir()
            .resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("dungeon_spawn_config.json");

    private static final String DUNGEON_UNDERGROUND_ID = "dungeon_underground";
    private static final String DUNGEON_JUMP_ID = "dungeon_jump";
    private static final String DUNGEON_DISTORTION_ID = "dungeon_distortion";
    private static final String DUNGEON_MEGALOPOLIS_ID = "dungeon_megalopolis";
    private static final String DUNGEON_PARIS_ID = "dungeon_paris";
    private static final String DEFAULT_ALLOWED_DIMENSION = "minecraft:overworld";
    private static final List<String> DEFAULT_ALLOWED_DIMENSIONS = List.of(DEFAULT_ALLOWED_DIMENSION);

    /**
     * Version of {@code dungeon_spawn_config.json}. Bumped whenever a default has to be relifted on
     * existing files; the migration keys off this version and never off the flag's value, so a choice
     * made by the server owner after the migration is preserved forever (action plan 175, § 7.1).
     */
    private static final int CURRENT_CONFIG_VERSION = 2;
    private static final int MAX_REPELLENT_RADIUS = 64;

    private static PortalSpawnConfig INSTANCE;

    /** Parsed {@link #bannedBiomes}, rebuilt on every load (static, so never serialized). */
    private static Set<ResourceKey<Biome>> bannedBiomeIds = Set.of();
    private static List<TagKey<Biome>> bannedBiomeTags = List.of();

    /**
     * 0 = file written before the Bell Tower dungeon existed. Must stay 0 at declaration and must not
     * be set in the constructor: Gson runs both on an existing file, so a non-zero default here would
     * make a pre-migration file look already migrated. {@link #migrateConfigVersion()} stamps it.
     */
    private int configVersion = 0;

    private int spawnIntervalSeconds = 600;
    private int portalLifetimeMinutes = 30;
    private int notificationRadiusBlocks = 80;
    private Boolean enableNotification = false;
    private int spawnRadiusMin = 32;
    private int spawnRadiusMax = 80;
    private boolean enabled = true;
    private Float spawnChance = 1.0f;
    private List<String> allowedDimensions = new ArrayList<>(DEFAULT_ALLOWED_DIMENSIONS);
    private List<String> bannedBlocks = new ArrayList<>();
    /** Biome ids ({@code minecraft:plains}) or biome tags ({@code #minecraft:is_ocean}) where no portal spawns. */
    private List<String> bannedBiomes = new ArrayList<>();
    /** Cube radius scanned for {@code #cobblesafari:portal_repellents} around a candidate portal; 0 = off. */
    private int repellentRadius = 16;
    private List<DungeonDimensionEntry> dimensions = new ArrayList<>();

    public PortalSpawnConfig() {
        dimensions.add(new DungeonDimensionEntry(DUNGEON_UNDERGROUND_ID, true, 1));
        dimensions.add(new DungeonDimensionEntry(DUNGEON_JUMP_ID, true, 1));
        dimensions.add(new DungeonDimensionEntry(DUNGEON_DISTORTION_ID, true, 1));
        dimensions.add(new DungeonDimensionEntry(DUNGEON_MEGALOPOLIS_ID, true, 1));
        dimensions.add(new DungeonDimensionEntry(DUNGEON_PARIS_ID, true, 1));
    }

    /**
     * One-shot migrations, keyed off the file version and never off a flag's value, so a choice made
     * by the server owner after a migration is preserved forever (action plan 175, section 7.1).
     * <ul>
     *   <li>v1: configurations written before the Bell Tower dungeon existed carry
     *   {@code dungeon_jump: isEnabled = false}, a value that expressed the absence of the dungeon and
     *   not a server owner's choice. It is lifted once.</li>
     *   <li>v2: the portal notification becomes opt-in (action plan 188, P1).</li>
     * </ul>
     */
    private static void migrateConfigVersion() {
        if (INSTANCE == null || INSTANCE.configVersion >= CURRENT_CONFIG_VERSION) {
            return;
        }
        if (INSTANCE.configVersion < 1) {
            for (DungeonDimensionEntry entry : INSTANCE.dimensions) {
                if (DUNGEON_JUMP_ID.equals(entry.getDimensionId()) && !entry.isEnabled()) {
                    entry.setEnabled(true);
                    CobbleSafari.LOGGER.info("dungeon_spawn_config.json migrated to v1: {} enabled", DUNGEON_JUMP_ID);
                }
            }
        }
        if (INSTANCE.configVersion < 2) {
            INSTANCE.enableNotification = false;
            CobbleSafari.LOGGER.info("dungeon_spawn_config.json migrated to v2: portal notification disabled by default");
        }
        INSTANCE.configVersion = CURRENT_CONFIG_VERSION;
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
                if (INSTANCE.bannedBlocks == null) {
                    INSTANCE.bannedBlocks = new ArrayList<>();
                }
                if (INSTANCE.bannedBiomes == null) {
                    INSTANCE.bannedBiomes = new ArrayList<>();
                }
                INSTANCE.repellentRadius = Math.clamp(INSTANCE.repellentRadius, 0, MAX_REPELLENT_RADIUS);
                clampSpawnChanceInInstance();
                migrateConfigVersion();
                addMissingDimensions();
                normalizeAllowedDimensions();
                save();
                CobbleSafari.LOGGER.info("Dungeon portal config loaded from {}", CONFIG_PATH);
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("Failed to load dungeon portal config, using defaults", e);
                INSTANCE = new PortalSpawnConfig();
            }
        } else {
            INSTANCE = new PortalSpawnConfig();
            migrateConfigVersion();
            save();
        }
        rebuildBannedBiomes();
    }

    /** Parses {@link #bannedBiomes} once into id and tag caches; unreadable entries are skipped. */
    private static void rebuildBannedBiomes() {
        Set<ResourceKey<Biome>> ids = new HashSet<>();
        List<TagKey<Biome>> tags = new ArrayList<>();
        List<String> raw = INSTANCE == null || INSTANCE.bannedBiomes == null ? List.of() : INSTANCE.bannedBiomes;
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String trimmed = entry.trim();
            boolean isTag = trimmed.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(isTag ? trimmed.substring(1) : trimmed);
            if (id == null) {
                CobbleSafari.LOGGER.warn("Ignoring invalid bannedBiomes entry: {}", trimmed);
                continue;
            }
            if (isTag) {
                tags.add(TagKey.create(Registries.BIOME, id));
            } else {
                ids.add(ResourceKey.create(Registries.BIOME, id));
            }
        }
        bannedBiomeIds = Set.copyOf(ids);
        bannedBiomeTags = List.copyOf(tags);
    }

    private static void clampSpawnChanceInInstance() {
        if (INSTANCE == null) return;
        if (INSTANCE.spawnChance == null) {
            INSTANCE.spawnChance = 1.0f;
            return;
        }
        float v = INSTANCE.spawnChance;
        if (v < 0f || v > 1f) {
            INSTANCE.spawnChance = Math.clamp(v, 0f, 1f);
        }
    }

    private static void addMissingDimensions() {
        if (INSTANCE == null) return;

        boolean modified = false;
        modified |= ensureDimensionConfig(DUNGEON_UNDERGROUND_ID, true, 1);
        modified |= ensureDimensionConfig(DUNGEON_JUMP_ID, true, 1);
        modified |= ensureDimensionConfig(DUNGEON_DISTORTION_ID, true, 1);
        // dungeon_ultraspace is deliberately absent: this file is the Hoopa portal wheel, and an
        // "enabled" entry for a sub-dungeon would be an invitation to turn it on (183, section 3.2).
        modified |= ensureDimensionConfig(DUNGEON_MEGALOPOLIS_ID, true, 1);
        modified |= ensureDimensionConfig(DUNGEON_PARIS_ID, true, 1);

        if (modified) {
            save();
        }
    }

    public static boolean ensureDimensionEntry(String dungeonId, boolean defaultEnabled, int defaultWeight) {
        PortalSpawnConfig inst = getInstance();
        Optional<DungeonDimensionEntry> existing = inst.dimensions.stream()
                .filter(entry -> entry.getDimensionId().equals(dungeonId))
                .findFirst();
        if (existing.isEmpty()) {
            inst.dimensions.add(new DungeonDimensionEntry(dungeonId, defaultEnabled, defaultWeight));
            CobbleSafari.LOGGER.info("Added dungeon dimension config: {} (enabled={}, weight={})",
                    dungeonId, defaultEnabled, defaultWeight);
            save();
            return true;
        }
        return false;
    }

    private static Set<String> collectDungeonDestinationDimensionIds() {
        Set<String> ids = new HashSet<>();
        if (INSTANCE == null || INSTANCE.dimensions == null) {
            return ids;
        }
        for (DungeonDimensionEntry entry : INSTANCE.dimensions) {
            String raw = entry.getDimensionId();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.contains(":")) {
                ids.add(trimmed);
            } else {
                ids.add("cobblesafari:" + trimmed);
            }
        }
        return ids;
    }

    private static boolean isUnsafeSpawnDimension(String trimmed, Set<String> dungeonDestIds) {
        if (trimmed.startsWith("cobblesafari:")) {
            return true;
        }
        if (dungeonDestIds.contains(trimmed)) {
            return true;
        }
        String normalized = trimmed.contains(":") ? trimmed : "cobblesafari:" + trimmed;
        return dungeonDestIds.contains(normalized);
    }

    private static void normalizeAllowedDimensions() {
        if (INSTANCE == null) {
            return;
        }
        List<String> raw = INSTANCE.allowedDimensions;
        if (raw == null) {
            INSTANCE.allowedDimensions = new ArrayList<>(DEFAULT_ALLOWED_DIMENSIONS);
            return;
        }
        Set<String> dungeonDestIds = collectDungeonDestinationDimensionIds();
        List<String> filtered = new ArrayList<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String trimmed = entry.trim();
            if (isUnsafeSpawnDimension(trimmed, dungeonDestIds)) {
                CobbleSafari.LOGGER.warn(
                        "The dimension \"{}\" was removed from the list of dimensions where the hoopa portals can spawn for safety reasons.",
                        trimmed);
                continue;
            }
            if (ResourceLocation.tryParse(trimmed) == null) {
                CobbleSafari.LOGGER.warn("Ignoring invalid allowedDimensions entry: {}", trimmed);
                continue;
            }
            if (!filtered.contains(trimmed)) {
                filtered.add(trimmed);
            }
        }
        INSTANCE.allowedDimensions = filtered.isEmpty()
                ? new ArrayList<>(DEFAULT_ALLOWED_DIMENSIONS)
                : filtered;
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

    public static boolean isNotificationEnabled() {
        Boolean v = getInstance().enableNotification;
        return v != null && v;
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

    public static float getSpawnChance() {
        Float c = getInstance().spawnChance;
        float v = c == null ? 1.0f : c;
        return Math.clamp(v, 0f, 1f);
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

    public static List<String> getAllowedDimensions() {
        return List.copyOf(getInstance().allowedDimensions);
    }

    public static boolean isSpawnDimensionAllowed(ResourceKey<Level> dimension) {
        return getAllowedDimensions().contains(dimension.location().toString());
    }

    public static List<String> getBannedBlocks() {
        List<String> list = getInstance().bannedBlocks;
        return list == null ? List.of() : List.copyOf(list);
    }

    /** Whether natural portals are banned from this biome (by id or tag, action plan 188, P3). */
    public static boolean isBiomeBanned(Holder<Biome> biome) {
        getInstance();
        for (TagKey<Biome> tag : bannedBiomeTags) {
            if (biome.is(tag)) {
                return true;
            }
        }
        return biome.unwrapKey().map(bannedBiomeIds::contains).orElse(false);
    }

    public static int getRepellentRadius() {
        return Math.clamp(getInstance().repellentRadius, 0, MAX_REPELLENT_RADIUS);
    }

    /** Returns the loaded singleton, loading or creating defaults as needed. Never null. */
    private static PortalSpawnConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        if (INSTANCE == null) {
            INSTANCE = new PortalSpawnConfig();
        }
        return INSTANCE;
    }
}
