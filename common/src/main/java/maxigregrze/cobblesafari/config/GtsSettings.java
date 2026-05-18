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
import java.util.List;
import java.util.Locale;

/**
 * Configuration serveur pour le Global Trade Station ({@code gts_settings.json}).
 */
public class GtsSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("gts_settings.json");

    private static GtsSettings INSTANCE;

    private boolean tradeNotification = true;
    private boolean tradeReminder = true;
    private int pokemonBestBefore = 7;
    private List<String> bannedPokemons = new ArrayList<>();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                GtsSettings parsed = GSON.fromJson(reader, GtsSettings.class);
                if (parsed == null) {
                    INSTANCE = new GtsSettings();
                } else {
                    if (parsed.bannedPokemons == null) {
                        parsed.bannedPokemons = new ArrayList<>();
                    }
                    INSTANCE = parsed;
                }
                INSTANCE.validateAndFix();
                save();
                CobbleSafari.LOGGER.info("CobbleSafari >> gts_settings.json loaded from {}", CONFIG_PATH);
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("CobbleSafari >> Failed to read gts_settings.json", e);
                INSTANCE = new GtsSettings();
                INSTANCE.validateAndFix();
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("CobbleSafari >> Failed to parse gts_settings.json", e);
                INSTANCE = new GtsSettings();
                INSTANCE.validateAndFix();
            }
        } else {
            try {
                Files.createDirectories(CONFIG_DIR);
            } catch (IOException ignored) {}
            INSTANCE = new GtsSettings();
            INSTANCE.validateAndFix();
            save();
            CobbleSafari.LOGGER.info("CobbleSafari >> Created default gts_settings.json at {}", CONFIG_PATH);
        }
    }

    private void validateAndFix() {
        pokemonBestBefore = Math.max(1, Math.min(31, pokemonBestBefore));
        if (bannedPokemons == null) {
            bannedPokemons = new ArrayList<>();
        }
        bannedPokemons.replaceAll(s -> s == null ? "" : s.trim().toLowerCase(Locale.ROOT));
        bannedPokemons.removeIf(String::isEmpty);
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
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to write gts_settings.json", e);
        }
    }

    public static GtsSettings get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public boolean isTradeNotification() {
        return tradeNotification;
    }

    public boolean isTradeReminder() {
        return tradeReminder;
    }

    public int getPokemonBestBefore() {
        return pokemonBestBefore;
    }

    public List<String> getBannedPokemons() {
        return bannedPokemons;
    }

    public boolean isSpeciesBanned(String speciesPathOrIdLowercase) {
        if (speciesPathOrIdLowercase == null || speciesPathOrIdLowercase.isEmpty()) {
            return false;
        }
        String n = speciesPathOrIdLowercase.toLowerCase(Locale.ROOT);
        for (String b : bannedPokemons) {
            if (n.equals(b) || n.endsWith(":" + b)) {
                return true;
            }
        }
        return false;
    }
}
