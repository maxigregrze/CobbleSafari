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

public class IncubatorConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("incubator_config.json");

    private static IncubatorConfig INSTANCE;

    private float cobbreedingHatchSpeedMultiplier = 0.66f;
    private int defaultWildEggHatchTimeTicks = 14400;
    private int eggNestMinRefillTicks = 36000;
    private int eggNestMaxRefillTicks = 72000;

    public IncubatorConfig() {
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(reader, IncubatorConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new IncubatorConfig();
                }
                validateAndFixConfig();
                CobbleSafari.LOGGER.info("Incubator config loaded from {}", CONFIG_PATH);
                save();
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("Failed to load incubator config, using defaults", e);
                INSTANCE = new IncubatorConfig();
            }
        } else {
            INSTANCE = new IncubatorConfig();
            save();
        }
    }

    private static void validateAndFixConfig() {
        if (INSTANCE == null) return;
        if (INSTANCE.cobbreedingHatchSpeedMultiplier <= 0 || INSTANCE.cobbreedingHatchSpeedMultiplier > 1.0f) {
            INSTANCE.cobbreedingHatchSpeedMultiplier = 0.66f;
        }
        if (INSTANCE.defaultWildEggHatchTimeTicks <= 0) {
            INSTANCE.defaultWildEggHatchTimeTicks = 14400;
        }
        if (INSTANCE.eggNestMinRefillTicks < 20) {
            INSTANCE.eggNestMinRefillTicks = 36000;
        }
        if (INSTANCE.eggNestMaxRefillTicks < INSTANCE.eggNestMinRefillTicks) {
            INSTANCE.eggNestMaxRefillTicks = INSTANCE.eggNestMinRefillTicks;
        }
    }

    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new IncubatorConfig();
        }
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to create config directory", e);
        }
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
            CobbleSafari.LOGGER.info("Incubator config saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to save incubator config", e);
        }
    }

    public static float getCobbreedingHatchSpeedMultiplier() {
        if (INSTANCE == null) return 0.66f;
        return INSTANCE.cobbreedingHatchSpeedMultiplier;
    }

    public static int getDefaultWildEggHatchTimeTicks() {
        if (INSTANCE == null) return 14400;
        return INSTANCE.defaultWildEggHatchTimeTicks;
    }

    public static int getEggNestMinRefillTicks() {
        if (INSTANCE == null) return 36000;
        return INSTANCE.eggNestMinRefillTicks;
    }

    public static int getEggNestMaxRefillTicks() {
        if (INSTANCE == null) return 72000;
        return INSTANCE.eggNestMaxRefillTicks;
    }
}
