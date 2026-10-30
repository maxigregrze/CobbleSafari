package maxigregrze.cobblesafari.dungeon.hyperspace;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kotlin.ranges.IntRange;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads {@code data/<ns>/hyperspace_clusters/*.json} (action plan 196, § 2.3). Never throws: an
 * invalid file is skipped, an invalid cluster or member is dropped with a WARN naming the file.
 */
public final class HyperspaceClusterDataLoader {

    private static final String PREFIX = "hyperspace_clusters";
    private static final String DEFAULT_POSITION = "grounded";

    private HyperspaceClusterDataLoader() {}

    public static void load(MinecraftServer server) {
        HyperspaceClusterRegistry.clear();
        Map<ResourceLocation, Resource> resources = server.getResourceManager()
                .listResources(PREFIX, id -> id.getPath().endsWith(".json"));
        int clusters = 0;
        int skipped = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            String source = entry.getKey().toString();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    skipped++;
                    continue;
                }
                List<HyperspaceCluster> parsed = parseFile(root.getAsJsonObject(), source);
                if (parsed == null) {
                    skipped++;
                    continue;
                }
                parsed.forEach(HyperspaceClusterRegistry::register);
                clusters += parsed.size();
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[Hyperspace] Failed to load {}", source, e);
                skipped++;
            }
        }
        CobbleSafari.LOGGER.info("[Hyperspace] Loaded {} cluster(s) from {} file(s), {} file(s) skipped",
                clusters, resources.size() - skipped, skipped);
    }

    private static List<HyperspaceCluster> parseFile(JsonObject json, String source) {
        if (!json.has("star") || !json.has("type") || !json.has("clusters") || !json.get("clusters").isJsonArray()) {
            CobbleSafari.LOGGER.warn("[Hyperspace] {} needs star, type and a clusters array", source);
            return null;
        }
        int star = json.get("star").getAsInt();
        String type = json.get("type").getAsString().trim().toLowerCase(Locale.ROOT);
        if (star < 1 || star > 5 || type.isEmpty()) {
            CobbleSafari.LOGGER.warn("[Hyperspace] {} has an invalid star or type", source);
            return null;
        }
        List<HyperspaceCluster> out = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (JsonElement el : json.getAsJsonArray("clusters")) {
            if (!el.isJsonObject()) {
                continue;
            }
            HyperspaceCluster cluster = parseCluster(el.getAsJsonObject(), star, type, source);
            if (cluster == null) {
                continue;
            }
            if (!ids.add(cluster.id())) {
                CobbleSafari.LOGGER.warn("[Hyperspace] {} duplicate cluster id '{}' - ignored", source, cluster.id());
                continue;
            }
            out.add(cluster);
        }
        return out;
    }

    private static HyperspaceCluster parseCluster(JsonObject json, int star, String type, String source) {
        if (!json.has("id") || !json.has("members") || !json.get("members").isJsonArray()) {
            CobbleSafari.LOGGER.warn("[Hyperspace] {} cluster without id or members - ignored", source);
            return null;
        }
        String id = json.get("id").getAsString().trim().toLowerCase(Locale.ROOT);
        double weight = json.has("weight") ? json.get("weight").getAsDouble() : 1.0;
        if (id.isEmpty() || !ResourceLocation.isValidPath(id) || weight <= 0) {
            CobbleSafari.LOGGER.warn("[Hyperspace] {} cluster '{}' has an invalid id or weight - ignored", source, id);
            return null;
        }
        List<HyperspaceCluster.Member> members = new ArrayList<>();
        for (JsonElement el : json.getAsJsonArray("members")) {
            if (el.isJsonObject()) {
                HyperspaceCluster.Member member = parseMember(el.getAsJsonObject(), id, source);
                if (member != null) {
                    members.add(member);
                }
            }
        }
        if (members.isEmpty()) {
            CobbleSafari.LOGGER.warn("[Hyperspace] {} cluster '{}' has no valid member - ignored", source, id);
            return null;
        }
        return new HyperspaceCluster(id, star, type, weight, members);
    }

    private static HyperspaceCluster.Member parseMember(JsonObject json, String clusterId, String source) {
        if (!json.has("pokemon") || !json.has("rarity") || !json.has("level") || !json.has("alphaLevel")) {
            CobbleSafari.LOGGER.warn("[Hyperspace] {} cluster '{}': member needs pokemon, rarity, level, alphaLevel",
                    source, clusterId);
            return null;
        }
        String pokemon = json.get("pokemon").getAsString().trim().toLowerCase(Locale.ROOT);
        int rarity = json.get("rarity").getAsInt();
        IntRange level = parseRange(json.get("level").getAsString());
        IntRange alphaLevel = parseRange(json.get("alphaLevel").getAsString());
        String position = json.has("position") ? json.get("position").getAsString().trim() : DEFAULT_POSITION;
        String speciesName = pokemon.split(" ", 2)[0];
        if (PokemonSpecies.INSTANCE.getByName(speciesName) == null) {
            CobbleSafari.LOGGER.warn("[Hyperspace] {} cluster '{}': unknown species '{}' - ignored",
                    source, clusterId, speciesName);
            return null;
        }
        if (rarity <= 0 || level == null || alphaLevel == null) {
            CobbleSafari.LOGGER.warn("[Hyperspace] {} cluster '{}': '{}' has an invalid rarity or level range",
                    source, clusterId, pokemon);
            return null;
        }
        return new HyperspaceCluster.Member(pokemon, rarity, level, alphaLevel, position);
    }

    /** Parses {@code "14-16"} or {@code "20"}; {@code null} when malformed or outside 1..100. */
    private static IntRange parseRange(String raw) {
        try {
            String[] parts = raw.trim().split("-");
            int lo = Integer.parseInt(parts[0].trim());
            int hi = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : lo;
            if (parts.length > 2 || lo < 1 || hi > 100 || lo > hi) {
                return null;
            }
            return new IntRange(lo, hi);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
