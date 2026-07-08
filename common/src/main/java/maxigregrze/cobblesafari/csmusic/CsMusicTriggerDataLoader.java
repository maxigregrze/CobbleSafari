package maxigregrze.cobblesafari.csmusic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads trigger rules from {@code data/<ns>/csmusic/definition/*.json}. A file holds a list of
 * cumulable rules; each rule proposes a track when its {@link CsMusicCondition} matches a player.
 */
public final class CsMusicTriggerDataLoader {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String PREFIX = "csmusic/definition";

    private CsMusicTriggerDataLoader() {}

    public static void load(MinecraftServer server) {
        CsMusicTriggerRegistry.clear();
        List<CsMusicRule> rules = new ArrayList<>();

        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources =
                manager.listResources(PREFIX, id -> id.getPath().endsWith(".json"));
        int files = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                CsMusicTriggerFileData data = GSON.fromJson(reader, CsMusicTriggerFileData.class);
                if (data == null || data.rules == null) {
                    continue;
                }
                int idx = 0;
                for (CsMusicTriggerFileData.Rule raw : data.rules) {
                    CsMusicRule rule = compile(entry.getKey(), idx++, raw);
                    if (rule != null) {
                        rules.add(rule);
                    }
                }
                files++;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[CSMusic] Failed to load trigger file {}", entry.getKey(), e);
            }
        }

        CsMusicTriggerRegistry.addAll(rules);
        CobbleSafari.LOGGER.info("[CSMusic] Loaded {} trigger rule(s) from {} file(s)", rules.size(), files);
    }

    private static CsMusicRule compile(ResourceLocation file, int idx, CsMusicTriggerFileData.Rule raw) {
        if (raw == null) {
            return null;
        }
        boolean hasMusic = raw.music != null && !raw.music.isBlank();
        boolean hasTag = raw.tag != null && !raw.tag.isBlank();
        if (hasMusic == hasTag) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} must set exactly one of 'music' / 'tag'", file, idx);
            return null;
        }

        CsMusicTriggerFileData.When w = raw.when != null ? raw.when : new CsMusicTriggerFileData.When();
        CsMusicCondition.BattleMode battle = CsMusicCondition.BattleMode.fromJson(w.battle);
        if (battle == null) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} invalid battle mode '{}' — defaulting to 'any'", file, idx, w.battle);
            battle = CsMusicCondition.BattleMode.ANY;
        }

        CsMusicCondition cond = new CsMusicCondition(
                blankToNull(w.dimension),
                blankToNull(w.biome),
                blankToNull(w.biome_tag),
                battle,
                blankToNull(w.species),
                blankToNull(w.form));

        String source = "definition:" + file.getPath() + "#" + idx;
        String musicId = hasMusic ? raw.music.trim() : null;
        String poolTag = hasTag ? raw.tag.trim().toLowerCase(Locale.ROOT) : null;
        return new CsMusicRule(source, musicId, poolTag, Math.max(0, raw.priority), cond);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
