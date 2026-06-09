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

/**
 * Client-local HUD preferences ({@code hud_config.json}, plan 118 §11). Read and written on the
 * player's own machine so HUD preferences follow them across worlds and servers.
 * {@code objectivesForceOpen} is the default force-open state applied on client start and after
 * {@code /cobblesafari refresh}; the in-game keybind toggles a separate session flag (see
 * {@link maxigregrze.cobblesafari.client.objectives.ObjectivesHudController}).
 */
public class HudConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("hud_config.json");

    private static HudConfig INSTANCE;

    private boolean displayTimerBackground = true;
    private boolean objectivesForceOpen = false;
    private int objectivesAutoHide = 15;
    private int objectivesInactivityShows = 15;
    /** Open↔closed slide duration, expressed in **ticks** (plan 118 §9.2). */
    private int objectivesAnimationTime = 10;
    private boolean objectivesUsePercentage = false;
    private float objectivesPlacementPercentage = 0.5f;

    public HudConfig() {
        // Required no-arg constructor for GSON deserialization.
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                HudConfig parsed = GSON.fromJson(reader, HudConfig.class);
                INSTANCE = parsed != null ? parsed : new HudConfig();
            } catch (IOException | RuntimeException e) {
                CobbleSafari.LOGGER.error("CobbleSafari >> Failed to read hud_config.json; using defaults", e);
                INSTANCE = new HudConfig();
            }
        } else {
            INSTANCE = new HudConfig();
        }
        INSTANCE.clamp();
        save();
    }

    private void clamp() {
        objectivesAutoHide = Math.max(0, objectivesAutoHide);
        objectivesInactivityShows = Math.max(0, objectivesInactivityShows);
        objectivesAnimationTime = Math.max(1, objectivesAnimationTime);
        objectivesPlacementPercentage = Math.max(0.0f, Math.min(1.0f, objectivesPlacementPercentage));
    }

    /** Animation duration in ticks (the config value is already in ticks). */

    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new HudConfig();
        }
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to write hud_config.json", e);
        }
    }

    private static HudConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new HudConfig();
        }
        return INSTANCE;
    }

    public static boolean isDisplayTimerBackground() {
        return get().displayTimerBackground;
    }

    /** Default force-open state from {@code hud_config.json} (not the live session toggle). */
    public static boolean isObjectivesForceOpen() {
        return get().objectivesForceOpen;
    }

    public static int getObjectivesAutoHideTicks() {
        return get().objectivesAutoHide * 20;
    }

    public static int getObjectivesInactivityShowsTicks() {
        return get().objectivesInactivityShows * 20;
    }

    public static int getObjectivesAnimationTicks() {
        return Math.max(1, get().objectivesAnimationTime);
    }

    public static boolean isObjectivesUsePercentage() {
        return get().objectivesUsePercentage;
    }

    public static float getObjectivesPlacementPercentage() {
        return get().objectivesPlacementPercentage;
    }
}
