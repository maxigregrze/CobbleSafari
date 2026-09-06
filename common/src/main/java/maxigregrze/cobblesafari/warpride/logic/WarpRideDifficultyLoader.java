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

        return new WarpRideDifficulty(noBonusDistance, bands, readRewards(fileId, json));
    }

    /**
     * Reads the optional {@code rewards} block (action plan 184, section 3.4).
     *
     * <p>Its failure policy is deliberately <b>not</b> the bands' policy. A band whose
     * {@code bonusChance} does not ratchet to zero produces a ride that never ends, which is why a
     * malformed band rejects the whole file. No reward scale has that power - at worst it hands out
     * a Pokemon that is too weak or too strong. Rejecting the file over a three-entry array would
     * drop the <i>bands</i> back to the built-ins too, a punishment unrelated to the mistake. So the
     * granularity of the fallback is the field, and a missing block is silent: that is every
     * difficulty.json written before this plan.</p>
     */
    private static WarpRideRewards readRewards(ResourceLocation fileId, JsonObject json) {
        WarpRideRewards fallback = WarpRideRewards.defaults();
        if (!json.has("rewards")) {
            return fallback;
        }
        if (!json.get("rewards").isJsonObject()) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} has a 'rewards' entry that is not an object; "
                    + "using the built-in reward scale", fileId);
            return fallback;
        }
        JsonObject r = json.getAsJsonObject("rewards");

        int stepLy = readBoundedInt(fileId, r, "performanceStepLy", fallback.stepLy(), 1);
        int maxSteps = readBoundedInt(fileId, r, "performanceMaxSteps", fallback.maxSteps(), 0);

        return new WarpRideRewards(
                stepLy, maxSteps,
                readIntRow(fileId, r, "levelOffset", DEFAULT_LEVEL_OFFSET),
                readIntRow(fileId, r, "ivPerfectCount", DEFAULT_IV_PERFECT_COUNT),
                readPercentRow(fileId, r, "shinyPercentBase", DEFAULT_SHINY_BASE),
                readPercentRow(fileId, r, "shinyPercentPerStep", DEFAULT_SHINY_PER_STEP),
                readPercentRow(fileId, r, "hiddenAbilityPercentBase", DEFAULT_HA_BASE),
                readPercentRow(fileId, r, "hiddenAbilityPercentPerStep", DEFAULT_HA_PER_STEP),
                readPercentRow(fileId, r, "bonusMovePercentBase", DEFAULT_MOVE_BASE),
                readPercentRow(fileId, r, "bonusMovePercentPerStep", DEFAULT_MOVE_PER_STEP),
                readPercentRow(fileId, r, "secondMovePercentBase", DEFAULT_SECOND_MOVE_BASE),
                readPercentRow(fileId, r, "secondMovePercentPerStep", DEFAULT_SECOND_MOVE_PER_STEP),
                readPercentRow(fileId, r, "alphaPercentBase", DEFAULT_ALPHA_BASE),
                readPercentRow(fileId, r, "alphaPercentPerStep", DEFAULT_ALPHA_PER_STEP));
    }

    // The shipped rows, spelled out here rather than read back off WarpRideRewards.defaults():
    // that class exposes each scale only through its (rank, p) accessor, and inverting
    // base + perStep * p to recover two rows would be a fragile way to write a constant.
    private static final int[] DEFAULT_LEVEL_OFFSET = {-10, -5, 0, 5};
    private static final int[] DEFAULT_IV_PERFECT_COUNT = {3, 4, 5, 6};
    private static final double[] DEFAULT_SHINY_BASE = {1.0, 1.0, 1.0, 0.0};
    private static final double[] DEFAULT_SHINY_PER_STEP = {0.0, 1.0, 2.0, 4.0};
    private static final double[] DEFAULT_HA_BASE = {0.0, 1.0, 2.0, 4.0};
    private static final double[] DEFAULT_HA_PER_STEP = {0.0, 1.0, 2.0, 4.0};
    private static final double[] DEFAULT_MOVE_BASE = {0.0, 3.3, 6.6, 10.0};
    private static final double[] DEFAULT_MOVE_PER_STEP = {0.0, 3.3, 6.6, 10.0};
    private static final double[] DEFAULT_SECOND_MOVE_BASE = {0.0, 0.0, 0.0, 5.0};
    private static final double[] DEFAULT_SECOND_MOVE_PER_STEP = {0.0, 0.0, 0.0, 5.0};
    private static final double[] DEFAULT_ALPHA_BASE = {0.0, 1.0, 2.0, 3.0};
    private static final double[] DEFAULT_ALPHA_PER_STEP = {0.0, 1.0, 2.0, 3.0};

    private static int readBoundedInt(ResourceLocation fileId, JsonObject rewards, String key,
                                      int fallback, int minimum) {
        if (!rewards.has(key)) {
            return fallback;
        }
        JsonElement element = rewards.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} rewards.{} is not a number; using {}",
                    fileId, key, fallback);
            return fallback;
        }
        int value = element.getAsInt();
        if (value < minimum) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} rewards.{} = {} is below {}; using {}",
                    fileId, key, value, minimum, fallback);
            return fallback;
        }
        return value;
    }

    private static int[] readIntRow(ResourceLocation fileId, JsonObject rewards, String key,
                                    int[] fallback) {
        JsonArray array = rankArray(fileId, rewards, key);
        if (array == null) {
            return fallback;
        }
        int[] out = new int[WarpRideDifficulty.RANK_COUNT];
        for (int rank = 0; rank < out.length; rank++) {
            out[rank] = array.get(rank).getAsInt();
        }
        return out;
    }

    private static double[] readPercentRow(ResourceLocation fileId, JsonObject rewards, String key,
                                           double[] fallback) {
        JsonArray array = rankArray(fileId, rewards, key);
        if (array == null) {
            return fallback;
        }
        double[] out = new double[WarpRideDifficulty.RANK_COUNT];
        for (int rank = 0; rank < out.length; rank++) {
            double value = array.get(rank).getAsDouble();
            double clamped = Math.max(0.0, Math.min(100.0, value));
            if (Double.compare(clamped, value) != 0) {
                CobbleSafari.LOGGER.warn("[WarpRide] {} rewards.{}[{}] = {} is outside 0..100; clamped to {}",
                        fileId, key, rank, value, clamped);
            }
            out[rank] = clamped;
        }
        return out;
    }

    /** @return the array when present and exactly {@code RANK_COUNT} numbers long, else null. */
    private static JsonArray rankArray(ResourceLocation fileId, JsonObject rewards, String key) {
        if (!rewards.has(key)) {
            CobbleSafari.LOGGER.debug("[WarpRide] {} rewards.{} is absent; using the built-in row",
                    fileId, key);
            return null;
        }
        if (!rewards.get(key).isJsonArray()) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} rewards.{} is not an array; using the built-in row",
                    fileId, key);
            return null;
        }
        JsonArray array = rewards.getAsJsonArray(key);
        if (array.size() != WarpRideDifficulty.RANK_COUNT) {
            CobbleSafari.LOGGER.warn("[WarpRide] {} rewards.{} has {} entries instead of {}; "
                    + "using the built-in row", fileId, key, array.size(), WarpRideDifficulty.RANK_COUNT);
            return null;
        }
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                CobbleSafari.LOGGER.warn("[WarpRide] {} rewards.{}[{}] is not a number; "
                        + "using the built-in row", fileId, key, i);
                return null;
            }
        }
        return array;
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
