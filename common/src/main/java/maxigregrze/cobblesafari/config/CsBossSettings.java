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
 * Configuration serveur du Boss Battle System ({@code csboss_settings.json}).
 */
public class CsBossSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("csboss_settings.json");

    private static final String DEFAULT_COWARDICE_KEY = "death.attack.csboss_cowardice";

    private static CsBossSettings INSTANCE;

    private boolean uniqueLootCommunism = false;
    private int maximumConcurrentFights = 5;
    private int maximumFightDuration = 900;   // secondes — plafond dur global (pas de plancher)
    private int defaultPlayerRadius = 24;     // blocs
    private int defaultBlockRadius = 2;       // chunks
    private int maxConcurrentBulletsPerSession = 256;
    private int maxPlayerRadius = 64;         // plafond dur (blocs)
    private int maxBlockRadius = 8;           // plafond dur (chunks)
    private int arenaYTolerance = 16;         // blocs (capture participants + barre)
    private String deathReasonCowardice = DEFAULT_COWARDICE_KEY;
    private int balmDispenserRechargeSeconds = 30;
    private int balmBossDamagePercent = 5;    // % de la barre/timer du boss retiré par baume touché
    private boolean showFightingPokemons = true; // affiche les Pokémon des joueurs autour du boss (esthétique)

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
                // best-effort ; save() ci-dessous remontera toute erreur réelle.
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
