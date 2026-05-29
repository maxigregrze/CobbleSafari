package maxigregrze.cobblesafari.config;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiscConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir()
            .resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("misc_config.json");

    private static MiscConfig INSTANCE;

    private double balloonSpawnMultiplier = 1.0;
    private int balloonCheckIntervalTicks = 200;
    private int balloonSpawnRadius = 32;
    private int balloonHeightAboveGround = 32;
    private int balloonLifetimeTicks = 6000;
    private boolean balloonEnabled = true;

    /**
     * Valeurs par défaut pour un bloc « lost item » nouvellement placé (voir {@code misc_config.json}, clé {@code lostItem}).
     */
    private LostItemDefaults lostItem = new LostItemDefaults();

    /** Valeurs par défaut pour un bloc « pokéball auspicieuse » nouvellement placé (clé {@code auspiciousPokeball}). */
    private AuspiciousPokeballDefaults auspiciousPokeball = new AuspiciousPokeballDefaults();

    /** Défauts au placement pour {@code cobblesafari:auspiciouspokeball_small} (clé {@code auspiciousPokeballSmall}). */
    private AuspiciousPokeballDefaults auspiciousPokeballSmall = new AuspiciousPokeballDefaults();

    /** Défauts au placement pour {@code cobblesafari:auspiciouspokeball_gold} (clé {@code auspiciousPokeballGold}). */
    private AuspiciousPokeballDefaults auspiciousPokeballGold = new AuspiciousPokeballDefaults();

    /** Nombre maximal d'instances Union Room (structures) dans {@code cobblesafari:unionroom}. */
    private int unionRoomMaxInstances = 10;
    /** Nombre maximal de guests par session Union Room (hôte non compté), type {@code default}. */
    private int unionRoomMaxGuestsPerSession = 6;
    /** Nombre maximal d'instances Union Plaza concurrentes. */
    private int unionRoomPlazaMaxInstances = 2;
    /** Nombre maximal de guests par session Union Plaza (hôte non compté) — 4× la room (6) par défaut. */
    private int unionRoomPlazaMaxGuestsPerSession = 24;
    /** Types de salon (clé → limites). Absent du JSON ⇒ reconstruit depuis les champs ci‑dessus. */
    private Map<String, RoomTypeConfig> unionRoomTypes = null;
    /** Dimensions depuis lesquelles un joueur ne peut pas entrer dans l'Union Room (IDs complets, ex. {@code cobblesafari:domedimension}). */
    private List<String> unionRoomBannedDimensions = new ArrayList<>();

    private static Map<String, RoomTypeConfig> roomTypeRuntime = new HashMap<>();

    public MiscConfig() {
    }

    public static final class LostItemDefaults {
        public String poolBerryId = "cobblesafari:lostitem_berry";
        public String poolCandyId = "cobblesafari:lostitem_candy";
        public String poolBallsId = "cobblesafari:lostitem_ball";
        public String poolTreasuresId = "cobblesafari:lostitem_treasure";
        public String lostItemLootTableId = "cobblesafari:lostitem";
        public String lootItemId = "cobblemon:poke_ball";
        public int minRoll = 1;
        public int maxRoll = 3;
        public int mode = 0;
    }

    public static final class AuspiciousPokeballDefaults {
        public String poolBerryId = "cobblesafari:auspiciouspokeball/berry";
        public String poolCandyId = "cobblesafari:auspiciouspokeball/candy";
        public String poolBallsId = "cobblesafari:auspiciouspokeball/ball";
        public String poolTreasuresId = "cobblesafari:auspiciouspokeball/treasure";
        public int minRoll = 1;
        public int maxRoll = 3;
    }

    public static final class AuspiciousPokeballSmallDefaults {
        public String poolBerryId = "cobblesafari:auspiciouspokeball/berry_small";
        public String poolCandyId = "cobblesafari:auspiciouspokeball/candy_small";
        public String poolBallsId = "cobblesafari:auspiciouspokeball/ball_small";
        public String poolTreasuresId = "cobblesafari:auspiciouspokeball/treasure_small";
        public int minRoll = 1;
        public int maxRoll = 3;
    }

    public static final class RoomTypeConfig {
        public int maxInstances;
        public int maxGuestsPerSession;

        public RoomTypeConfig() {}

        public RoomTypeConfig(int maxInstances, int maxGuestsPerSession) {
            this.maxInstances = maxInstances;
            this.maxGuestsPerSession = maxGuestsPerSession;
        }
    }

    public static final class AuspiciousPokeballGoldenDefaults {
        public String poolBerryId = "cobblesafari:auspiciouspokeball/berry_gold";
        public String poolCandyId = "cobblesafari:auspiciouspokeball/candy_gold";
        public String poolBallsId = "cobblesafari:auspiciouspokeball/ball_gold";
        public String poolTreasuresId = "cobblesafari:auspiciouspokeball/treasure_gold";
        public int minRoll = 1;
        public int maxRoll = 3;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(reader, MiscConfig.class);
                if (INSTANCE == null) {
                    CobbleSafari.LOGGER.warn(
                            "CobbleSafari >> misc_config.json at {} deserialized to null; using defaults",
                            CONFIG_PATH);
                    INSTANCE = new MiscConfig();
                }
                INSTANCE.ensureLostItemDefaults();
                INSTANCE.ensureAuspiciousPokeballDefaults();
                INSTANCE.ensureAuspiciousPokeballSmallDefaults();
                INSTANCE.ensureAuspiciousPokeballGoldDefaults();
                INSTANCE.ensureUnionRoomDefaults();
                CobbleSafari.LOGGER.info("CobbleSafari >> misc_config.json loaded successfully from {}", CONFIG_PATH);
                CobbleSafari.LOGGER.info("CobbleSafari >> Persisting misc_config.json after load (canonical schema on disk)");
                save();
            } catch (IOException e) {
                CobbleSafari.LOGGER.error(
                        "CobbleSafari >> Failed to read misc_config.json at {} (I/O error). Using in-memory defaults; the file on disk was not modified.",
                        CONFIG_PATH,
                        e);
                INSTANCE = new MiscConfig();
                INSTANCE.ensureUnionRoomDefaults();
            } catch (Exception e) {
                CobbleSafari.LOGGER.error(
                        "CobbleSafari >> Failed to parse misc_config.json at {} (invalid JSON). Using in-memory defaults; the file on disk was not modified.",
                        CONFIG_PATH,
                        e);
                INSTANCE = new MiscConfig();
                INSTANCE.ensureUnionRoomDefaults();
            }
        } else {
            CobbleSafari.LOGGER.info(
                    "CobbleSafari >> misc_config.json not found at {}, creating default file",
                    CONFIG_PATH);
            INSTANCE = new MiscConfig();
            CobbleSafari.LOGGER.info("CobbleSafari >> Persisting misc_config.json after load (first-time default file)");
            save();
        }
    }

    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new MiscConfig();
        }
        INSTANCE.ensureLostItemDefaults();
        INSTANCE.ensureAuspiciousPokeballDefaults();
        INSTANCE.ensureAuspiciousPokeballSmallDefaults();
        INSTANCE.ensureAuspiciousPokeballGoldDefaults();
        INSTANCE.ensureUnionRoomDefaults();
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to create config directory", e);
        }
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
            CobbleSafari.LOGGER.info("CobbleSafari >> misc_config.json written to {}", CONFIG_PATH);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to save misc config", e);
        }
    }

    public static double getBalloonSpawnMultiplier() {
        if (INSTANCE == null) return 1.0;
        return Math.max(0.0, INSTANCE.balloonSpawnMultiplier);
    }

    public static int getBalloonCheckIntervalTicks() {
        if (INSTANCE == null) return 200;
        return Math.max(1, INSTANCE.balloonCheckIntervalTicks);
    }

    public static int getBalloonSpawnRadius() {
        if (INSTANCE == null) return 32;
        return Math.max(1, INSTANCE.balloonSpawnRadius);
    }

    public static int getBalloonHeightAboveGround() {
        if (INSTANCE == null) return 32;
        return Math.max(1, INSTANCE.balloonHeightAboveGround);
    }

    public static int getBalloonLifetimeTicks() {
        if (INSTANCE == null) return 6000;
        return Math.max(20, INSTANCE.balloonLifetimeTicks);
    }

    public static boolean isBalloonEnabled() {
        if (INSTANCE == null) return true;
        return INSTANCE.balloonEnabled;
    }

    public void ensureLostItemDefaults() {
        if (this.lostItem == null) {
            this.lostItem = new LostItemDefaults();
        }
        LostItemDefaults li = this.lostItem;
        if (li.poolBerryId == null) {
            li.poolBerryId = new LostItemDefaults().poolBerryId;
        }
        if (li.poolCandyId == null) {
            li.poolCandyId = new LostItemDefaults().poolCandyId;
        }
        if (li.poolBallsId == null) {
            li.poolBallsId = new LostItemDefaults().poolBallsId;
        }
        if (li.poolTreasuresId == null) {
            li.poolTreasuresId = new LostItemDefaults().poolTreasuresId;
        }
        if (li.lostItemLootTableId == null) {
            li.lostItemLootTableId = new LostItemDefaults().lostItemLootTableId;
        }
        if (li.lootItemId == null) {
            li.lootItemId = "";
        }
        li.mode = Math.max(0, Math.min(2, li.mode));
    }

    private static void fillNullAuspiciousPools(AuspiciousPokeballDefaults ap, AuspiciousPokeballDefaults template) {
        if (ap.poolBerryId == null) {
            ap.poolBerryId = template.poolBerryId;
        }
        if (ap.poolCandyId == null) {
            ap.poolCandyId = template.poolCandyId;
        }
        if (ap.poolBallsId == null) {
            ap.poolBallsId = template.poolBallsId;
        }
        if (ap.poolTreasuresId == null) {
            ap.poolTreasuresId = template.poolTreasuresId;
        }
    }

    public void ensureAuspiciousPokeballDefaults() {
        if (this.auspiciousPokeball == null) {
            this.auspiciousPokeball = new AuspiciousPokeballDefaults();
        }
        fillNullAuspiciousPools(this.auspiciousPokeball, new AuspiciousPokeballDefaults());
    }

    public void ensureAuspiciousPokeballSmallDefaults() {
        if (this.auspiciousPokeballSmall == null) {
            this.auspiciousPokeballSmall = new AuspiciousPokeballDefaults();
        }
        fillNullAuspiciousPools(this.auspiciousPokeballSmall, new AuspiciousPokeballDefaults());
    }

    public void ensureAuspiciousPokeballGoldDefaults() {
        if (this.auspiciousPokeballGold == null) {
            this.auspiciousPokeballGold = new AuspiciousPokeballDefaults();
        }
        fillNullAuspiciousPools(this.auspiciousPokeballGold, new AuspiciousPokeballDefaults());
    }

    private static LostItemDefaults lostItemOrDefaults() {
        if (INSTANCE != null) {
            INSTANCE.ensureLostItemDefaults();
            return INSTANCE.lostItem;
        }
        return new LostItemDefaults();
    }

    private static AuspiciousPokeballDefaults auspiciousPokeballOrDefaults() {
        if (INSTANCE != null) {
            INSTANCE.ensureAuspiciousPokeballDefaults();
            return INSTANCE.auspiciousPokeball;
        }
        return new AuspiciousPokeballDefaults();
    }

    private static AuspiciousPokeballDefaults auspiciousPokeballSmallOrDefaults() {
        if (INSTANCE != null) {
            INSTANCE.ensureAuspiciousPokeballSmallDefaults();
            return INSTANCE.auspiciousPokeballSmall;
        }
        return new AuspiciousPokeballDefaults();
    }

    private static AuspiciousPokeballDefaults auspiciousPokeballGoldOrDefaults() {
        if (INSTANCE != null) {
            INSTANCE.ensureAuspiciousPokeballGoldDefaults();
            return INSTANCE.auspiciousPokeballGold;
        }
        return new AuspiciousPokeballDefaults();
    }

    private static String nonBlankOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    public static String getLostItemPoolBerryId() {
        LostItemDefaults d = lostItemOrDefaults();
        return nonBlankOrDefault(d.poolBerryId, new LostItemDefaults().poolBerryId);
    }

    public static String getLostItemPoolCandyId() {
        LostItemDefaults d = lostItemOrDefaults();
        return nonBlankOrDefault(d.poolCandyId, new LostItemDefaults().poolCandyId);
    }

    public static String getLostItemPoolBallsId() {
        LostItemDefaults d = lostItemOrDefaults();
        return nonBlankOrDefault(d.poolBallsId, new LostItemDefaults().poolBallsId);
    }

    public static String getLostItemPoolTreasuresId() {
        LostItemDefaults d = lostItemOrDefaults();
        return nonBlankOrDefault(d.poolTreasuresId, new LostItemDefaults().poolTreasuresId);
    }

    public static String getLostItemLootTableId() {
        LostItemDefaults d = lostItemOrDefaults();
        return nonBlankOrDefault(d.lostItemLootTableId, new LostItemDefaults().lostItemLootTableId);
    }

    public static String getLostItemLootItemId() {
        String s = lostItemOrDefaults().lootItemId;
        return s == null ? "" : s.trim();
    }

    public static int getLostItemMinRoll() {
        return Math.max(0, lostItemOrDefaults().minRoll);
    }

    public static int getLostItemMaxRoll() {
        return Math.max(0, lostItemOrDefaults().maxRoll);
    }

    public static int getLostItemMode() {
        return Math.max(0, Math.min(2, lostItemOrDefaults().mode));
    }

    public static String getAuspiciousPokeballPoolBerryId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballOrDefaults();
        return nonBlankOrDefault(d.poolBerryId, new AuspiciousPokeballDefaults().poolBerryId);
    }

    public static String getAuspiciousPokeballPoolCandyId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballOrDefaults();
        return nonBlankOrDefault(d.poolCandyId, new AuspiciousPokeballDefaults().poolCandyId);
    }

    public static String getAuspiciousPokeballPoolBallsId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballOrDefaults();
        return nonBlankOrDefault(d.poolBallsId, new AuspiciousPokeballDefaults().poolBallsId);
    }

    public static String getAuspiciousPokeballPoolTreasuresId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballOrDefaults();
        return nonBlankOrDefault(d.poolTreasuresId, new AuspiciousPokeballDefaults().poolTreasuresId);
    }

    public static int getAuspiciousPokeballMinRoll() {
        return Math.max(0, auspiciousPokeballOrDefaults().minRoll);
    }

    public static int getAuspiciousPokeballMaxRoll() {
        return Math.max(0, auspiciousPokeballOrDefaults().maxRoll);
    }

    public static String getAuspiciousPokeballSmallPoolBerryId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballSmallOrDefaults();
        return nonBlankOrDefault(d.poolBerryId, new AuspiciousPokeballDefaults().poolBerryId);
    }

    public static String getAuspiciousPokeballSmallPoolCandyId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballSmallOrDefaults();
        return nonBlankOrDefault(d.poolCandyId, new AuspiciousPokeballDefaults().poolCandyId);
    }

    public static String getAuspiciousPokeballSmallPoolBallsId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballSmallOrDefaults();
        return nonBlankOrDefault(d.poolBallsId, new AuspiciousPokeballDefaults().poolBallsId);
    }

    public static String getAuspiciousPokeballSmallPoolTreasuresId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballSmallOrDefaults();
        return nonBlankOrDefault(d.poolTreasuresId, new AuspiciousPokeballDefaults().poolTreasuresId);
    }

    public static int getAuspiciousPokeballSmallMinRoll() {
        return Math.max(0, auspiciousPokeballSmallOrDefaults().minRoll);
    }

    public static int getAuspiciousPokeballSmallMaxRoll() {
        return Math.max(0, auspiciousPokeballSmallOrDefaults().maxRoll);
    }

    public static String getAuspiciousPokeballGoldPoolBerryId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballGoldOrDefaults();
        return nonBlankOrDefault(d.poolBerryId, new AuspiciousPokeballDefaults().poolBerryId);
    }

    public static String getAuspiciousPokeballGoldPoolCandyId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballGoldOrDefaults();
        return nonBlankOrDefault(d.poolCandyId, new AuspiciousPokeballDefaults().poolCandyId);
    }

    public static String getAuspiciousPokeballGoldPoolBallsId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballGoldOrDefaults();
        return nonBlankOrDefault(d.poolBallsId, new AuspiciousPokeballDefaults().poolBallsId);
    }

    public static String getAuspiciousPokeballGoldPoolTreasuresId() {
        AuspiciousPokeballDefaults d = auspiciousPokeballGoldOrDefaults();
        return nonBlankOrDefault(d.poolTreasuresId, new AuspiciousPokeballDefaults().poolTreasuresId);
    }

    public static int getAuspiciousPokeballGoldMinRoll() {
        return Math.max(0, auspiciousPokeballGoldOrDefaults().minRoll);
    }

    public static int getAuspiciousPokeballGoldMaxRoll() {
        return Math.max(0, auspiciousPokeballGoldOrDefaults().maxRoll);
    }

    public void ensureUnionRoomDefaults() {
        if (this.unionRoomBannedDimensions == null) {
            this.unionRoomBannedDimensions = new ArrayList<>();
        }
        this.unionRoomMaxInstances = Math.max(1, Math.min(100, this.unionRoomMaxInstances));
        this.unionRoomMaxGuestsPerSession = Math.max(1, Math.min(100, this.unionRoomMaxGuestsPerSession));
        this.unionRoomPlazaMaxInstances = Math.max(1, Math.min(100, this.unionRoomPlazaMaxInstances));
        this.unionRoomPlazaMaxGuestsPerSession = Math.max(1, Math.min(100, this.unionRoomPlazaMaxGuestsPerSession));
        rebuildRoomTypeRuntime();
    }

    private void rebuildRoomTypeRuntime() {
        roomTypeRuntime = new HashMap<>();
        RoomTypeConfig defaultType =
                new RoomTypeConfig(this.unionRoomMaxInstances, this.unionRoomMaxGuestsPerSession);
        roomTypeRuntime.put("default", defaultType);
        roomTypeRuntime.put("room", defaultType);
        roomTypeRuntime.put("plaza",
                new RoomTypeConfig(this.unionRoomPlazaMaxInstances, this.unionRoomPlazaMaxGuestsPerSession));
        if (this.unionRoomTypes != null) {
            for (Map.Entry<String, RoomTypeConfig> e : this.unionRoomTypes.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) {
                    continue;
                }
                RoomTypeConfig c = e.getValue();
                c.maxInstances = Math.max(1, Math.min(100, c.maxInstances));
                c.maxGuestsPerSession = Math.max(1, Math.min(100, c.maxGuestsPerSession));
                roomTypeRuntime.put(e.getKey(), c);
            }
        }
        roomTypeRuntime.put("default", defaultType);
        roomTypeRuntime.put("room", defaultType);
    }

    public static RoomTypeConfig getRoomType(String key) {
        if (INSTANCE == null) {
            return new RoomTypeConfig(10, 6);
        }
        if (roomTypeRuntime.isEmpty()) {
            INSTANCE.ensureUnionRoomDefaults();
        }
        RoomTypeConfig def = roomTypeRuntime.get("default");
        if (def == null) {
            return new RoomTypeConfig(10, 6);
        }
        if (key == null) {
            return def;
        }
        return roomTypeRuntime.getOrDefault(key, def);
    }

    public static int getUnionRoomMaxInstances() {
        return getRoomType("default").maxInstances;
    }

    public static List<String> getUnionRoomBannedDimensions() {
        if (INSTANCE == null) {
            return List.of();
        }
        if (INSTANCE.unionRoomBannedDimensions == null) {
            return List.of();
        }
        return INSTANCE.unionRoomBannedDimensions;
    }
}
