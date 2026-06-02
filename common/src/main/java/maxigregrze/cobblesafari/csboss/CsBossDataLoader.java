package maxigregrze.cobblesafari.csboss;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.BossEvent;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Charge les définitions CSBoss depuis {@code data/<ns>/csboss/*.json} (plan 100 § 2.2).
 * Calqué sur {@code GtsDataLoader} : validations strictes, jamais d'exception remontée.
 */
public final class CsBossDataLoader {
    private static final String PREFIX = "csboss";
    private static final Pattern BOSS_ID_PATTERN = Pattern.compile("[a-z0-9._-]+");

    private CsBossDataLoader() {}

    public static void load(MinecraftServer server) {
        CsBossRegistry.clear();
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
                CsBossDefinition def = parse(root.getAsJsonObject(), entry.getKey().toString());
                if (def == null) {
                    skipped++;
                    continue;
                }
                if (CsBossRegistry.has(def.bossId())) {
                    CobbleSafari.LOGGER.warn("[CSBoss] bossId '{}' overwritten by {}", def.bossId(), entry.getKey());
                }
                CsBossRegistry.register(def);
                ok++;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[CSBoss] Failed to load {}", entry.getKey(), e);
                skipped++;
            }
        }
        CobbleSafari.LOGGER.info("[CSBoss] Loaded {} boss definition(s), {} skipped", ok, skipped);
    }

    private static CsBossDefinition parse(JsonObject json, String source) {
        if (!json.has("bossId") || !json.has("maximumDuration")
                || !json.has("minimumDuration") || !json.has("specie") || !json.has("rewards")) {
            CobbleSafari.LOGGER.warn("[CSBoss] {} missing required field(s)", source);
            return null;
        }

        String bossId = json.get("bossId").getAsString().trim();
        if (bossId.isEmpty() || !BOSS_ID_PATTERN.matcher(bossId).matches()) {
            CobbleSafari.LOGGER.warn("[CSBoss] {} invalid bossId '{}'", source, bossId);
            return null;
        }

        int maximumDuration = json.get("maximumDuration").getAsInt();
        int minimumDuration = json.get("minimumDuration").getAsInt();
        if (minimumDuration < 1 || maximumDuration < minimumDuration) {
            CobbleSafari.LOGGER.warn("[CSBoss] {} bad durations (min={}, max={})", source, minimumDuration, maximumDuration);
            return null;
        }

        String specie = json.get("specie").getAsString().trim();
        if (specie.isEmpty() || specie.toLowerCase(Locale.ROOT).contains("random")) {
            CobbleSafari.LOGGER.warn("[CSBoss] {} invalid specie '{}' (empty or random)", source, specie);
            return null;
        }
        try {
            PokemonProperties props = PokemonProperties.Companion.parse(specie);
            if (props.getSpecies() == null) {
                CobbleSafari.LOGGER.warn("[CSBoss] {} specie line has no species: {}", source, specie);
                return null;
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.warn("[CSBoss] {} could not parse specie '{}'", source, specie, e);
            return null;
        }

        ResourceLocation rewards = ResourceLocation.tryParse(json.get("rewards").getAsString().trim());
        if (rewards == null) {
            CobbleSafari.LOGGER.warn("[CSBoss] {} invalid rewards loot table id", source);
            return null;
        }

        List<String> tags = readStringList(json, "tags");
        List<String> moveSet = readStringList(json, "moveSet");

        int size = json.has("size") ? Math.max(1, json.get("size").getAsInt()) : CsBossDefinition.DEFAULT_SIZE;
        int cdMin = json.has("moveCooldownMin")
                ? Math.max(1, json.get("moveCooldownMin").getAsInt()) : CsBossDefinition.DEFAULT_COOLDOWN_MIN;
        int cdMax = json.has("moveCooldownMax")
                ? Math.max(cdMin, json.get("moveCooldownMax").getAsInt()) : Math.max(cdMin, CsBossDefinition.DEFAULT_COOLDOWN_MAX);
        boolean isStatic = !json.has("isStatic") || json.get("isStatic").getAsBoolean();

        ResourceLocation uniqueReward = null;
        if (json.has("uniqueReward") && !json.get("uniqueReward").getAsString().isBlank()) {
            uniqueReward = ResourceLocation.tryParse(json.get("uniqueReward").getAsString().trim());
            if (uniqueReward == null) {
                CobbleSafari.LOGGER.warn("[CSBoss] {} invalid uniqueReward id — ignored", source);
            }
        }

        String music = json.has("music") && !json.get("music").getAsString().isBlank()
                ? json.get("music").getAsString().trim() : null;

        String displayName = readDisplayName(json);

        BossEvent.BossBarOverlay overlay = parseOverlay(json.has("healthStyle")
                ? json.get("healthStyle").getAsString() : null, source);
        BossEvent.BossBarColor color = parseColor(json.has("healthColor")
                ? json.get("healthColor").getAsString() : null, source);

        return new CsBossDefinition(bossId, displayName, tags, maximumDuration, minimumDuration, specie, size,
                moveSet, cdMin, cdMax, isStatic, uniqueReward, rewards, music, overlay, color);
    }

    /** Lit le nom affiché ({@code displayName}, alias toléré {@code displayname}). */
    private static String readDisplayName(JsonObject json) {
        for (String key : new String[]{"displayName", "displayname"}) {
            if (json.has(key) && json.get(key).isJsonPrimitive()) {
                String val = json.get(key).getAsString().trim();
                if (!val.isEmpty()) {
                    return val;
                }
            }
        }
        return null;
    }

    private static List<String> readStringList(JsonObject json, String key) {
        List<String> out = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray arr = json.getAsJsonArray(key);
            for (JsonElement el : arr) {
                if (el.isJsonPrimitive()) {
                    String s = el.getAsString().trim();
                    if (!s.isEmpty()) {
                        out.add(s);
                    }
                }
            }
        }
        return out;
    }

    private static BossEvent.BossBarOverlay parseOverlay(String raw, String source) {
        if (raw == null) {
            return BossEvent.BossBarOverlay.PROGRESS;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "notched_6" -> BossEvent.BossBarOverlay.NOTCHED_6;
            case "notched_10" -> BossEvent.BossBarOverlay.NOTCHED_10;
            case "notched_12" -> BossEvent.BossBarOverlay.NOTCHED_12;
            case "notched_20" -> BossEvent.BossBarOverlay.NOTCHED_20;
            case "progress" -> BossEvent.BossBarOverlay.PROGRESS;
            default -> {
                CobbleSafari.LOGGER.warn("[CSBoss] {} unknown healthStyle '{}' — default progress", source, raw);
                yield BossEvent.BossBarOverlay.PROGRESS;
            }
        };
    }

    private static BossEvent.BossBarColor parseColor(String raw, String source) {
        if (raw == null) {
            return BossEvent.BossBarColor.PURPLE;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "pink" -> BossEvent.BossBarColor.PINK;
            case "blue" -> BossEvent.BossBarColor.BLUE;
            case "red" -> BossEvent.BossBarColor.RED;
            case "green" -> BossEvent.BossBarColor.GREEN;
            case "yellow" -> BossEvent.BossBarColor.YELLOW;
            case "purple" -> BossEvent.BossBarColor.PURPLE;
            case "white" -> BossEvent.BossBarColor.WHITE;
            default -> {
                CobbleSafari.LOGGER.warn("[CSBoss] {} unknown healthColor '{}' — default purple", source, raw);
                yield BossEvent.BossBarColor.PURPLE;
            }
        };
    }
}
