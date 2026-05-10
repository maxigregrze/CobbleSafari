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

    public MiscConfig() {
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
}
