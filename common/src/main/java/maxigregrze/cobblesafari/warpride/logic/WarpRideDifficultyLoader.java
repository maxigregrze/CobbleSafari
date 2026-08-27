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
import java.util.List;
import java.util.Map;

/**
 * Loads {@code data/cobblesafari/ultra_warp_ride/difficulty.json}. Weight rows must sum to 1.0, and
 * every band at or past {@code noBonusDistance} must have {@code bonusChance == 0} — that ratchet is
 * the only guarantee a ride terminates. A rejected file falls back to the shipped balance.
 */
public final class WarpRideDifficultyLoader {

    private WarpRideDifficultyLoader() {}

    private static final String DATA_DIR = "ultra_warp_ride";
    private static final String FILE_NAME = "difficulty.json";
    private static final double SUM_EPSILON = 1.0e-4;

    private static WarpRideDifficulty active = WarpRideDifficulty.defaults();

    public static WarpRideDifficulty get() {
        return active;
    }

    public static void load(MinecraftServer server) {
        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources = manager.listResources(DATA_DIR,
                id -> id.getPath().endsWith(FILE_NAME));

        if (resources.isEmpty()) {
            active = WarpRideDifficulty.defaults();
            CobbleSafari.LOGGER.info("[WarpRide] No difficulty.json found; using the built-in table ({} bands)",
                    active.bands().size());
            return;
        }

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                WarpRideDifficulty parsed = parse(fileId, JsonParser.parseReader(reader));
                if (parsed != null) {
                    active = parsed;
                    CobbleSafari.LOGGER.info("[WarpRide] Loaded difficulty table from {} ({} bands, noBonusDistance={})",
                            fileId, parsed.bands().size(), parsed.noBonusDistance());
                    return;
                }
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[WarpRide] Failed to read {}", fileId, e);
            }
        }

        active = WarpRideDifficulty.defaults();
        CobbleSafari.LOGGER.warn("[WarpRide] difficulty.json was rejected; falling back to the built-in table");
    }

    private static WarpRideDifficulty parse(ResourceLocation fileId, JsonElement root) {
        if (!root.isJsonObject()) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} is not a JSON object", fileId);
            return null;
        }
        JsonObject json = root.getAsJsonObject();

        if (!json.has("noBonusDistance")) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} has no noBonusDistance; the termination guarantee of "
                    + "plan 180 § 3.8 cannot be checked, rejecting the file", fileId);
            return null;
        }
        int noBonusDistance = json.get("noBonusDistance").getAsInt();
        if (noBonusDistance <= 0) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} has noBonusDistance={} (must be > 0), rejecting",
                    fileId, noBonusDistance);
            return null;
        }

        if (!json.has("bands") || !json.get("bands").isJsonArray()) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} has no bands array", fileId);
            return null;
        }

        JsonArray bandsJson = json.getAsJsonArray("bands");
        List<WarpRideDifficulty.Band> bands = new ArrayList<>();
        int previousFrom = Integer.MIN_VALUE;

        for (int i = 0; i < bandsJson.size(); i++) {
            JsonObject b = bandsJson.get(i).getAsJsonObject();
            int from = b.has("fromDistance") ? b.get("fromDistance").getAsInt() : 0;
            if (from <= previousFrom) {
                CobbleSafari.LOGGER.warn("[WarpRide] {} band #{} has fromDistance={} which is not "
                        + "strictly increasing; rejecting the file", fileId, i, from);
                return null;
            }
            previousFrom = from;

            double[] rankWeights = readWeights(b, "rankWeights", WarpRideDifficulty.RANK_COUNT);
            double[] negativeWeights = readWeights(b, "negativeWeights",
                    WarpRideDifficulty.MAX_NEGATIVES_PER_WAVE + 1);
            if (rankWeights == null || negativeWeights == null) {
                CobbleSafari.LOGGER.warn("[WarpRide] {} band #{} (fromDistance={}) has a malformed "
                        + "weights array; rejecting the file", fileId, i, from);
                return null;
            }
            if (!sumsToOne(rankWeights)) {
                CobbleSafari.LOGGER.warn("[WarpRide] {} band #{} (fromDistance={}) has rankWeights "
                        + "summing to {} instead of 1.0; rejecting the file",
                        fileId, i, from, sum(rankWeights));
                return null;
            }
            if (!sumsToOne(negativeWeights)) {
                CobbleSafari.LOGGER.warn("[WarpRide] {} band #{} (fromDistance={}) has negativeWeights "
                        + "summing to {} instead of 1.0; rejecting the file",
                        fileId, i, from, sum(negativeWeights));
                return null;
            }

            double portalChance = b.has("portalChance") ? b.get("portalChance").getAsDouble() : 0.0;
            double bonusChance = b.has("bonusChance") ? b.get("bonusChance").getAsDouble() : 0.0;

            if (from >= noBonusDistance && bonusChance > 0.0) {
                CobbleSafari.LOGGER.warn("[WarpRide] {} band #{} (fromDistance={}) sits at or past "
                        + "noBonusDistance={} but still has bonusChance={}. The energy ratchet of "
                        + "plan 180 § 3.8 is the only thing that ends a ride: forcing it to 0",
                        fileId, i, from, noBonusDistance, bonusChance);
                bonusChance = 0.0;
            }

            bands.add(new WarpRideDifficulty.Band(from, rankWeights,
                    clamp01(portalChance), clamp01(bonusChance), negativeWeights));
        }

        if (bands.isEmpty()) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} declares no band", fileId);
            return null;
        }
        if (bands.get(0).fromDistance != 0) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} first band starts at {} instead of 0; rejecting",
                    fileId, bands.get(0).fromDistance);
            return null;
        }
        if (bands.get(bands.size() - 1).fromDistance < noBonusDistance) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} has no band at or past noBonusDistance={}; the "
                    + "bonus cutoff would never be reached, rejecting", fileId, noBonusDistance);
            return null;
        }

        return new WarpRideDifficulty(noBonusDistance, bands);
    }

    private static double[] readWeights(JsonObject band, String key, int expectedLength) {
        if (!band.has(key) || !band.get(key).isJsonArray()) {
            return null;
        }
        JsonArray array = band.getAsJsonArray(key);
        if (array.size() != expectedLength) {
            return null;
        }
        double[] out = new double[expectedLength];
        for (int i = 0; i < expectedLength; i++) {
            out[i] = Math.max(0.0, array.get(i).getAsDouble());
        }
        return out;
    }

    private static double sum(double[] values) {
        double total = 0.0;
        for (double v : values) {
            total += v;
        }
        return total;
    }

    private static boolean sumsToOne(double[] values) {
        return Math.abs(sum(values) - 1.0) <= SUM_EPSILON;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
