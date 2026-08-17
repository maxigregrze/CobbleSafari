package maxigregrze.cobblesafari.csmusic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Loads csmusic definitions from the initialisation files {@code data/<ns>/csmusic/*.json}.
 * Modeled on {@code CsBossDataLoader}: strict validation, never throws.
 * Sound ids are not validated here (they live in the client resource pack).
 */
public final class CsMusicDataLoader {
    private static final String PREFIX = "csmusic";
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9._-]+");

    private CsMusicDataLoader() {}

    /**
     * Reads whatever the server currently exposes. On the reload path this is only correct once the
     * server has swapped its {@code resources} field, which is why both loaders defer to the tick
     * following a datapack reload rather than running inside it - see
     * {@code DimensionEvents#onDatapackReload}.
     */
    public static void load(MinecraftServer server) {
        ResourceManager manager = server.getResourceManager();
        CsMusicRegistry.clear();
        // listResources recurses, so csmusic/<SUBDIR>/ (the definition files) must be excluded here -
        // otherwise every definition file is parsed as an initialisation file and warns about a
        // missing 'loop'.
        String triggerDir = "/" + CsMusicTriggerDataLoader.SUBDIR + "/";
        Map<ResourceLocation, Resource> resources = manager.listResources(PREFIX,
                id -> id.getPath().endsWith(".json") && !id.getPath().contains(triggerDir));
        int ok = 0;
        int skipped = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    skipped++;
                    continue;
                }
                CsMusicDefinition def = parse(entry.getKey(), root.getAsJsonObject());
                if (def == null) {
                    skipped++;
                    continue;
                }
                CsMusicRegistry.register(def);
                ok++;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[CSMusic] Failed to load {}", entry.getKey(), e);
                skipped++;
            }
        }
        CsMusicRegistry.validateRelations();
        CobbleSafari.LOGGER.info("[CSMusic] Loaded {} music definition(s), {} skipped", ok, skipped);
    }

    private static CsMusicDefinition parse(ResourceLocation file, JsonObject json) {
        String name = nameFromFile(file);
        if (name == null || !ID_PATTERN.matcher(name).matches()) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} invalid music id", file);
            return null;
        }
        // fully qualified id: <namespace>:<filename> (e.g. cobblesafari:underground)
        String id = file.getNamespace() + ":" + name;
        if (!json.has("loop")) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} missing required 'loop'", file);
            return null;
        }
        ResourceLocation loop = ResourceLocation.tryParse(json.get("loop").getAsString().trim());
        if (loop == null) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} invalid 'loop' sound id", file);
            return null;
        }
        ResourceLocation intro = optionalSound(json, "intro", file);
        ResourceLocation outro = optionalSound(json, "outro", file);

        // Dead since the trigger rework, and silently accepting it lets an author believe the value
        // is doing something - /csmusic list used to echo it right back at them.
        if (json.has("priority")) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} 'priority' is ignored since the trigger rework -"
                    + " priority now lives on the trigger rule and on the area", file);
        }

        String parent = json.has("parent") && !json.get("parent").getAsString().isBlank()
                ? json.get("parent").getAsString().trim() : null;

        return new CsMusicDefinition(id, loop, intro, outro, parent, parseTags(json, file));
    }

    /**
     * Tags must be an array of strings. Any other shape used to be dropped without a word, so a
     * {@code "tags": "route"} left the track out of its pool with nothing in the log to explain it.
     */
    private static Set<String> parseTags(JsonObject json, ResourceLocation file) {
        Set<String> tags = new LinkedHashSet<>();
        if (!json.has("tags")) {
            return tags;
        }
        JsonElement raw = json.get("tags");
        if (!raw.isJsonArray()) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} 'tags' must be an array of strings (found {}) -"
                    + " no tag applied, this track will not appear in any tag pool",
                    file, raw.isJsonPrimitive() ? "a single value" : "an object");
            return tags;
        }
        for (JsonElement e : raw.getAsJsonArray()) {
            if (!e.isJsonPrimitive()) {
                CobbleSafari.LOGGER.warn("[CSMusic] {} 'tags' holds a non-string entry - ignored", file);
                continue;
            }
            String tag = e.getAsString().trim().toLowerCase(Locale.ROOT);
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private static ResourceLocation optionalSound(JsonObject json, String key, ResourceLocation file) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(json.get(key).getAsString().trim());
        if (rl == null) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} invalid '{}' sound id - ignored", file, key);
        }
        return rl;
    }

    /** {@code.../csmusic/<name>.json} → {@code name}. */
    private static String nameFromFile(ResourceLocation file) {
        String path = file.getPath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.endsWith(".json") ? name.substring(0, name.length() - ".json".length()) : name;
    }
}
