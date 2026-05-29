package maxigregrze.cobblesafari.wondertrade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.WonderTradeSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WonderTradeDataLoader {
    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_EVENT_NAME = "eventName";
    private static final String PREFIX_GROUPS = "wonder_trade/pokemon_groups";
    private static final String PREFIX_EVENTS = "wonder_trade/events";

    private WonderTradeDataLoader() {}

    public static void load(MinecraftServer server) {
        WonderTradeGroupRegistry.clear();
        WonderTradeEventRegistry.clear();
        ResourceManager manager = server.getResourceManager();
        loadGroups(manager);
        loadEvents(manager);
    }

    private static void loadGroups(ResourceManager manager) {
        Map<ResourceLocation, Resource> resources = manager.listResources(PREFIX_GROUPS, id -> id.getPath().endsWith(".json"));
        int ok = 0;
        int skipped = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    skipped++;
                    continue;
                }
                JsonObject json = root.getAsJsonObject();
                if (!json.has(KEY_GROUP_ID) || !json.has("population")) {
                    CobbleSafari.LOGGER.warn("[WonderTrade] group {} missing groupId/population", entry.getKey());
                    skipped++;
                    continue;
                }
                String groupId = json.get(KEY_GROUP_ID).getAsString();
                JsonArray pop = json.getAsJsonArray("population");
                List<String> population = new ArrayList<>();
                for (JsonElement el : pop) {
                    population.add(el.getAsString());
                }
                if (population.isEmpty()) {
                    CobbleSafari.LOGGER.warn("[WonderTrade] group {} has empty population", entry.getKey());
                    skipped++;
                    continue;
                }
                WonderTradeGroupRegistry.register(new PokemonGroupDefinition(groupId, population));
                ok++;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[WonderTrade] Failed to load group {}", entry.getKey(), e);
                skipped++;
            }
        }
        CobbleSafari.LOGGER.info("[WonderTrade] Loaded {} pokemon group(s), {} skipped", ok, skipped);
    }

    private static void loadEvents(ResourceManager manager) {
        Map<ResourceLocation, Resource> resources = manager.listResources(PREFIX_EVENTS, id -> id.getPath().endsWith(".json"));
        int ok = 0;
        int skipped = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    skipped++;
                    continue;
                }
                JsonObject json = root.getAsJsonObject();
                if (!json.has("eventId") || !json.has("eventPools")) {
                    CobbleSafari.LOGGER.warn("[WonderTrade] event {} missing eventId/eventPools", entry.getKey());
                    skipped++;
                    continue;
                }
                String eventId = json.get("eventId").getAsString();
                String eventName = json.has(KEY_EVENT_NAME) ? json.get(KEY_EVENT_NAME).getAsString() : eventId;
                if (!json.has(KEY_EVENT_NAME)) {
                    CobbleSafari.LOGGER.warn("[WonderTrade] event {} missing eventName; using eventId as fallback", entry.getKey());
                }
                boolean hasBanner = json.has("hasCustomBanner") && json.get("hasCustomBanner").getAsBoolean();
                String customBannerName = json.has("customBannerName") ? json.get("customBannerName").getAsString() : "";
                if (hasBanner && (customBannerName == null || customBannerName.isBlank())) {
                    CobbleSafari.LOGGER.warn(
                            "[WonderTrade] event {} hasCustomBanner=true but customBannerName is missing/blank; using default banner",
                            entry.getKey());
                }
                JsonArray pools = json.getAsJsonArray("eventPools");
                List<WonderTradeSettings.WeightedPoolEntry> eventPools = new ArrayList<>();
                for (JsonElement el : pools) {
                    if (!el.isJsonObject()) continue;
                    JsonObject p = el.getAsJsonObject();
                    if (!p.has(KEY_GROUP_ID) || !p.has("weight")) continue;
                    WonderTradeSettings.WeightedPoolEntry w = new WonderTradeSettings.WeightedPoolEntry();
                    w.groupId = p.get(KEY_GROUP_ID).getAsString();
                    w.weight = p.get("weight").getAsInt();
                    eventPools.add(w);
                }
                if (eventPools.isEmpty()) {
                    CobbleSafari.LOGGER.warn("[WonderTrade] event {} has no valid pools", entry.getKey());
                    skipped++;
                    continue;
                }
                WonderTradeEventRegistry.register(new WonderTradeEventDefinition(
                        eventId, eventName, hasBanner, customBannerName, eventPools));
                ok++;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[WonderTrade] Failed to load event {}", entry.getKey(), e);
                skipped++;
            }
        }
        CobbleSafari.LOGGER.info("[WonderTrade] Loaded {} event(s), {} skipped", ok, skipped);
    }
}
