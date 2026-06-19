package maxigregrze.cobblesafari.chat;

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
import java.util.regex.Pattern;

/**
 * Loads {@code data/<ns>/chat_conversation/*.json} into {@link ChatConversationRegistry}
 *.
 *
 * <p><b>Strict validation:</b> any structural error logs an explicit reason to the server log and
 * <em>aborts the loading of that specific JSON</em> (the file is skipped, others keep loading). No
 * partially-valid conversation is ever registered. Referenced advancements / loot tables / offer
 * templates are <em>not</em> checked here (resolved at runtime).
 */
public final class ChatConversationDataLoader {

    private ChatConversationDataLoader() {}

    private static final String DATA_DIR = "chat_conversation";
    private static final int CURRENT_VERSION = 1;
    private static final Pattern SAFE_ID = Pattern.compile("^[a-z0-9_]+$");

    public static void load(MinecraftServer server) {
        ChatConversationRegistry.clear();
        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources =
                manager.listResources(DATA_DIR, id -> id.getPath().endsWith(".json"));

        int loaded = 0;
        int skipped = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                ChatConversationDefinition def = parse(fileId, reader);
                if (def == null) {
                    skipped++;
                    continue;
                }
                ChatConversationRegistry.register(def);
                loaded++;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[Chat] {} : failed to read/parse, skipped", fileId, e);
                skipped++;
            }
        }
        CobbleSafari.LOGGER.info("Loaded {} chat conversations ({} skipped)", loaded, skipped);
    }

    /** Logs the reason and returns {@code null} (= abort this JSON) on any structural error. */
    private static ChatConversationDefinition parse(ResourceLocation fileId, InputStreamReader reader) {
        JsonElement root = JsonParser.parseReader(reader);
        if (!root.isJsonObject()) {
            return reject(fileId, "not a JSON object");
        }
        JsonObject json = root.getAsJsonObject();

        // 2. schemaVersion
        if (!json.has("schemaVersion") || !json.get("schemaVersion").getAsJsonPrimitive().isNumber()) {
            return reject(fileId, "missing/invalid schemaVersion");
        }
        int schemaVersion = json.get("schemaVersion").getAsInt();
        if (schemaVersion > CURRENT_VERSION) {
            return reject(fileId, "unsupported schemaVersion " + schemaVersion);
        }

        // 3. id
        if (!json.has("id")) {
            return reject(fileId, "missing id");
        }
        String id = json.get("id").getAsString();
        if (id.isEmpty() || !SAFE_ID.matcher(id).matches()) {
            return reject(fileId, "invalid id '" + id + "' (expected ^[a-z0-9_]+$)");
        }

        // 4. displayName
        if (!json.has("displayName") || json.get("displayName").getAsString().isEmpty()) {
            return reject(fileId, "missing displayName");
        }
        String displayName = json.get("displayName").getAsString();

        // 5. displayPriority
        if (!json.has("displayPriority") || !json.get("displayPriority").getAsJsonPrimitive().isNumber()) {
            return reject(fileId, "missing/invalid displayPriority");
        }
        int displayPriority = json.get("displayPriority").getAsInt();

        // 6. textureFile
        if (!json.has("textureFile")) {
            return reject(fileId, "missing textureFile");
        }
        String textureFile = json.get("textureFile").getAsString();
        if (!SAFE_ID.matcher(textureFile).matches()) {
            return reject(fileId, "invalid textureFile '" + textureFile + "' (expected ^[a-z0-9_]+$)");
        }

        // 7-8. unlockedFromStart + unlockingAdvancement
        if (!json.has("unlockedFromStart") || !json.get("unlockedFromStart").getAsJsonPrimitive().isBoolean()) {
            return reject(fileId, "missing unlockedFromStart");
        }
        boolean unlockedFromStart = json.get("unlockedFromStart").getAsBoolean();
        String unlockingAdvancement =
                json.has("unlockingAdvancement") ? json.get("unlockingAdvancement").getAsString() : null;
        if (!unlockedFromStart) {
            if (unlockingAdvancement == null || ResourceLocation.tryParse(unlockingAdvancement) == null) {
                return reject(fileId, "locked conversation without valid unlockingAdvancement");
            }
        }

        // 9. steps
        if (!json.has("steps") || !json.get("steps").isJsonArray()) {
            return reject(fileId, "missing/invalid steps array");
        }
        JsonArray stepsArr = json.getAsJsonArray("steps");
        if (stepsArr.isEmpty()) {
            return reject(fileId, "empty steps");
        }

        List<ChatStepDefinition> steps = new ArrayList<>();
        for (int i = 0; i < stepsArr.size(); i++) {
            if (!stepsArr.get(i).isJsonObject()) {
                return reject(fileId, "step " + i + ": not a JSON object");
            }
            ChatStepDefinition step = parseStep(fileId, stepsArr.get(i).getAsJsonObject(), i,
                    i == stepsArr.size() - 1);
            if (step == null) {
                return null; // reason already logged
            }
            steps.add(step);
        }

        return new ChatConversationDefinition(schemaVersion, id, displayName, displayPriority,
                textureFile, unlockedFromStart, unlockingAdvancement, steps);
    }

    private static ChatStepDefinition parseStep(ResourceLocation fileId, JsonObject obj, int index,
                                                boolean isLast) {
        // 10. message arrays (may be empty, must be present arrays of strings)
        List<String> before = readStringArray(obj, "messagesBefore");
        List<String> after = readStringArray(obj, "messagesAfter");
        if (before == null || after == null) {
            return rejectStep(fileId, index, "missing messagesBefore/messagesAfter (must be string arrays)");
        }

        boolean repeatable = obj.has("repeatable") && obj.get("repeatable").getAsBoolean();

        // 13. repeatable only on last step
        if (repeatable && !isLast) {
            return rejectStep(fileId, index, "repeatable flag only allowed on the last step");
        }

        String advancement = obj.has("advancement") ? obj.get("advancement").getAsString() : null;
        String statistic = obj.has("statistic") ? obj.get("statistic").getAsString() : null;
        int statisticAmount = obj.has("statisticAmount") ? obj.get("statisticAmount").getAsInt() : 0;

        if (repeatable) {
            // 12. repeatable requires a valid statistic + amount > 0
            if (statistic == null || ResourceLocation.tryParse(statistic) == null) {
                return rejectStep(fileId, index, "repeatable step requires a valid 'statistic' id");
            }
            if (statisticAmount <= 0) {
                return rejectStep(fileId, index, "repeatable step requires 'statisticAmount' > 0");
            }
        } else {
            // 11. non-repeatable requires a valid advancement
            if (advancement == null || ResourceLocation.tryParse(advancement) == null) {
                return rejectStep(fileId, index, "missing/invalid 'advancement'");
            }
        }

        // 14. rewardItems, if present, must be a parseable loot-table id (a ResourceLocation).
        String rewardItems = obj.has("rewardItems") ? obj.get("rewardItems").getAsString() : null;
        if (rewardItems != null && ResourceLocation.tryParse(rewardItems) == null) {
            return rejectStep(fileId, index, "invalid rewardItems id '" + rewardItems + "'");
        }
        // rewardPersonalTrade is a free GTS unique-offer template id (NOT a ResourceLocation),
        // so only reject an explicitly empty value.
        String rewardTrade = obj.has("rewardPersonalTrade") ? obj.get("rewardPersonalTrade").getAsString() : null;
        if (rewardTrade != null && rewardTrade.isEmpty()) {
            return rejectStep(fileId, index, "empty rewardPersonalTrade id");
        }

        boolean waitNextDay = obj.has("waitNextDay") && obj.get("waitNextDay").getAsBoolean();

        return new ChatStepDefinition(before, after, advancement, statistic, statisticAmount,
                rewardItems, rewardTrade, waitNextDay, repeatable);
    }

    /** Returns null if the field is missing or not an array of strings. */
    private static List<String> readStringArray(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (JsonElement el : obj.getAsJsonArray(key)) {
            if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
                return null;
            }
            out.add(el.getAsString());
        }
        return out;
    }

    private static ChatConversationDefinition reject(ResourceLocation fileId, String reason) {
        CobbleSafari.LOGGER.error("[Chat] {} : {}, skipped", fileId, reason);
        return null;
    }

    private static ChatStepDefinition rejectStep(ResourceLocation fileId, int index, String reason) {
        CobbleSafari.LOGGER.error("[Chat] {} : step {}: {}, skipped", fileId, index, reason);
        return null;
    }
}
