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
            new WeightedPokeballEntry("cobblemon:poke_ball", 30),
            new WeightedPokeballEntry("cobblemon:great_ball", 20),
            new WeightedPokeballEntry("cobblemon:ultra_ball", 15),
            new WeightedPokeballEntry("cobblemon:premier_ball", 10),
            new WeightedPokeballEntry("cobblemon:luxury_ball", 6),
            new WeightedPokeballEntry("cobblemon:dusk_ball", 6),
            new WeightedPokeballEntry("cobblemon:timer_ball", 6),
            new WeightedPokeballEntry("cobblemon:quick_ball", 4),
            new WeightedPokeballEntry("cobblemon:repeat_ball", 3)
    ));

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(reader, RandomizerItemsConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new RandomizerItemsConfig();
                }
                if (INSTANCE.availablePokeBalls == null) {
                    INSTANCE.availablePokeBalls = new ArrayList<>();
                }
                save();
                CobbleSafari.LOGGER.info("Randomizer items config loaded from {}", CONFIG_PATH);
                return;
            } catch (IOException e) {
                CobbleSafari.LOGGER.error("Failed to load randomizer items config, using defaults", e);
            }
        }

        INSTANCE = new RandomizerItemsConfig();
        save();
    }

    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new RandomizerItemsConfig();
        }
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to save randomizer items config", e);
        }
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
