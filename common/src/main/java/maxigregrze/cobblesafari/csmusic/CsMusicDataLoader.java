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
 * Loads csmusic definitions from {@code data/<ns>/csmusic/*.json}.
 * Modeled on {@code CsBossDataLoader}: strict validation, never throws.
 * Sound ids are not validated here (they live in the client resource pack).
 */
public final class CsMusicDataLoader {
    private static final String PREFIX = "csmusic";
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9._-]+");

    private CsMusicDataLoader() {}

    public static void load(MinecraftServer server) {
        CsMusicRegistry.clear();
        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources =
                manager.listResources(PREFIX, id -> id.getPath().endsWith(".json"));
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
        int priority = json.has("priority")
                ? Math.max(1, json.get("priority").getAsInt()) : CsMusicDefinition.DEFAULT_PRIORITY;

        String parent = json.has("parent") && !json.get("parent").getAsString().isBlank()
                ? json.get("parent").getAsString().trim() : null;

        Set<String> tags = new LinkedHashSet<>();
        if (json.has("tags") && json.get("tags").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("tags")) {
                String tag = e.getAsString().trim().toLowerCase(Locale.ROOT);
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
        }

        return new CsMusicDefinition(id, loop, intro, outro, priority, parent, tags);
    }

    private static ResourceLocation optionalSound(JsonObject json, String key, ResourceLocation file) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(json.get(key).getAsString().trim());
        if (rl == null) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} invalid '{}' sound id — ignored", file, key);
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
