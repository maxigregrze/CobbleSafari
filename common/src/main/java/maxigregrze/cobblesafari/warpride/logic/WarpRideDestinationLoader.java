package maxigregrze.cobblesafari.warpride.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code data/cobblesafari/ultra_warp_ride/destination/*.json}.
 *
 * <p>Validation follows the consumer. {@code typeId}, {@code tint} and {@code weight} have been
 * hard-validated since the ride engine shipped. <b>{@code structurePool} joins them here</b>
 * (action plan 183, section 13.2, finding W12): this plan is the first to generate anything from
 * it, and a pool that fails to resolve mid-handoff would leave the rider in a locked screen with
 * their once-per-run entry already spent. Rejecting at load is the only place that cannot happen.</p>
 *
 * <p>The encounter pools (action plan 190, E1) are hard on <b>shape</b> - a destination with no
 * normal pool at all is rejected, since the encounter is what the ride pays for - and soft on
 * <b>content</b>: an unreadable entry is dropped with a warning, and an entry whose species is
 * missing or has no model is only <em>reported</em> (E4). Whether it can be spawned is decided at
 * the roll, where species data is guaranteed current.</p>
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

        reportUnavailableEntries();

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

        // --- hard from this plan on: generation is now a real consumer (W12) ---
        String structurePool = json.has("structurePool") ? json.get("structurePool").getAsString() : "";
        if (structurePool.isEmpty()) {
            CobbleSafari.LOGGER.error("[WarpRide] Destination {} declares no structurePool, rejecting: "
                    + "the sub-dungeon generator would have nothing to place", fileId);
            return false;
        }
        if (ResourceLocation.tryParse(structurePool) == null) {
            CobbleSafari.LOGGER.error("[WarpRide] Destination {} has a malformed structurePool '{}', "
                    + "rejecting", fileId, structurePool);
            return false;
        }

        // --- hard from plan 187 on: a destination that names a dimension is generated in it ---
        // Validated exactly as strictly as structurePool, and for the same reason (W12): both are
        // consumed mid-handoff, when the rider's once-per-run entry is already spent and the only
        // remaining outcome for a bad value is a locked screen.
        String dimension = readOptionalString(json, "dimension");
        if (dimension != null) {
            if (ResourceLocation.tryParse(dimension) == null) {
                CobbleSafari.LOGGER.error("[WarpRide] Destination {} has a malformed dimension '{}', "
                        + "rejecting", fileId, dimension);
                return false;
            }
            DungeonConfig host = DungeonDimensions.getDungeonByDimensionId(dimension);
            if (host == null) {
                CobbleSafari.LOGGER.error("[WarpRide] Destination {} names dimension '{}', which is not "
                        + "a registered dungeon; rejecting", fileId, dimension);
                return false;
            }
            if (!host.isSubDungeon()) {
                CobbleSafari.LOGGER.error("[WarpRide] Destination {} names dimension '{}', which is a "
                        + "registered dungeon but not a sub-dungeon; rejecting", fileId, dimension);
                return false;
            }
        }

        String biome = readOptionalString(json, "biome");
        if (biome != null && ResourceLocation.tryParse(biome) == null) {
            CobbleSafari.LOGGER.error("[WarpRide] Destination {} has a malformed biome '{}', rejecting",
                    fileId, biome);
            return false;
        }
        if (dimension != null && biome != null) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} declares both a dimension and a biome; "
                    + "the biome is ignored - a dimension supplies its own", fileId);
        }

        // --- encounter pools (plan 190, E1) ---
        if (json.has("encounters")) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} still declares 'encounters', ignored since "
                    + "plan 190: use 'normalPool' and 'rewardPool'", fileId);
        }
        WarpRideEncounterPool normalPool = readPool(fileId, json, "normalPool", false);
        if (normalPool.isEmpty()) {
            CobbleSafari.LOGGER.error("[WarpRide] Destination {} has no usable normalPool (no entry and no "
                    + "fallback), rejecting: the encounter is what the ride pays for", fileId);
            return false;
        }
        WarpRideEncounterPool rewardPool = readPool(fileId, json, "rewardPool", true);
        double[] rewardPoolPercent = readRewardPoolPercent(fileId, json);

        WarpRideDestination destination = new WarpRideDestination(
                destinationIdOf(fileId), typeId, weight, structurePool, tint, dimension, biome,
                normalPool, rewardPool, rewardPoolPercent);

        if (!WarpRideDestinationRegistry.register(destination)) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} duplicates typeId={} already "
                    + "registered; skipping. typeId must be unique — it is what a persisted record "
                    + "resolves through", fileId, typeId);
            return false;
        }
        return true;
    }

    /**
     * The destination's human name: the file's base name, without directory or extension
     * ({@code .../destination/debug_alpha.json} to {@code debug_alpha}). Nothing to author, and the
     * six real destinations will name themselves (action plan 184, section 12.3).
     */
    private static String destinationIdOf(ResourceLocation fileId) {
        String path = fileId.getPath();
        int slash = path.lastIndexOf('/');
        String name = slash < 0 ? path : path.substring(slash + 1);
        return name.endsWith(".json") ? name.substring(0, name.length() - ".json".length()) : name;
    }

    /** A trimmed string field, or {@code null} when absent, non-primitive or blank. */
    private static String readOptionalString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return null;
        }
        String value = json.get(key).getAsString().trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Reads one pool. A normal pool lists bare strings, all of weight 1; a reward pool lists
     * {@code {"pokemon": ..., "weight": ...}} objects. Absent means {@link WarpRideEncounterPool#EMPTY}.
     */
    private static WarpRideEncounterPool readPool(ResourceLocation fileId, JsonObject json, String key,
                                                  boolean weighted) {
        if (!json.has(key)) {
            return WarpRideEncounterPool.EMPTY;
        }
        if (!json.get(key).isJsonObject()) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} {} is not an object; ignored", fileId, key);
            return WarpRideEncounterPool.EMPTY;
        }
        JsonObject pool = json.getAsJsonObject(key);
        List<WarpRideEncounterPool.Entry> entries = new ArrayList<>();
        if (pool.has("pokemon") && pool.get("pokemon").isJsonArray()) {
            JsonArray array = pool.getAsJsonArray("pokemon");
            for (int i = 0; i < array.size(); i++) {
                WarpRideEncounterPool.Entry entry = weighted
                        ? readWeightedEntry(array.get(i))
                        : readPlainEntry(array.get(i));
                if (entry == null) {
                    CobbleSafari.LOGGER.warn("[WarpRide] Destination {} {}.pokemon[{}] is malformed{}; dropped",
                            fileId, key, i, weighted ? " (expects a 'pokemon' string and a 'weight' >= 1)" : "");
                } else {
                    entries.add(entry);
                }
            }
        }
        return new WarpRideEncounterPool(entries, readOptionalString(pool, "fallback"));
    }

    private static WarpRideEncounterPool.Entry readPlainEntry(JsonElement element) {
        if (!element.isJsonPrimitive()) {
            return null;
        }
        String value = element.getAsString().trim();
        return value.isEmpty() ? null : new WarpRideEncounterPool.Entry(value, 1);
    }

    private static WarpRideEncounterPool.Entry readWeightedEntry(JsonElement element) {
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();
        String value = readOptionalString(obj, "pokemon");
        if (value == null || !obj.has("weight") || !obj.get("weight").isJsonPrimitive()
                || !obj.get("weight").getAsJsonPrimitive().isNumber()) {
            return null;
        }
        int weight = obj.get("weight").getAsInt();
        return weight < 1 ? null : new WarpRideEncounterPool.Entry(value, weight);
    }

    /** The optional per-destination override of the reward-pool chance (plan 190, E2). */
    private static double[] readRewardPoolPercent(ResourceLocation fileId, JsonObject json) {
        if (!json.has("rewardPoolPercent")) {
            return null;
        }
        if (!json.get("rewardPoolPercent").isJsonArray()
                || json.getAsJsonArray("rewardPoolPercent").size() != WarpRideDifficulty.RANK_COUNT) {
            CobbleSafari.LOGGER.warn("[WarpRide] Destination {} rewardPoolPercent must be {} numbers; "
                    + "ignored, the difficulty.json row applies", fileId, WarpRideDifficulty.RANK_COUNT);
            return null;
        }
        JsonArray array = json.getAsJsonArray("rewardPoolPercent");
        double[] out = new double[WarpRideDifficulty.RANK_COUNT];
        for (int rank = 0; rank < out.length; rank++) {
            out[rank] = Math.clamp(array.get(rank).getAsDouble(), 0.0, 100.0);
        }
        return out;
    }

    /**
     * One line per destination naming the entries that cannot be spawned right now, and why (plan
     * 190, E4). Informational only - the roll re-checks - but it is what answers "why do I only ever
     * get Poipole?" without reading code.
     */
    private static void reportUnavailableEntries() {
        for (WarpRideDestination destination : WarpRideDestinationRegistry.all()) {
            List<String> unavailable = new ArrayList<>();
            collectUnavailable(destination.normalPool(), "normal", unavailable);
            collectUnavailable(destination.rewardPool(), "reward", unavailable);
            if (!unavailable.isEmpty()) {
                CobbleSafari.LOGGER.info("[WarpRide] Destination {}: {} entr{} currently unavailable, "
                                + "skipped at the roll: {}", destination.destinationId(), unavailable.size(),
                        unavailable.size() == 1 ? "y" : "ies", String.join(", ", unavailable));
            }
        }
    }

    private static void collectUnavailable(WarpRideEncounterPool pool, String label, List<String> into) {
        for (WarpRideEncounterPool.Entry entry : pool.entries()) {
            describeUnavailable(entry.pokemon(), label, into);
        }
        if (pool.fallback() != null) {
            describeUnavailable(pool.fallback(), label + " fallback", into);
        }
    }

    private static void describeUnavailable(String pokemon, String label, List<String> into) {
        WarpRideEncounterPool.Availability availability = WarpRideEncounterPool.availability(pokemon);
        if (availability == WarpRideEncounterPool.Availability.NOT_FOUND) {
            into.add(pokemon + " (" + label + ", not found)");
        } else if (availability == WarpRideEncounterPool.Availability.NOT_IMPLEMENTED) {
            into.add(pokemon + " (" + label + ", not implemented)");
        }
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
