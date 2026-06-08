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
 * Server configuration for the Boss Battle System ({@code csboss_settings.json}).
 */
public class CsBossSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("csboss_settings.json");

    private static final String DEFAULT_COWARDICE_KEY = "death.attack.csboss_cowardice";

    private static CsBossSettings INSTANCE;

    private boolean uniqueLootCommunism = false;
    private int maximumConcurrentFights = 5;
    private int maximumFightDuration = 900;   // seconds — hard global cap (no floor)
    private int defaultPlayerRadius = 24;     // blocks
    private int defaultBlockRadius = 2;       // chunks
    private int maxConcurrentBulletsPerSession = 256;
    private int maxPlayerRadius = 64;         // hard cap (blocks)
    private int maxBlockRadius = 8;           // hard cap (chunks)
    private int arenaYTolerance = 16;         // blocks (participant capture + bar)
    private String deathReasonCowardice = DEFAULT_COWARDICE_KEY;
    private int balmDispenserRechargeSeconds = 30;
    private int balmBossDamagePercent = 5;    // % of boss bar/timer removed per balm hit
    private boolean showFightingPokemons = true; // show players' Pokémon around the boss (cosmetic)

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                CsBossSettings parsed = GSON.fromJson(reader, CsBossSettings.class);
                INSTANCE = parsed == null ? new CsBossSettings() : parsed;
                INSTANCE.validateAndFix();
                save();
                CobbleSafari.LOGGER.info("CobbleSafari >> csboss_settings.json loaded from {}", CONFIG_PATH);
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("CobbleSafari >> Failed to read csboss_settings.json", e);
                INSTANCE = new CsBossSettings();
                INSTANCE.validateAndFix();
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("CobbleSafari >> Failed to parse csboss_settings.json", e);
                INSTANCE = new CsBossSettings();
                INSTANCE.validateAndFix();
            }
        } else {
            try {
                Files.createDirectories(CONFIG_DIR);
            } catch (IOException ignored) {
                // best-effort; save() below will surface any real error.
            }
            INSTANCE = new CsBossSettings();
            INSTANCE.validateAndFix();
            save();
            CobbleSafari.LOGGER.info("CobbleSafari >> Created default csboss_settings.json at {}", CONFIG_PATH);
        }
    }

    private void validateAndFix() {
        maximumFightDuration = Math.max(1, maximumFightDuration);
        maximumConcurrentFights = Math.max(0, maximumConcurrentFights);
        defaultPlayerRadius = Math.max(1, defaultPlayerRadius);
        defaultBlockRadius = Math.max(0, defaultBlockRadius);
        maxConcurrentBulletsPerSession = Math.max(1, maxConcurrentBulletsPerSession);
        maxPlayerRadius = Math.max(defaultPlayerRadius, maxPlayerRadius);
        maxBlockRadius = Math.max(defaultBlockRadius, maxBlockRadius);
        arenaYTolerance = Math.max(1, arenaYTolerance);
        if (deathReasonCowardice == null || deathReasonCowardice.isBlank()) {
            deathReasonCowardice = DEFAULT_COWARDICE_KEY;
        }
        balmDispenserRechargeSeconds = Math.max(1, balmDispenserRechargeSeconds);
        balmBossDamagePercent = Math.max(0, Math.min(100, balmBossDamagePercent));
    }

    public static void save() {
        if (INSTANCE == null) {
            return;
        }
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to write csboss_settings.json", e);
        }
    }

    public static CsBossSettings get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public boolean isUniqueLootCommunism() {
        return uniqueLootCommunism;
    }

    public int getMaximumConcurrentFights() {
        return maximumConcurrentFights;
    }

    public int getMaximumFightDuration() {
        return maximumFightDuration;
    }

    public int getDefaultPlayerRadius() {
        return defaultPlayerRadius;
    }

    public int getDefaultBlockRadius() {
        return defaultBlockRadius;
    }

    public int getMaxConcurrentBulletsPerSession() {
        return maxConcurrentBulletsPerSession;
    }

    public int getMaxPlayerRadius() {
        return maxPlayerRadius;
    }

    public int getMaxBlockRadius() {
        return maxBlockRadius;
    }

    public int getArenaYTolerance() {
        return arenaYTolerance;
    }

    public String getDeathReasonCowardice() {
        return deathReasonCowardice;
    }

    public int getBalmDispenserRechargeSeconds() {
        return balmDispenserRechargeSeconds;
    }

    public int getBalmBossDamagePercent() {
        return balmBossDamagePercent;
    }

    public boolean isShowFightingPokemons() {
        return showFightingPokemons;
    }
}
