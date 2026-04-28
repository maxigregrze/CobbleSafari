package maxigregrze.cobblesafari.rotomphone;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Pattern;

public final class RotomPhoneSkinDataLoader {
    private RotomPhoneSkinDataLoader() {}

    private static final String DATA_DIR = "rotomphone_skins";
    private static final int CURRENT_VERSION = 1;
    private static final Pattern HEX_COLOR = Pattern.compile("^[0-9a-fA-F]{6}$");

    public static void load(MinecraftServer server) {
        RotomPhoneSkinRegistry.clear();
        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources = manager.listResources(DATA_DIR,
                id -> id.getPath().endsWith(".json"));

        int loaded = 0;
        int skipped = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                RotomPhoneSkinDefinition skin = parseSkin(fileId, reader);
                if (skin == null) {
                    skipped++;
                    continue;
                }
                RotomPhoneSkinRegistry.register(skin);
                loaded++;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[RotomPhone] Failed to load skin {}", fileId, e);
                skipped++;
            }
        }
        CobbleSafari.LOGGER.info("Loaded {} rotomphone skins ({} skipped)", loaded, skipped);
    }

    private static RotomPhoneSkinDefinition parseSkin(ResourceLocation fileId, InputStreamReader reader) {
        JsonElement root = JsonParser.parseReader(reader);
        if (!root.isJsonObject()) {
            CobbleSafari.LOGGER.warn("[RotomPhone] {} is not a JSON object, skipped", fileId);
            return null;
        }
        JsonObject json = root.getAsJsonObject();

        if (!json.has("id")) {
            CobbleSafari.LOGGER.warn("[RotomPhone] {} missing required field 'id', skipped", fileId);
            return null;
        }
        String id = json.get("id").getAsString();
        if (id.contains(" ")) {
            CobbleSafari.LOGGER.warn("[RotomPhone] {} skin id '{}' contains spaces, skipped", fileId, id);
            return null;
        }

        if (!json.has("displayName")) {
            CobbleSafari.LOGGER.warn("[RotomPhone] {} missing required field 'displayName', skipped", fileId);
            return null;
        }
        String displayName = json.get("displayName").getAsString();

        if (!json.has("color")) {
            CobbleSafari.LOGGER.warn("[RotomPhone] {} missing required field 'color', skipped", fileId);
            return null;
        }
        String color = json.get("color").getAsString();
        if (!HEX_COLOR.matcher(color).matches()) {
            CobbleSafari.LOGGER.warn("[RotomPhone] {} has invalid hex color '{}', skipped", fileId, color);
            return null;
        }

        boolean hasCustomScreen = json.has("hasCustomScreen") && json.get("hasCustomScreen").getAsBoolean();
        boolean unlockedFromStart = json.has("unlockedFromStart") && json.get("unlockedFromStart").getAsBoolean();
        String unlockingAdvancement = json.has("unlockingAdvancement") ? json.get("unlockingAdvancement").getAsString() : "";
        boolean hasShinyVariant = json.has("hasShinyVariant") && json.get("hasShinyVariant").getAsBoolean();

        return new RotomPhoneSkinDefinition(id, displayName, color, hasCustomScreen, unlockedFromStart, unlockingAdvancement, hasShinyVariant);
    }
}
