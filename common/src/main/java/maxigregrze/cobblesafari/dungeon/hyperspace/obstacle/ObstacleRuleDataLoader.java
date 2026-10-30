package maxigregrze.cobblesafari.dungeon.hyperspace.obstacle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads {@code data/<ns>/obstacle_rules/*.json} (action plan 196, § 8.2) and resets the cache of
 * move field effects, so {@code /reload} picks up edited action effects too.
 */
public final class ObstacleRuleDataLoader {

    private static final String PREFIX = "obstacle_rules";
    private static final Map<ObstacleKind, ObstacleRule> RULES = new EnumMap<>(ObstacleKind.class);

    private ObstacleRuleDataLoader() {}

    @Nullable
    public static ObstacleRule get(ObstacleKind kind) {
        return RULES.get(kind);
    }

    public static void load(MinecraftServer server) {
        RULES.clear();
        MoveFieldEffect.clearCache();
        Map<ResourceLocation, Resource> resources = server.getResourceManager()
                .listResources(PREFIX, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            String source = entry.getKey().toString();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                JsonElement root = JsonParser.parseReader(reader);
                ObstacleRule rule = root.isJsonObject() ? parse(root.getAsJsonObject(), source) : null;
                if (rule != null) {
                    RULES.put(rule.kind(), rule);
                }
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[Obstacles] Failed to load {}", source, e);
            }
        }
        for (ObstacleKind kind : ObstacleKind.values()) {
            if (!RULES.containsKey(kind)) {
                CobbleSafari.LOGGER.warn("[Obstacles] no rule for '{}': it cannot be cleared", kind.blockName());
            }
        }
        CobbleSafari.LOGGER.info("[Obstacles] Loaded {} obstacle rule(s)", RULES.size());
    }

    private static ObstacleRule parse(JsonObject json, String source) {
        ResourceLocation obstacle = json.has("obstacle")
                ? ResourceLocation.tryParse(json.get("obstacle").getAsString().trim()) : null;
        ObstacleKind kind = null;
        for (ObstacleKind k : ObstacleKind.values()) {
            if (obstacle != null && CobbleSafari.MOD_ID.equals(obstacle.getNamespace())
                    && k.blockName().equals(obstacle.getPath())) {
                kind = k;
            }
        }
        if (kind == null) {
            CobbleSafari.LOGGER.warn("[Obstacles] {} names no known obstacle block - ignored", source);
            return null;
        }
        String message = json.has("message") ? json.get("message").getAsString().trim()
                : "cobblesafari.obstacle.no_move." + kind.id();
        ResourceLocation loot = json.has("lootTable")
                ? ResourceLocation.tryParse(json.get("lootTable").getAsString().trim()) : null;
        boolean anyMove = json.has("anyMove") && json.get("anyMove").getAsBoolean();
        return new ObstacleRule(kind, readIds(json, "types"), readIds(json, "moves"), anyMove, message, loot);
    }

    private static Set<String> readIds(JsonObject json, String key) {
        Set<String> out = new HashSet<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray arr = json.getAsJsonArray(key);
            for (JsonElement el : arr) {
                if (el.isJsonPrimitive()) {
                    out.add(el.getAsString().trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return out;
    }
}
