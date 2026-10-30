package maxigregrze.cobblesafari.cstrader.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
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

public final class CsTraderDataLoader {
    private CsTraderDataLoader() {}

    private static final String DATA_DIR = "cstrader";

    public static void load(MinecraftServer server) {
        CsTraderRegistry.clear();
        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources = manager.listResources(DATA_DIR,
                id -> id.getPath().endsWith(".json"));

        int loaded = 0;
        int skipped = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                CsTraderDefinition definition = loadSingle(fileId, reader);
                if (definition == null) {
                    skipped++;
                    continue;
                }
                CsTraderRegistry.register(definition);
                loaded++;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[CSTrader] Failed to load {}", fileId, e);
                skipped++;
            }
        }
        CobbleSafari.LOGGER.info("Loaded {} cstrader definitions ({} skipped)", loaded, skipped);
    }

    private static CsTraderDefinition loadSingle(ResourceLocation fileId, InputStreamReader reader) {
        JsonElement root = JsonParser.parseReader(reader);
        if (!root.isJsonObject()) {
            CobbleSafari.LOGGER.warn("[CSTrader] {} is not an object, skipped", fileId);
            return null;
        }
        JsonObject json = root.getAsJsonObject();
        if (!json.has("name") || !json.has("textureFile") || !json.has("variants")) {
            CobbleSafari.LOGGER.warn("[CSTrader] {} missing required fields (name/textureFile/variants), skipped", fileId);
            return null;
        }

        String name = json.get("name").getAsString();
        String displayName = json.has("displayName") ? json.get("displayName").getAsString() : name;
        String textureFile = json.get("textureFile").getAsString();
        boolean isKilleable = !json.has("isKilleable") || json.get("isKilleable").getAsBoolean();

        JsonArray variantsArray = json.getAsJsonArray("variants");
        List<CsTraderVariantDefinition> variants = new ArrayList<>();
        int variantIndex = 0;
        for (JsonElement variantElement : variantsArray) {
            variantIndex++;
            if (!variantElement.isJsonObject()) {
                CobbleSafari.LOGGER.warn("[CSTrader] {} variant #{} is invalid, skipped", fileId, variantIndex);
                continue;
            }
            JsonObject variantJson = variantElement.getAsJsonObject();
            CsTraderVariantDefinition variant = parseVariant(fileId, name, variantJson, variantIndex);
            if (variant != null) {
                variants.add(variant);
            }
        }

        if (variants.isEmpty()) {
            CobbleSafari.LOGGER.warn("[CSTrader] {} has no valid variants, skipped", fileId);
            return null;
        }

        return new CsTraderDefinition(name, displayName, textureFile, isKilleable, variants);
    }

    private static CsTraderVariantDefinition parseVariant(ResourceLocation fileId, String traderName, JsonObject variantJson, int variantIndex) {
        if (!variantJson.has("id") || (!variantJson.has("trades") && !variantJson.has("tradeGroups"))) {
            CobbleSafari.LOGGER.warn("[CSTrader] {} trader={} variant #{} missing id/trades/tradeGroups, skipped",
                    fileId, traderName, variantIndex);
            return null;
        }
        String id = variantJson.get("id").getAsString();
        List<String> aliases = new ArrayList<>();
        if (variantJson.has("aliases") && variantJson.get("aliases").isJsonArray()) {
            for (JsonElement aliasElement : variantJson.getAsJsonArray("aliases")) {
                aliases.add(aliasElement.getAsString());
            }
        }

        // Groups are drawn in file order; a top-level "trades" list acts as one more, last group.
        List<CsTraderTradeGroup> groups = new ArrayList<>();
        if (variantJson.has("tradeGroups") && variantJson.get("tradeGroups").isJsonArray()) {
            for (JsonElement groupElement : variantJson.getAsJsonArray("tradeGroups")) {
                if (!groupElement.isJsonObject() || !groupElement.getAsJsonObject().has("trades")) {
                    CobbleSafari.LOGGER.warn("[CSTrader] {} trader={} variant={} has an invalid trade group, skipped",
                            fileId, traderName, id);
                    continue;
                }
                CsTraderTradeGroup group = parseTradeGroup(fileId, traderName, id, groupElement.getAsJsonObject());
                if (group != null) {
                    groups.add(group);
                }
            }
        }
        if (variantJson.has("trades")) {
            CsTraderTradeGroup group = parseTradeGroup(fileId, traderName, id, variantJson);
            if (group != null) {
                groups.add(group);
            }
        }

        if (groups.isEmpty()) {
            CobbleSafari.LOGGER.warn("[CSTrader] {} trader={} variant={} has no valid trades, skipped",
                    fileId, traderName, id);
            return null;
        }
        return new CsTraderVariantDefinition(id, aliases, groups);
    }

    /** Reads {@code trades} + {@code minTrades}/{@code maxTrades} from a variant or a trade group object. */
    private static CsTraderTradeGroup parseTradeGroup(ResourceLocation fileId, String traderName, String variantId,
                                                      JsonObject groupJson) {
        int minTrades = groupJson.has("minTrades") ? groupJson.get("minTrades").getAsInt() : 1;
        int maxTrades = groupJson.has("maxTrades") ? groupJson.get("maxTrades").getAsInt() : minTrades;

        List<CsTraderTradeDefinition> trades = new ArrayList<>();
        int tradeIndex = 0;
        for (JsonElement tradeElement : groupJson.getAsJsonArray("trades")) {
            tradeIndex++;
            if (!tradeElement.isJsonObject()) {
                CobbleSafari.LOGGER.warn("[CSTrader] {} trader={} variant={} trade #{} invalid object, skipped",
                        fileId, traderName, variantId, tradeIndex);
                continue;
            }
            CsTraderTradeDefinition trade = parseTrade(fileId, traderName, variantId, tradeIndex, tradeElement.getAsJsonObject());
            if (trade != null) {
                trades.add(trade);
            }
        }
        if (trades.isEmpty()) {
            return null;
        }

        int clampedMax = Math.max(1, Math.min(maxTrades, trades.size()));
        int clampedMin = Math.max(1, Math.min(minTrades, clampedMax));
        return new CsTraderTradeGroup(clampedMin, clampedMax, trades);
    }

    private static CsTraderTradeDefinition parseTrade(ResourceLocation fileId, String traderName, String variantId, int tradeIndex, JsonObject tradeJson) {
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

        List<CsTraderItemSpec> resultItems = new ArrayList<>();
        List<CsTraderResultOption> resultOptions = new ArrayList<>();
        List<ResourceLocation> resultTmPool = new ArrayList<>();

        boolean hasResultItems = tradeJson.has("resultItems") && tradeJson.get("resultItems").isJsonArray();
        boolean hasResultOptions = tradeJson.has("resultOptions") && tradeJson.get("resultOptions").isJsonArray();
        boolean hasResultTm = tradeJson.has("resultTm") || tradeJson.has("resultTmPool");
        if (!hasResultItems && !hasResultOptions && !hasResultTm) {
            warnSkip(fileId, traderName, variantId, tradeIndex, "missing resultItems/resultOptions/resultTm");
            return null;
        }

        if (hasResultItems) {
            for (JsonElement itemElement : tradeJson.getAsJsonArray("resultItems")) {
                CsTraderItemSpec spec = resolveItemSpec(fileId, traderName, variantId, tradeIndex, "resultItems", itemElement);
                if (spec != null) {
                    resultItems.add(spec);
                }
            }
        }
        if (hasResultOptions) {
            for (JsonElement optionElement : tradeJson.getAsJsonArray("resultOptions")) {
                if (!optionElement.isJsonObject()) continue;
                JsonObject optionJson = optionElement.getAsJsonObject();
                if (!optionJson.has("itemId") || !optionJson.has("qty")) continue;
                CsTraderItemSpec spec = resolveItemSpec(fileId, traderName, variantId, tradeIndex,
                        "resultOptions.itemId", optionJson.get("itemId"));
                if (spec == null) continue;
                int qty = Math.max(1, optionJson.get("qty").getAsInt());
                resultOptions.add(new CsTraderResultOption(spec, qty));
            }
        }
        if (hasResultTm) {
            if (tradeJson.has("resultTm")) {
                addTmId(resultTmPool, fileId, traderName, variantId, tradeIndex, tradeJson.get("resultTm"));
            }
            if (tradeJson.has("resultTmPool") && tradeJson.get("resultTmPool").isJsonArray()) {
                for (JsonElement tmElement : tradeJson.getAsJsonArray("resultTmPool")) {
                    addTmId(resultTmPool, fileId, traderName, variantId, tradeIndex, tmElement);
                }
            }
        }

        if (resultItems.isEmpty() && resultOptions.isEmpty() && resultTmPool.isEmpty()) {
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

        return new CsTraderTradeDefinition(
                sourceItem1,
                sourceQty1,
                sourceItem2,
                sourceQty2,
                resultItems,
                resultOptions,
                resultTmPool,
                resultQty,
                resultQtyMin,
                resultQtyMax,
                priceShiftMax,
                maxUses,
                xp,
                priceMultiplier
        );
    }

    /**
     * Reads a result item, accepting either a bare id string (the historical form, unchanged) or an
     * object carrying data components.
     */
    private static CsTraderItemSpec resolveItemSpec(ResourceLocation fileId, String traderName, String variantId,
                                                    int tradeIndex, String field, JsonElement element) {
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            Item item = resolveItemStrict(fileId, traderName, variantId, tradeIndex, field, primitive.getAsString());
            return item == null ? null : CsTraderItemSpec.of(item);
        }
        if (!element.isJsonObject()) {
            warnSkip(fileId, traderName, variantId, tradeIndex, field + " must be an id or an object");
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        if (!object.has("id")) {
            warnSkip(fileId, traderName, variantId, tradeIndex, field + " object is missing 'id'");
            return null;
        }
        Item item = resolveItemStrict(fileId, traderName, variantId, tradeIndex, field, object.get("id").getAsString());
        if (item == null) {
            return null;
        }

        List<CsTraderComponentSpec> components = new ArrayList<>();
        if (object.has("dataComponents") && object.get("dataComponents").isJsonArray()) {
            for (JsonElement componentElement : object.getAsJsonArray("dataComponents")) {
                if (!componentElement.isJsonObject()) continue;
                JsonObject componentJson = componentElement.getAsJsonObject();
                if (!componentJson.has("dataId") || !componentJson.has("dataValue")) {
                    warnSkip(fileId, traderName, variantId, tradeIndex,
                            field + " component is missing dataId/dataValue");
                    continue;
                }
                ResourceLocation dataId;
                try {
                    dataId = ResourceLocation.parse(componentJson.get("dataId").getAsString());
                } catch (Exception e) {
                    warnSkip(fileId, traderName, variantId, tradeIndex,
                            field + " component has an invalid dataId");
                    continue;
                }
                if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(dataId)) {
                    warnSkip(fileId, traderName, variantId, tradeIndex,
                            field + " component type not found: " + dataId);
                    continue;
                }
                components.add(new CsTraderComponentSpec(dataId, componentJson.get("dataValue")));
            }
        }
        return new CsTraderItemSpec(item, List.copyOf(components));
    }

    /** Stores a TM id verbatim; existence is checked when the offer is generated, not here. */
    private static void addTmId(List<ResourceLocation> pool, ResourceLocation fileId, String traderName,
                                String variantId, int tradeIndex, JsonElement element) {
        String raw = element.isJsonPrimitive() ? element.getAsString() : null;
        if (raw == null || raw.isBlank()) {
            warnSkip(fileId, traderName, variantId, tradeIndex, "resultTm entry is empty");
            return;
        }
        try {
            pool.add(ResourceLocation.parse(raw));
        } catch (Exception e) {
            warnSkip(fileId, traderName, variantId, tradeIndex, "resultTm has an invalid id: " + raw);
        }
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
        CobbleSafari.LOGGER.warn("[CSTrader] Skipping trade file={} trader={} variant={} tradeIndex={} reason={}",
                fileId, traderName, variantId, tradeIndex, reason);
    }
}
