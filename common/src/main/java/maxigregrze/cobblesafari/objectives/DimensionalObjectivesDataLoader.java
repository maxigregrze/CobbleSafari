package maxigregrze.cobblesafari.objectives;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads dimensional-objective definitions from {@code data/<ns>/dimensional_objectives/*.json}
 *. Modeled on {@code CsBossDataLoader}: strict validation, never throws; an invalid
 * file is skipped, an invalid task entry inside an otherwise-valid file is ignored.
 */
public final class DimensionalObjectivesDataLoader {

    private static final String PREFIX = "dimensional_objectives";
    private static final String DEFAULT_DISPLAY_NAME =
            "cobblesafari.dimensional_objectives.auspicious_pokeball_reward_display_name";

    private DimensionalObjectivesDataLoader() {}

    public static void load(MinecraftServer server) {
        DimensionalObjectivesRegistry.clear();
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
                DimensionalObjectivesDefinition def = parse(root.getAsJsonObject(), entry.getKey().toString());
                if (def == null) {
                    skipped++;
                    continue;
                }
                if (DimensionalObjectivesRegistry.has(def.dimensionId())) {
                    CobbleSafari.LOGGER.warn("[Objectives] dimension '{}' overwritten by {}",
                            def.dimensionId(), entry.getKey());
                }
                DimensionalObjectivesRegistry.register(def);
                ok++;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[Objectives] Failed to load {}", entry.getKey(), e);
                skipped++;
            }
        }
        CobbleSafari.LOGGER.info("[Objectives] Loaded {} dimension definition(s), {} skipped", ok, skipped);
        purgeOrphanedAssignments(server);
    }

    /** Drops persisted assignments whose dimension no longer has a definition after this (re)load (C8). */
    private static void purgeOrphanedAssignments(MinecraftServer server) {
        if (server.getLevel(net.minecraft.world.level.Level.OVERWORLD) == null) {
            return; // storage not available yet (very early load)
        }
        try {
            int purged = maxigregrze.cobblesafari.data.ObjectivesSavedData.get(server)
                    .purgeUndefinedDimensions(dim -> {
                        ResourceLocation rl = ResourceLocation.tryParse(dim);
                        return rl != null && DimensionalObjectivesRegistry.has(rl);
                    });
            if (purged > 0) {
                CobbleSafari.LOGGER.info("[Objectives] Purged {} orphaned assignment(s) for undefined dimensions", purged);
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[Objectives] Orphan assignment purge failed", e);
        }
    }

    private static DimensionalObjectivesDefinition parse(JsonObject json, String source) {
        if (!json.has("dimensionId")) {
            CobbleSafari.LOGGER.warn("[Objectives] {} missing dimensionId", source);
            return null;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(json.get("dimensionId").getAsString().trim());
        if (dimensionId == null) {
            CobbleSafari.LOGGER.warn("[Objectives] {} invalid dimensionId", source);
            return null;
        }

        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
        if (!enabled) {
            DimensionalObjectivesRegistry.unregister(dimensionId);
            CobbleSafari.LOGGER.info("[Objectives] {} disabled for dimension '{}'", source, dimensionId);
            return null;
        }

        boolean instanciated = json.has("isInstanciated") && json.get("isInstanciated").getAsBoolean();
        boolean auspicious = json.has("enableFinalCompletionAuspiciousReward")
                && json.get("enableFinalCompletionAuspiciousReward").getAsBoolean();
        boolean finalReward = !json.has("enableFinalCompletionReward")
                || json.get("enableFinalCompletionReward").getAsBoolean();

        String displayName = json.has("auspiciousPokeballRewardDisplayName")
                && !json.get("auspiciousPokeballRewardDisplayName").getAsString().isBlank()
                ? json.get("auspiciousPokeballRewardDisplayName").getAsString().trim()
                : DEFAULT_DISPLAY_NAME;

        ResourceLocation fallback = optionalLootTable(json, "fallbackCompletionReward", source);
        if (auspicious && fallback == null) {
            CobbleSafari.LOGGER.warn("[Objectives] {} enables auspicious reward but has no valid fallbackCompletionReward", source);
            return null;
        }

        ResourceLocation finalTable = optionalLootTable(json, "finalCompletionReward", source);
        if (finalReward && finalTable == null) {
            CobbleSafari.LOGGER.warn("[Objectives] {} enables final reward but finalCompletionReward is empty/invalid", source);
            return null;
        }

        if (!json.has("tasks") || !json.get("tasks").isJsonArray()) {
            CobbleSafari.LOGGER.warn("[Objectives] {} missing tasks array", source);
            return null;
        }
        List<DimensionalObjectivesDefinition.TaskPoolEntry> tasks = new ArrayList<>();
        for (JsonElement el : json.getAsJsonArray("tasks")) {
            if (!el.isJsonObject()) {
                continue;
            }
            DimensionalObjectivesDefinition.TaskPoolEntry entry = parseTask(el.getAsJsonObject(), source);
            if (entry != null) {
                tasks.add(entry);
            }
        }
        if (tasks.isEmpty()) {
            CobbleSafari.LOGGER.warn("[Objectives] {} has no valid task entries", source);
            return null;
        }

        return new DimensionalObjectivesDefinition(dimensionId, instanciated, auspicious, displayName,
                fallback, finalReward, finalTable, tasks);
    }

    private static DimensionalObjectivesDefinition.TaskPoolEntry parseTask(JsonObject json, String source) {
        if (!json.has("taskId") || !json.has("weight")) {
            CobbleSafari.LOGGER.warn("[Objectives] {} task entry missing taskId/weight — ignored", source);
            return null;
        }
        TaskType type = TaskType.byId(json.get("taskId").getAsString().trim());
        if (type == null) {
            CobbleSafari.LOGGER.warn("[Objectives] {} unknown taskId '{}' — ignored", source, json.get("taskId").getAsString());
            return null;
        }
        if (type.isDeferred()) {
            CobbleSafari.LOGGER.warn("[Objectives] {} task '{}' is reserved (Cobblemon 1.8) — ignored", source, type.id());
            return null;
        }
        int weight = json.get("weight").getAsInt();
        if (weight <= 0) {
            CobbleSafari.LOGGER.warn("[Objectives] {} task '{}' weight must be > 0 — ignored", source, type.id());
            return null;
        }

        int countMin = 1;
        int countMax = 9;
        if (json.has("countRange") && json.get("countRange").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("countRange");
            if (arr.size() == 2) {
                countMin = clampCount(arr.get(0).getAsInt());
                countMax = clampCount(arr.get(1).getAsInt());
                if (countMin > countMax) {
                    int t = countMin;
                    countMin = countMax;
                    countMax = t;
                }
            }
        }

        List<String> allowedSpecies = readStringList(json, "allowedSpecies");
        if (allowedSpecies.isEmpty()) {
            allowedSpecies = new ArrayList<>(List.of("random"));
        }

        List<Integer> allowedTypes = readTypeIndices(json);

        ResourceLocation taskReward = optionalLootTable(json, "taskReward", source);

        return new DimensionalObjectivesDefinition.TaskPoolEntry(
                type, weight, countMin, countMax, allowedSpecies, allowedTypes, taskReward);
    }

    private static int clampCount(int v) {
        return Math.max(1, Math.min(9, v));
    }

    private static List<Integer> readTypeIndices(JsonObject json) {
        List<Integer> indices = new ArrayList<>();
        if (json.has("allowedTypes") && json.get("allowedTypes").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("allowedTypes")) {
                if (!el.isJsonPrimitive()) {
                    continue;
                }
                int idx = typeIndexFromName(el.getAsString().trim());
                if (idx >= 0 && !indices.contains(idx)) {
                    indices.add(idx);
                }
            }
        }
        if (indices.isEmpty()) {
            for (int i = 0; i < PowerVariantRegistry.ELEMENTAL_COUNT; i++) {
                indices.add(i);
            }
        }
        return indices;
    }

    /** Resolves a Showdown type id (e.g. {@code "fire"}) to its variant index, or {@code -1}. */
    public static int typeIndexFromName(String name) {
        if (name == null || name.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < PowerVariantRegistry.ELEMENTAL_COUNT; i++) {
            if (PowerVariantRegistry.suffix(i).equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> readStringList(JsonObject json, String key) {
        List<String> out = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray(key)) {
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

    private static ResourceLocation optionalLootTable(JsonObject json, String key, String source) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) {
            return null;
        }
        ResourceLocation loc = ResourceLocation.tryParse(json.get(key).getAsString().trim());
        if (loc == null) {
            CobbleSafari.LOGGER.warn("[Objectives] {} invalid loot table id for '{}'", source, key);
        }
        return loc;
    }
}
