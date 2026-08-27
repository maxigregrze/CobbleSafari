package maxigregrze.cobblesafari.warpride.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code data/cobblesafari/ultra_warp_ride/destination/*.json}. Staged validation: a missing
 * or duplicate {@code typeId}, {@code tint} or {@code weight} is rejected, while an unresolvable
 * {@code structurePool} or empty species list is only warned about, having no consumer yet.
 */
public final class WarpRideDestinationLoader {

    private WarpRideDestinationLoader() {}

    private static final String DATA_DIR = "ultra_warp_ride/destination";
    private static final int DEFAULT_TINT = 0xFFFFFFFF;

    public static void load(MinecraftServer server) {
        WarpRideDestinationRegistry.clear();

        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources = manager.listResources(DATA_DIR,
                id -> id.getPath().endsWith(".json"));

        int loaded = 0;
        int skipped = 0;

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                if (loadSingle(fileId, JsonParser.parseReader(reader))) {
                    loaded++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[WarpRide] Failed to load destination file {}", fileId, e);
                skipped++;
            }
        }

        if (WarpRideDestinationRegistry.isEmpty()) {
            CobbleSafari.LOGGER.warn("[WarpRide] No destination registered; portals cannot be rolled "
                    + "and the mini-game will refuse to start");
        } else {
            CobbleSafari.LOGGER.info("[WarpRide] Loaded {} destinations ({} skipped)", loaded, skipped);
        }
    }

    private static boolean loadSingle(ResourceLocation fileId, JsonElement root) {
        if (!root.isJsonObject()) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} is not a JSON object, skipping", fileId);
            return false;
        }
        JsonObject json = root.getAsJsonObject();

        // --- hard-validated: consumed by this plan ---
        if (!json.has("typeId")) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} has no typeId, skipping", fileId);
            return false;
        }
        int typeId = json.get("typeId").getAsInt();
        if (typeId < 1) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} has typeId={} (must be >= 1), skipping",
                    fileId, typeId);
            return false;
        }

        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
        if (weight < 0) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} has a negative weight, clamping to 0", fileId);
            weight = 0;
        }

        int tint = DEFAULT_TINT;
        if (json.has("tint")) {
            Integer parsed = parseTint(json.get("tint").getAsString());
            if (parsed == null) {
                CobbleSafari.LOGGER.warn("[WarpRide] Destination {} has an unparsable tint '{}', "
                        + "using white", fileId, json.get("tint").getAsString());
            } else {
                tint = parsed;
            }
        } else {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} has no tint, using white", fileId);
        }

        // --- warning only: no consumer in this plan (see the class javadoc) ---
        String structurePool = json.has("structurePool") ? json.get("structurePool").getAsString() : "";
        if (structurePool.isEmpty()) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} declares no structurePool. Harmless "
                    + "until the sub-dungeon mechanism exists, which is what will hard-validate it", fileId);
        } else if (ResourceLocation.tryParse(structurePool) == null) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} has a malformed structurePool '{}'. "
                    + "Harmless until generation consumes it, but it will be rejected then",
                    fileId, structurePool);
        }

        Map<Integer, List<String>> encounters = readEncounters(fileId, json);

        WarpRideDestination destination =
                new WarpRideDestination(typeId, weight, structurePool, tint, encounters);

        if (!WarpRideDestinationRegistry.register(destination)) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} duplicates typeId={} already "
                    + "registered; skipping. typeId must be unique — it is what a persisted record "
                    + "resolves through", fileId, typeId);
            return false;
        }
        return true;
    }

    private static Map<Integer, List<String>> readEncounters(ResourceLocation fileId, JsonObject json) {
        Map<Integer, List<String>> encounters = new LinkedHashMap<>();
        if (!json.has("encounters") || !json.get("encounters").isJsonObject()) {
            return encounters;
        }
        JsonObject obj = json.getAsJsonObject("encounters");
        for (int rank = 0; rank < WarpRideDifficulty.RANK_COUNT; rank++) {
            String key = Integer.toString(rank);
            if (!obj.has(key) || !obj.get(key).isJsonArray()) {
                continue;
            }
            JsonArray array = obj.getAsJsonArray(key);
            List<String> species = new ArrayList<>(array.size());
            for (JsonElement e : array) {
                String value = e.getAsString();
                if (!value.isBlank()) {
                    species.add(value);
                }
            }
            encounters.put(rank, List.copyOf(species));
        }
        if (encounters.isEmpty()) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} has no encounter list. Harmless until "
                    + "the species plan consumes it", fileId);
        }
        return encounters;
    }

    /** Accepts {@code #RRGGBB} and {@code #AARRGGBB}, with or without the leading hash. */
    private static Integer parseTint(String raw) {
        String value = raw.startsWith("#") ? raw.substring(1) : raw;
        try {
            if (value.length() == 6) {
                return 0xFF000000 | Integer.parseInt(value, 16);
            }
            if (value.length() == 8) {
                return (int) Long.parseLong(value, 16);
            }
        } catch (NumberFormatException ignored) {
            // Falls through to the null return below; the caller logs and uses white.
        }
        return null;
    }
}
