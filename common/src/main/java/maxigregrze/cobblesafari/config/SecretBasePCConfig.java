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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SecretBasePCConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("secretbase_pc_config.json");

    private static SecretBasePCConfig INSTANCE;

    private List<TierEntry> tiers = defaultTiers();
    private Map<String, Integer> flagBattery = defaultFlagBattery();

    public SecretBasePCConfig() {
    }

    public static class TierEntry {
        public int tierId;
        public int maxBattery;
        public List<EffectEntry> effects = new ArrayList<>();
    }

    public static class EffectEntry {
        public int effectId;
        public boolean isUnlocked;
        public int costPer4Seconds;
    }

    private static List<TierEntry> defaultTiers() {
        List<TierEntry> list = new ArrayList<>();
        int[] maxBatteries = {100, 200, 500, 1000, 2000, 2000};
        int[][] costs = {
                {0, 2, -1, -1, -1},
                {0, 2, 3, -1, -1},
                {0, 2, 3, 10, -1},
                {0, 1, 2, 10, 25},
                {0, 1, 2, 5, 10},
                {0, 0, 0, 0, 0}
        };
        for (int t = 0; t < 6; t++) {
            TierEntry tier = new TierEntry();
            tier.tierId = t;
            tier.maxBattery = maxBatteries[t];
            tier.effects = new ArrayList<>();
            for (int e = 0; e < 5; e++) {
                EffectEntry eff = new EffectEntry();
                eff.effectId = e;
                eff.costPer4Seconds = costs[t][e];
                eff.isUnlocked = costs[t][e] >= 0;
                tier.effects.add(eff);
            }
            list.add(tier);
        }
        return list;
    }

    private static Map<String, Integer> defaultFlagBattery() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("regular", 10);
        map.put("bronze", 20);
        map.put("silver", 50);
        map.put("gold", 100);
        map.put("platinum", 200);
        return map;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(reader, SecretBasePCConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new SecretBasePCConfig();
                }
                validateAndFixConfig();
                CobbleSafari.LOGGER.info("Secret base PC config loaded from {}", CONFIG_PATH);
                save();
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("Failed to load secret base PC config, using defaults", e);
                INSTANCE = new SecretBasePCConfig();
            }
        } else {
            INSTANCE = new SecretBasePCConfig();
            save();
        }
    }

    private static void validateAndFixConfig() {
        if (INSTANCE == null) return;
        if (INSTANCE.tiers == null) INSTANCE.tiers = defaultTiers();
        if (INSTANCE.flagBattery == null) INSTANCE.flagBattery = defaultFlagBattery();
        for (TierEntry tier : INSTANCE.tiers) {
            if (tier.maxBattery < 0) tier.maxBattery = 100;
            if (tier.effects == null) tier.effects = new ArrayList<>();
        }
    }

    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new SecretBasePCConfig();
        }
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to create config directory", e);
        }
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
            CobbleSafari.LOGGER.info("Secret base PC config saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to save secret base PC config", e);
        }
    }

    public static int getMaxBattery(int rank) {
        if (INSTANCE == null) return 100;
        int idx = Math.max(0, Math.min(rank, INSTANCE.tiers.size() - 1));
        TierEntry tier = INSTANCE.tiers.get(idx);
        return Math.max(0, tier.maxBattery);
    }

    public static int getEffectCost(int rank, int effect) {
        if (INSTANCE == null) return -1;
        int r = Math.max(0, Math.min(rank, INSTANCE.tiers.size() - 1));
        TierEntry tier = INSTANCE.tiers.get(r);
        if (tier.effects == null) return -1;
        for (EffectEntry e : tier.effects) {
            if (e != null && e.effectId == effect) {
                if (!e.isUnlocked || e.costPer4Seconds < 0) return -1;
                return e.costPer4Seconds;
            }
        }
        return -1;
    }

    public static boolean isEffectLocked(int rank, int effect) {
        return getEffectCost(rank, effect) == -1;
    }

    public static int getFlagBatteryValue(String flagKey) {
        if (INSTANCE == null || INSTANCE.flagBattery == null) return 0;
        Integer v = INSTANCE.flagBattery.get(flagKey == null ? "" : flagKey.toLowerCase());
        return v == null ? 0 : Math.max(0, v);
    }
}
