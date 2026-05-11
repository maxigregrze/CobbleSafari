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

    public MiscConfig() {
    }

    /**
     * Schéma JSON sous la clé {@code lostItem} dans {@code misc_config.json}.
     */
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
        public String poolBerryId = "cobblesafari:lostitem_berry";
        public String poolCandyId = "cobblesafari:lostitem_candy";
        public String poolBallsId = "cobblesafari:lostitem_ball";
        public String poolTreasuresId = "cobblesafari:lostitem_treasure";
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
                CobbleSafari.LOGGER.info("CobbleSafari >> misc_config.json loaded successfully from {}", CONFIG_PATH);
                CobbleSafari.LOGGER.info("CobbleSafari >> Persisting misc_config.json after load (canonical schema on disk)");
                save();
            } catch (IOException e) {
                CobbleSafari.LOGGER.error(
                        "CobbleSafari >> Failed to read misc_config.json at {} (I/O error). Using in-memory defaults; the file on disk was not modified.",
                        CONFIG_PATH,
                        e);
                INSTANCE = new MiscConfig();
            } catch (Exception e) {
                CobbleSafari.LOGGER.error(
                        "CobbleSafari >> Failed to parse misc_config.json at {} (invalid JSON). Using in-memory defaults; the file on disk was not modified.",
                        CONFIG_PATH,
                        e);
                INSTANCE = new MiscConfig();
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

    public void ensureAuspiciousPokeballDefaults() {
        if (this.auspiciousPokeball == null) {
            this.auspiciousPokeball = new AuspiciousPokeballDefaults();
        }
        AuspiciousPokeballDefaults ap = this.auspiciousPokeball;
        if (ap.poolBerryId == null) {
            ap.poolBerryId = new AuspiciousPokeballDefaults().poolBerryId;
        }
        if (ap.poolCandyId == null) {
            ap.poolCandyId = new AuspiciousPokeballDefaults().poolCandyId;
        }
        if (ap.poolBallsId == null) {
            ap.poolBallsId = new AuspiciousPokeballDefaults().poolBallsId;
        }
        if (ap.poolTreasuresId == null) {
            ap.poolTreasuresId = new AuspiciousPokeballDefaults().poolTreasuresId;
        }
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
}
