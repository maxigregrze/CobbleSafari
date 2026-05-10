package maxigregrze.cobblesafari.config;

import com.cobblemon.mod.common.item.PokeBallItem;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RandomizerItemsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("randomizer_items_config.json");

    private static RandomizerItemsConfig INSTANCE;

    private List<WeightedPokeballEntry> availablePokeBalls = new ArrayList<>(List.of(
        new WeightedPokeballEntry("cobblemon:poke_ball", 1),
        new WeightedPokeballEntry("cobblemon:great_ball", 1),
        new WeightedPokeballEntry("cobblemon:ultra_ball", 1),
        new WeightedPokeballEntry("cobblemon:master_ball", 1),
        new WeightedPokeballEntry("cobblemon:premier_ball", 1),
        new WeightedPokeballEntry("cobblemon:cherish_ball", 1),
        new WeightedPokeballEntry("cobblemon:dive_ball", 1),
        new WeightedPokeballEntry("cobblemon:dusk_ball", 1),
        new WeightedPokeballEntry("cobblemon:fast_ball", 1),
        new WeightedPokeballEntry("cobblemon:friend_ball", 1),
        new WeightedPokeballEntry("cobblemon:heal_ball", 1),
        new WeightedPokeballEntry("cobblemon:heavy_ball", 1),
        new WeightedPokeballEntry("cobblemon:level_ball", 1),
        new WeightedPokeballEntry("cobblemon:love_ball", 1),
        new WeightedPokeballEntry("cobblemon:lure_ball", 1),
        new WeightedPokeballEntry("cobblemon:luxury_ball", 1),
        new WeightedPokeballEntry("cobblemon:moon_ball", 1),
        new WeightedPokeballEntry("cobblemon:nest_ball", 1),
        new WeightedPokeballEntry("cobblemon:net_ball", 1),
        new WeightedPokeballEntry("cobblemon:park_ball", 1),
        new WeightedPokeballEntry("cobblemon:quick_ball", 1),
        new WeightedPokeballEntry("cobblemon:repeat_ball", 1),
        new WeightedPokeballEntry("cobblemon:sport_ball", 1),
        new WeightedPokeballEntry("cobblemon:timer_ball", 1),
        new WeightedPokeballEntry("cobblemon:dream_ball", 1),
        new WeightedPokeballEntry("cobblemon:beast_ball", 1),
        new WeightedPokeballEntry("cobblemon:ancient_poke_ball", 1),
        new WeightedPokeballEntry("cobblemon:ancient_great_ball", 1),
        new WeightedPokeballEntry("cobblemon:ancient_ultra_ball", 1),
        new WeightedPokeballEntry("cobblemon:ancient_heavy_ball", 1),
        new WeightedPokeballEntry("cobblemon:ancient_leaden_ball", 1),
        new WeightedPokeballEntry("cobblemon:ancient_gigaton_ball", 1),
        new WeightedPokeballEntry("cobblemon:ancient_feather_ball", 1),
        new WeightedPokeballEntry("cobblemon:ancient_wing_ball", 1),
        new WeightedPokeballEntry("cobblemon:ancient_jet_ball", 1),
        new WeightedPokeballEntry("cobblemon:ancient_origin_ball", 1),
        new WeightedPokeballEntry("cobblemon:ancient_strange_ball", 1)
    ));

    private Integer redChainRandomShinyRollMax = 1024;

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(reader, RandomizerItemsConfig.class);
                if (INSTANCE == null) {
                    CobbleSafari.LOGGER.warn(
                            "CobbleSafari >> randomizer_items_config.json at {} deserialized to null; using defaults",
                            CONFIG_PATH);
                    INSTANCE = new RandomizerItemsConfig();
                }
                if (INSTANCE.availablePokeBalls == null) {
                    INSTANCE.availablePokeBalls = new ArrayList<>();
                }
                if (INSTANCE.redChainRandomShinyRollMax == null) {
                    INSTANCE.redChainRandomShinyRollMax = 4095;
                }
                CobbleSafari.LOGGER.info("CobbleSafari >> randomizer_items_config.json loaded successfully from {}", CONFIG_PATH);
                CobbleSafari.LOGGER.info("CobbleSafari >> Persisting randomizer_items_config.json after load (normalized null fields to defaults)");
                save();
            } catch (IOException e) {
                CobbleSafari.LOGGER.error(
                        "CobbleSafari >> Failed to read randomizer_items_config.json at {} (I/O error). Using defaults and creating a new file from defaults.",
                        CONFIG_PATH,
                        e);
                INSTANCE = new RandomizerItemsConfig();
                INSTANCE.redChainRandomShinyRollMax = 4095;
                CobbleSafari.LOGGER.info("CobbleSafari >> Persisting randomizer_items_config.json after load (recovery from read error)");
                save();
            } catch (Exception e) {
                CobbleSafari.LOGGER.error(
                        "CobbleSafari >> Failed to parse randomizer_items_config.json at {} (invalid JSON). Using defaults and creating a new file from defaults.",
                        CONFIG_PATH,
                        e);
                INSTANCE = new RandomizerItemsConfig();
                INSTANCE.redChainRandomShinyRollMax = 4095;
                CobbleSafari.LOGGER.info("CobbleSafari >> Persisting randomizer_items_config.json after load (recovery from parse error)");
                save();
            }
        } else {
            CobbleSafari.LOGGER.info(
                    "CobbleSafari >> randomizer_items_config.json not found at {}, creating default file",
                    CONFIG_PATH);
            INSTANCE = new RandomizerItemsConfig();
            INSTANCE.redChainRandomShinyRollMax = 4095;
            CobbleSafari.LOGGER.info("CobbleSafari >> Persisting randomizer_items_config.json after load (first-time default file)");
            save();
        }
    }

    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new RandomizerItemsConfig();
        }
        if (INSTANCE.redChainRandomShinyRollMax == null) {
            INSTANCE.redChainRandomShinyRollMax = 4095;
        }
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
                CobbleSafari.LOGGER.info("CobbleSafari >> randomizer_items_config.json written to {}", CONFIG_PATH);
            }
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to save randomizer_items_config.json", e);
        }
    }

    public static int getRedChainRandomShinyRollMax() {
        if (INSTANCE == null || INSTANCE.redChainRandomShinyRollMax == null) {
            return 4095;
        }
        return Math.max(0, INSTANCE.redChainRandomShinyRollMax);
    }

    public static List<WeightedPokeBall> getWeightedPokeBalls() {
        if (INSTANCE == null) {
            return List.of();
        }

        List<WeightedPokeBall> resolved = new ArrayList<>();
        for (WeightedPokeballEntry entry : INSTANCE.availablePokeBalls) {
            if (entry == null || entry.weight <= 0 || entry.pokeballId == null || entry.pokeballId.isBlank()) {
                continue;
            }
            ResourceLocation id;
            try {
                id = ResourceLocation.parse(entry.pokeballId);
            } catch (Exception ignored) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (!(item instanceof PokeBallItem pokeBallItem)) {
                CobbleSafari.LOGGER.warn("Invalid weighted pokeball entry in config: {}", entry.pokeballId);
                continue;
            }
            PokeBall pokeBall = pokeBallItem.getPokeBall();
            if (pokeBall != null) {
                resolved.add(new WeightedPokeBall(pokeBall, entry.weight));
            }
        }
        return resolved;
    }

    public record WeightedPokeBall(PokeBall pokeBall, int weight) {
    }

    public static final class WeightedPokeballEntry {
        public String pokeballId;
        public int weight;

        public WeightedPokeballEntry() {
        }

        public WeightedPokeballEntry(String pokeballId, int weight) {
            this.pokeballId = pokeballId;
            this.weight = weight;
        }
    }
}
