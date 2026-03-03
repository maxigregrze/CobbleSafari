package maxigregrze.cobblesafari.underground.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.InputStreamReader;
import java.util.Map;

public final class TreasureDataLoader {

    private TreasureDataLoader() {}

    private static final String DATA_DIR = "underground_treasure";

    private static int loadedCount;
    private static int skippedCount;

    public static void load(MinecraftServer server) {
        TreasureRegistry.clear();
        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources = manager.listResources(DATA_DIR,
                id -> id.getPath().endsWith(".json"));

        loadedCount = 0;
        skippedCount = 0;

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                loadSingleTreasure(fileId, reader);
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[TreasureDataLoader] Failed to load treasure file {}", fileId, e);
                skippedCount++;
            }
        }

        CobbleSafari.LOGGER.info("Loaded {} underground treasures ({} skipped)", loadedCount, skippedCount);
    }

    private static void loadSingleTreasure(ResourceLocation fileId, InputStreamReader reader) {
        JsonElement root = JsonParser.parseReader(reader);
        if (!root.isJsonObject()) {
            CobbleSafari.LOGGER.warn("[TreasureDataLoader] File {} is not a JSON object, skipping", fileId);
            skippedCount++;
            return;
        }
        JsonObject json = root.getAsJsonObject();

        if (!hasRequiredFields(json)) {
            CobbleSafari.LOGGER.warn("[TreasureDataLoader] File {} missing required fields, skipping", fileId);
            skippedCount++;
            return;
        }

        String itemIdStr = json.get("itemId").getAsString();
        String textureId = json.get("textureId").getAsString();
        String shapeId = json.get("shape").getAsString();
        int weight = json.get("weight").getAsInt();
        int minQty = json.has("minQty") ? json.get("minQty").getAsInt() : 1;
        int maxQty = json.has("maxQty") ? json.get("maxQty").getAsInt() : 1;

        if (weight <= 0) {
            CobbleSafari.LOGGER.warn("[TreasureDataLoader] File {} has weight <= 0, skipping", fileId);
            skippedCount++;
            return;
        }

        if (minQty < 1) minQty = 1;
        if (maxQty < minQty) maxQty = minQty;

        Item item = resolveItem(itemIdStr, fileId);
        if (item == null) {
            skippedCount++;
            return;
        }

        ShapeDefinition shape = ShapeRegistry.getShape(shapeId);
        if (shape == null) {
            CobbleSafari.LOGGER.warn("[TreasureDataLoader] Shape '{}' not found, skipping {}", shapeId, fileId);
            skippedCount++;
            return;
        }

        TreasureDefinition def = new TreasureDefinition(
                textureId, textureId, shape.getMatrix(), item, weight, minQty, maxQty
        );
        TreasureRegistry.register(def);
        loadedCount++;
    }

    private static boolean hasRequiredFields(JsonObject json) {
        return json.has("itemId") && json.has("textureId") && json.has("shape") && json.has("weight");
    }

    private static Item resolveItem(String itemIdStr, ResourceLocation fileId) {
        ResourceLocation itemLoc;
        try {
            itemLoc = ResourceLocation.parse(itemIdStr);
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn("[TreasureDataLoader] File {} has invalid itemId '{}', skipping", fileId, itemIdStr);
            return null;
        }

        if (!BuiltInRegistries.ITEM.containsKey(itemLoc)) {
            String namespace = itemLoc.getNamespace();
            if (Services.PLATFORM.isModLoaded(namespace)) {
                CobbleSafari.LOGGER.warn("[TreasureDataLoader] Item '{}' not found (typo or missing item in mod '{}'?), skipping {}", itemIdStr, namespace, fileId);
            } else {
                CobbleSafari.LOGGER.info("[TreasureDataLoader] Item '{}' not found (mod '{}' not installed), skipping {}", itemIdStr, namespace, fileId);
            }
            return null;
        }

        Item item = BuiltInRegistries.ITEM.get(itemLoc);
        if (item == Items.AIR) {
            CobbleSafari.LOGGER.debug("[TreasureDataLoader] Item '{}' resolved to AIR, skipping {}", itemIdStr, fileId);
            return null;
        }

        return item;
    }
}
