package maxigregrze.cobblesafari.incubator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.IncubatorConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EggIncubatorRegistry {

    private EggIncubatorRegistry() {}

    private static final Map<ResourceLocation, EggIncubatorRecipe> RECIPES = new HashMap<>();
    private static final String DATA_DIR = "egg_incubator";

    public static void load(MinecraftServer server) {
        RECIPES.clear();
        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources = manager.listResources(DATA_DIR,
                id -> id.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                String inputStr = json.get("input").getAsString();
                ResourceLocation inputItem = ResourceLocation.parse(inputStr);

                JsonArray outputsArray = json.getAsJsonArray("outputs");
                List<String> outputs = new ArrayList<>();
                for (JsonElement elem : outputsArray) {
                    outputs.add(elem.getAsString());
                }

                if (outputs.isEmpty()) {
                    CobbleSafari.LOGGER.warn("Egg incubator data file {} has empty outputs, skipping", fileId);
                    continue;
                }

                int hatchTime = json.has("hatchtime") ? json.get("hatchtime").getAsInt() : -1;
                int shinyBoost = json.has("shinyboost") ? json.get("shinyboost").getAsInt() : 64;
                if (hatchTime <= 0) {
                    hatchTime = IncubatorConfig.getDefaultWildEggHatchTimeTicks();
                }
                if (shinyBoost < 1) {
                    shinyBoost = 1;
                }

                RECIPES.put(inputItem, new EggIncubatorRecipe(inputItem, outputs, hatchTime, shinyBoost));
                CobbleSafari.LOGGER.debug("Loaded egg incubator recipe for {} with {} outputs", inputItem, outputs.size());
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("Failed to load egg incubator data file {}", fileId, e);
            }
        }

        CobbleSafari.LOGGER.info("Loaded {} egg incubator recipes", RECIPES.size());
    }

    public static EggIncubatorRecipe getRecipe(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return RECIPES.get(itemId);
    }

    public static boolean isValidInput(ItemStack stack) {
        return getRecipe(stack) != null;
    }
}
