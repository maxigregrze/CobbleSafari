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
import java.util.Set;

/**
 * Server configuration for the Wonder Trade pool ({@code wondertrade_settings.json}).
 */
public class WonderTradeSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("wondertrade_settings.json");

    private static WonderTradeSettings INSTANCE;

    /** Default pools the onboarding owns; it rewrites only these (action plan 198, section 8.4). */
    private static final Set<String> ONBOARDING_POOL_IDS = Set.of(
            "common", "common_vanilla", "legendaries", "legendaries_vanilla", "mythical", "ultrabeast", "paradox");
    /** The managed pools always weigh this much together. */
    private static final int ONBOARDING_POOL_TOTAL = 100;

    /** @deprecated legacy field kept only to migrate the value to {@code misc_config.json}. */
    @Deprecated
    private int resetHour = 0;
    private int dailyTrades = 3;
    private boolean doPokemonExpire = true;
    private int pokemonBestBefore = 7;
    private boolean autoFill = true;
    private int autoFillAmount = 50;
    private List<WeightedPoolEntry> autoFillFromPools = new ArrayList<>();
    private int minLevel = 5;
    private int maxLevel = 55;
    private int minIV = 20;
    private int maxIV = 31;
    private double perfectChance = 0.01;
    private double ribbonChance = 0.01;
    private List<String> bannedSpecies = new ArrayList<>();
    private List<String> bannedHeldItems = new ArrayList<>();

    public static final class WeightedPoolEntry {
        public String groupId = "";
        public int weight = 1;
    }

    public WonderTradeSettings() {
        // Required no-arg constructor for GSON deserialization.
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                WonderTradeSettings parsed = GSON.fromJson(reader, WonderTradeSettings.class);
                if (parsed == null) {
                    INSTANCE = new WonderTradeSettings();
                } else {
                    if (parsed.autoFillFromPools == null) {
                        parsed.autoFillFromPools = new ArrayList<>();
                    }
                    if (parsed.bannedSpecies == null) {
                        parsed.bannedSpecies = new ArrayList<>();
                    }
                    if (parsed.bannedHeldItems == null) {
                        parsed.bannedHeldItems = new ArrayList<>();
                    }
                    INSTANCE = parsed;
                }
                INSTANCE.ensureDefaults();
                INSTANCE.validateAndFix();
                save();
                CobbleSafari.LOGGER.info("CobbleSafari >> wondertrade_settings.json loaded from {}", CONFIG_PATH);
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("CobbleSafari >> Failed to read wondertrade_settings.json", e);
                INSTANCE = new WonderTradeSettings();
                INSTANCE.ensureDefaults();
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("CobbleSafari >> Failed to parse wondertrade_settings.json", e);
                INSTANCE = new WonderTradeSettings();
                INSTANCE.ensureDefaults();
            }
        } else {
            try {
                Files.createDirectories(CONFIG_DIR);
            } catch (IOException ignored) {
                // Directory creation is best-effort; save() below will surface any real error.
            }
            INSTANCE = new WonderTradeSettings();
            INSTANCE.ensureDefaults();
            INSTANCE.validateAndFix();
            save();
            CobbleSafari.LOGGER.info("CobbleSafari >> Created default wondertrade_settings.json at {}", CONFIG_PATH);
        }
        
        if (INSTANCE != null) {
            maxigregrze.cobblesafari.config.MiscConfig.migrateDailyResetHourIfUnset(INSTANCE.resetHour);
        }
    }

    private void ensureDefaults() {
        if (autoFillFromPools.isEmpty()) {
            // Vanilla-only pools by design: a fresh install without a species sidemod must never
            // roll one of the 174 Pokemon Cobblemon ships data for but has no model for.
            autoFillFromPools.add(pool("common_vanilla", 98));
            autoFillFromPools.add(pool("legendaries_vanilla", 2));
        }
    }

    private static WeightedPoolEntry pool(String id, int w) {
        WeightedPoolEntry e = new WeightedPoolEntry();
        e.groupId = id;
        e.weight = w;
        return e;
    }

    /**
     * Rewrites the onboarding-managed default pools; any other entry is kept, after them, with its
     * weight. Events and pool groups are never touched (action plan 198, section 8.4).
     */
    public void applyOnboardingPools(MiscConfig.OnboardingState state) {
        List<WeightedPoolEntry> pools = new ArrayList<>();
        if (state.legendariesAndMythicals) {
            pools.add(pool("legendaries", 2));
            pools.add(pool("mythical", 1));
        } else {
            pools.add(pool("legendaries_vanilla", 2));
        }
        if (state.ultraBeasts) {
            pools.add(pool("ultrabeast", 1));
        }
        if (state.paradox) {
            pools.add(pool("paradox", 1));
        }
        int others = pools.stream().mapToInt(e -> e.weight).sum();
        pools.add(0, pool(state.missingRegular ? "common" : "common_vanilla", ONBOARDING_POOL_TOTAL - others));
        for (WeightedPoolEntry e : autoFillFromPools) {
            if (e != null && !ONBOARDING_POOL_IDS.contains(e.groupId)) {
                pools.add(e);
            }
        }
        autoFillFromPools = pools;
    }

    private void validateAndFix() {
        resetHour = Math.max(0, Math.min(23, resetHour));
        pokemonBestBefore = Math.max(1, Math.min(31, pokemonBestBefore));
        autoFillAmount = Math.max(0, autoFillAmount);
        minLevel = Math.max(1, minLevel);
        maxLevel = Math.max(1, maxLevel);
        if (minLevel > maxLevel) {
            int t = minLevel;
            minLevel = maxLevel;
            maxLevel = t;
        }
        minIV = Math.max(0, Math.min(31, minIV));
        maxIV = Math.max(0, Math.min(31, maxIV));
        if (minIV > maxIV) {
            int t = minIV;
            minIV = maxIV;
            maxIV = t;
        }
        perfectChance = Math.max(0.0, Math.min(1.0, perfectChance));
        ribbonChance = Math.max(0.0, Math.min(1.0, ribbonChance));
        for (WeightedPoolEntry e : autoFillFromPools) {
            if (e.weight < 0) e.weight = 0;
        }
        normalizeBanlist(bannedSpecies);
        normalizeBanlist(bannedHeldItems);
    }

    private static void normalizeBanlist(List<String> list) {
        if (list == null) {
            return;
        }
        list.replaceAll(s -> s == null ? "" : s.trim().toLowerCase(Locale.ROOT));
        list.removeIf(String::isEmpty);
    }

    public static void save() {
        if (INSTANCE == null) return;
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to write wondertrade_settings.json", e);
        }
    }

    public static WonderTradeSettings get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public int getDailyTrades() {
        return dailyTrades;
    }

    public boolean isDoPokemonExpire() {
        return doPokemonExpire;
    }

    public int getPokemonBestBefore() {
        return pokemonBestBefore;
    }

    public boolean isAutoFill() {
        return autoFill;
    }

    public int getAutoFillAmount() {
        return autoFillAmount;
    }

    public List<WeightedPoolEntry> getAutoFillFromPools() {
        return autoFillFromPools;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getMinIV() {
        return minIV;
    }

    public int getMaxIV() {
        return maxIV;
    }

    public double getPerfectChance() {
        return perfectChance;
    }

    public double getRibbonChance() {
        return ribbonChance;
    }

    public boolean isUnlimitedDailyTrades() {
        return dailyTrades < 0;
    }

    public boolean isSpeciesBanned(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        String n = id.toLowerCase(Locale.ROOT);
        for (String b : bannedSpecies) {
            if (n.equals(b) || n.endsWith(":" + b)) {
                return true;
            }
        }
        return false;
    }

    public boolean isHeldItemBanned(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        return bannedHeldItems.contains(id.toLowerCase(Locale.ROOT));
    }
}
