package maxigregrze.cobblesafari.cftrader.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CfTraderDataLoader {
    private CfTraderDataLoader() {}

    private static final String DATA_DIR = "cftrader";

    public static void load(MinecraftServer server) {
        CfTraderRegistry.clear();
        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources = manager.listResources(DATA_DIR,
                id -> id.getPath().endsWith(".json"));

        int loaded = 0;
        int skipped = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                CfTraderDefinition definition = loadSingle(fileId, reader);
                if (definition == null) {
                    skipped++;
                    continue;
                }
                CfTraderRegistry.register(definition);
                loaded++;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[CFTrader] Failed to load {}", fileId, e);
                skipped++;
            }
        }
        CobbleSafari.LOGGER.info("Loaded {} cftrader definitions ({} skipped)", loaded, skipped);
    }

    private static CfTraderDefinition loadSingle(ResourceLocation fileId, InputStreamReader reader) {
        JsonElement root = JsonParser.parseReader(reader);
        if (!root.isJsonObject()) {
            CobbleSafari.LOGGER.warn("[CFTrader] {} is not an object, skipped", fileId);
            return null;
        }
        JsonObject json = root.getAsJsonObject();
        if (!json.has("name") || !json.has("textureFile") || !json.has("variants")) {
            CobbleSafari.LOGGER.warn("[CFTrader] {} missing required fields (name/textureFile/variants), skipped", fileId);
            return null;
        }

        String name = json.get("name").getAsString();
        String displayName = json.has("displayName") ? json.get("displayName").getAsString() : name;
        String textureFile = json.get("textureFile").getAsString();
        boolean isKilleable = !json.has("isKilleable") || json.get("isKilleable").getAsBoolean();

        JsonArray variantsArray = json.getAsJsonArray("variants");
        List<CfTraderVariantDefinition> variants = new ArrayList<>();
        int variantIndex = 0;
        for (JsonElement variantElement : variantsArray) {
            variantIndex++;
            if (!variantElement.isJsonObject()) {
                CobbleSafari.LOGGER.warn("[CFTrader] {} variant #{} is invalid, skipped", fileId, variantIndex);
                continue;
            }
            JsonObject variantJson = variantElement.getAsJsonObject();
            CfTraderVariantDefinition variant = parseVariant(fileId, name, variantJson, variantIndex);
            if (variant != null) {
                variants.add(variant);
            }
        }

        if (variants.isEmpty()) {
            CobbleSafari.LOGGER.warn("[CFTrader] {} has no valid variants, skipped", fileId);
            return null;
        }

        return new CfTraderDefinition(name, displayName, textureFile, isKilleable, variants);
    }

    private static CfTraderVariantDefinition parseVariant(ResourceLocation fileId, String traderName, JsonObject variantJson, int variantIndex) {
        if (!variantJson.has("id") || !variantJson.has("trades")) {
            CobbleSafari.LOGGER.warn("[CFTrader] {} trader={} variant #{} missing id/trades, skipped",
                    fileId, traderName, variantIndex);
            return null;
        }
        String id = variantJson.get("id").getAsString();
        int minTrades = variantJson.has("minTrades") ? variantJson.get("minTrades").getAsInt() : 1;
        int maxTrades = variantJson.has("maxTrades") ? variantJson.get("maxTrades").getAsInt() : minTrades;
        List<String> aliases = new ArrayList<>();
        if (variantJson.has("aliases") && variantJson.get("aliases").isJsonArray()) {
            for (JsonElement aliasElement : variantJson.getAsJsonArray("aliases")) {
                aliases.add(aliasElement.getAsString());
            }
        }

        JsonArray tradesArray = variantJson.getAsJsonArray("trades");
        List<CfTraderTradeDefinition> trades = new ArrayList<>();
        int tradeIndex = 0;
        for (JsonElement tradeElement : tradesArray) {
            tradeIndex++;
            if (!tradeElement.isJsonObject()) {
                CobbleSafari.LOGGER.warn("[CFTrader] {} trader={} variant={} trade #{} invalid object, skipped",
                        fileId, traderName, id, tradeIndex);
                continue;
            }
            CfTraderTradeDefinition trade = parseTrade(fileId, traderName, id, tradeIndex, tradeElement.getAsJsonObject());
            if (trade != null) {
                trades.add(trade);
            }
        }

        if (trades.isEmpty()) {
            CobbleSafari.LOGGER.warn("[CFTrader] {} trader={} variant={} has no valid trades, skipped",
                    fileId, traderName, id);
            return null;
        }

        int clampedMax = Math.max(1, Math.min(maxTrades, trades.size()));
        int clampedMin = Math.max(1, Math.min(minTrades, clampedMax));
        return new CfTraderVariantDefinition(id, aliases, clampedMin, clampedMax, trades);
    }

    private static CfTraderTradeDefinition parseTrade(ResourceLocation fileId, String traderName, String variantId, int tradeIndex, JsonObject tradeJson) {
        if (!tradeJson.has("sourceItem1") || !tradeJson.has("sourceQty1")) {
            warnSkip(fileId, traderName, variantId, tradeIndex, "missing sourceItem1/sourceQty1");
            return null;
        }
        Item sourceItem1 = resolveItemStrict(fileId, traderName, variantId, tradeIndex, "sourceItem1", tradeJson.get("sourceItem1").getAsString());
        if (sourceItem1 == null) return null;
        int sourceQty1 = Math.max(1, tradeJson.get("sourceQty1").getAsInt());

        Item sourceItem2 = null;
        int sourceQty2 = 0;
        if (tradeJson.has("sourceItem2")) {
            sourceItem2 = resolveItemStrict(fileId, traderName, variantId, tradeIndex, "sourceItem2", tradeJson.get("sourceItem2").getAsString());
            if (sourceItem2 == null) return null;
            sourceQty2 = tradeJson.has("sourceQty2") ? Math.max(1, tradeJson.get("sourceQty2").getAsInt()) : 1;
        }

        List<Item> resultItems = new ArrayList<>();
        List<CfTraderResultOption> resultOptions = new ArrayList<>();

        boolean hasResultItems = tradeJson.has("resultItems") && tradeJson.get("resultItems").isJsonArray();
        boolean hasResultOptions = tradeJson.has("resultOptions") && tradeJson.get("resultOptions").isJsonArray();
        if (!hasResultItems && !hasResultOptions) {
            warnSkip(fileId, traderName, variantId, tradeIndex, "missing resultItems/resultOptions");
            return null;
        }

        if (hasResultItems) {
            for (JsonElement itemElement : tradeJson.getAsJsonArray("resultItems")) {
                Item item = resolveItemStrict(fileId, traderName, variantId, tradeIndex, "resultItems", itemElement.getAsString());
                if (item != null) {
                    resultItems.add(item);
                }
            }
        }
        if (hasResultOptions) {
            for (JsonElement optionElement : tradeJson.getAsJsonArray("resultOptions")) {
                if (!optionElement.isJsonObject()) continue;
                JsonObject optionJson = optionElement.getAsJsonObject();
                if (!optionJson.has("itemId") || !optionJson.has("qty")) continue;
                Item optionItem = resolveItemStrict(fileId, traderName, variantId, tradeIndex, "resultOptions.itemId", optionJson.get("itemId").getAsString());
                if (optionItem == null) continue;
                int qty = Math.max(1, optionJson.get("qty").getAsInt());
                resultOptions.add(new CfTraderResultOption(optionItem, qty));
            }
        }

        if (resultItems.isEmpty() && resultOptions.isEmpty()) {
            warnSkip(fileId, traderName, variantId, tradeIndex, "all result items/options invalid or missing");
            return null;
        }

        int resultQty = tradeJson.has("resultQty") ? Math.max(1, tradeJson.get("resultQty").getAsInt()) : 1;
        int resultQtyMin = tradeJson.has("resultQtyMin") ? Math.max(1, tradeJson.get("resultQtyMin").getAsInt()) : 0;
        int resultQtyMax = tradeJson.has("resultQtyMax") ? Math.max(resultQtyMin, tradeJson.get("resultQtyMax").getAsInt()) : 0;
        int priceShiftMax = tradeJson.has("priceShiftMax") ? Math.max(0, tradeJson.get("priceShiftMax").getAsInt()) : 0;
        int maxUses = tradeJson.has("maxUses") ? Math.max(1, tradeJson.get("maxUses").getAsInt()) : 8;
        int xp = tradeJson.has("xp") ? Math.max(0, tradeJson.get("xp").getAsInt()) : 0;
        float priceMultiplier = tradeJson.has("priceMultiplier") ? tradeJson.get("priceMultiplier").getAsFloat() : 0.0f;

        return new CfTraderTradeDefinition(
                sourceItem1,
                sourceQty1,
                sourceItem2,
                sourceQty2,
                resultItems,
                resultOptions,
                resultQty,
                resultQtyMin,
                resultQtyMax,
                priceShiftMax,
                maxUses,
                xp,
                priceMultiplier
        );
    }

    private static Item resolveItemStrict(ResourceLocation fileId, String traderName, String variantId, int tradeIndex,
                                          String field, String rawId) {
        if (rawId == null || rawId.isBlank()) {
            warnSkip(fileId, traderName, variantId, tradeIndex, field + " is empty");
            return null;
        }
        if ("minecraft:air".equals(rawId)) {
            warnSkip(fileId, traderName, variantId, tradeIndex, field + " cannot be minecraft:air");
            return null;
        }
        ResourceLocation id;
        try {
            id = ResourceLocation.parse(rawId);
        } catch (Exception e) {
            warnSkip(fileId, traderName, variantId, tradeIndex, field + " has invalid id: " + rawId);
            return null;
        }

        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            warnSkip(fileId, traderName, variantId, tradeIndex, field + " item not found: " + rawId);
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            warnSkip(fileId, traderName, variantId, tradeIndex, field + " resolved to AIR: " + rawId);
            return null;
        }
        return item;
    }

    private static void warnSkip(ResourceLocation fileId, String traderName, String variantId, int tradeIndex, String reason) {
        CobbleSafari.LOGGER.warn("[CFTrader] Skipping trade file={} trader={} variant={} tradeIndex={} reason={}",
                fileId, traderName, variantId, tradeIndex, reason);
    }
}
