package maxigregrze.cobblesafari.csmusic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads trigger rules from {@code data/<ns>/csmusic/<SUBDIR>/*.json}. A file holds a list of
 * cumulable rules; each rule proposes a track when its {@link CsMusicCondition} matches a player.
 *
 * <p>Every condition constant is resolved here, once, so the per-sweep evaluation stays free of
 * parsing and interning.</p>
 */
public final class CsMusicTriggerDataLoader {

    private static final Gson GSON = new GsonBuilder().create();
    /** Subdirectory of {@code csmusic/} holding trigger files - shared so the two loaders agree. */
    public static final String SUBDIR = "definition";
    private static final String PREFIX = "csmusic/" + SUBDIR;

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
            CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} invalid battle mode '{}' - defaulting to 'any'", file, idx, w.battle);
            battle = CsMusicCondition.BattleMode.ANY;
        }

        CsMusicTimeRange timeRange = null;
        if (w.time_range != null) {
            timeRange = CsMusicTimeRange.fromJson(w.time_range);
            if (timeRange == null) {
                CobbleSafari.LOGGER.warn(
                        "[CSMusic] {} rule#{} invalid time_range {} - expected [start, end] in [0, {}]"
                                + " with start != end; constraint ignored",
                        file, idx, Arrays.toString(w.time_range), CsMusicTimeRange.DAY_LENGTH);
            }
        }

        String rawStructure = blankToNull(w.structure);
        CsMusicStructureFilter structure = null;
        if (rawStructure != null) {
            structure = CsMusicStructureFilter.fromJson(rawStructure);
            if (structure == null) {
                CobbleSafari.LOGGER.warn(
                        "[CSMusic] {} rule#{} invalid structure filter '{}' - constraint ignored",
                        file, idx, rawStructure);
            }
        }

        String rawPiece = blankToNull(w.structure_piece);
        CsMusicPiecePattern structurePiece = rawPiece == null ? null : CsMusicPiecePattern.fromJson(rawPiece);

        // Area axes carry no compilation step: an area id is world data, not a ResourceLocation.
        // They are deliberately not cross-checked here - areas live in the world save and are read
        // lazily per dimension, so no such check is possible at server start (see /csmusic current).
        String area = blankToNull(w.area);
        String areaTag = blankToNull(w.area_tag);
        if (areaTag != null) {
            areaTag = areaTag.toLowerCase(Locale.ROOT);
        }

        CsMusicCondition cond = new CsMusicCondition(
                dimensionKey(file, idx, blankToNull(w.dimension)),
                timeRange,
                biomeKey(file, idx, blankToNull(w.biome)),
                biomeTagKey(file, idx, blankToNull(w.biome_tag)),
                weatherSet(file, idx, w.weather),
                area,
                areaTag,
                battle,
                blankToNull(w.species),
                blankToNull(w.form),
                structure,
                structurePiece);

        String source = "definition:" + file.getPath() + "#" + idx;
        String musicId = hasMusic ? raw.music.trim() : null;
        String poolTag = hasTag ? raw.tag.trim().toLowerCase(Locale.ROOT) : null;

        // The registry is already populated: CsMusicDataLoader.load runs before this loader in
        // DimensionEvents.onServerStarted, so an unresolvable target can be reported here.
        if (musicId != null && !CsMusicRegistry.has(musicId)) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} targets unknown music '{}' - it will never play",
                    file, idx, musicId);
        }
        if (poolTag != null && CsMusicRegistry.byTag(poolTag).isEmpty()) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} targets empty tag pool '{}' - it will never play",
                    file, idx, poolTag);
        }
        return new CsMusicRule(source, musicId, poolTag, Math.max(0, raw.priority), cond);
    }

    /**
     * Compiles the {@code when.weather} list. Unknown entries are dropped with a warning; an empty or
     * fully invalid list yields {@code null}, i.e. no constraint - the loader never kills a rule over
     * one bad value.
     */
    @Nullable
    private static Set<CsMusicWeather> weatherSet(ResourceLocation file, int idx,
                                                  @Nullable List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        EnumSet<CsMusicWeather> set = EnumSet.noneOf(CsMusicWeather.class);
        for (String entry : raw) {
            CsMusicWeather parsed = CsMusicWeather.fromJson(entry);
            if (parsed == null) {
                CobbleSafari.LOGGER.warn(
                        "[CSMusic] {} rule#{} unknown weather '{}' - expected none / rain / snow /"
                                + " thunder; value ignored", file, idx, entry);
            } else {
                set.add(parsed);
            }
        }
        if (set.isEmpty()) {
            return null;
        }
        if (set.size() == CsMusicWeather.values().length) {
            // How a packmaker writes "any weather" while believing they restricted something.
            CobbleSafari.LOGGER.warn(
                    "[CSMusic] {} rule#{} weather lists every state - the constraint is a no-op",
                    file, idx);
        }
        return Collections.unmodifiableSet(set);
    }

    @Nullable
    private static ResourceKey<Level> dimensionKey(ResourceLocation file, int idx, @Nullable String raw) {
        ResourceLocation rl = parseOrWarn(file, idx, raw, "dimension");
        return rl == null ? null : ResourceKey.create(Registries.DIMENSION, rl);
    }

    @Nullable
    private static ResourceKey<Biome> biomeKey(ResourceLocation file, int idx, @Nullable String raw) {
        ResourceLocation rl = parseOrWarn(file, idx, raw, "biome");
        return rl == null ? null : ResourceKey.create(Registries.BIOME, rl);
    }

    @Nullable
    private static TagKey<Biome> biomeTagKey(ResourceLocation file, int idx, @Nullable String raw) {
        String stripped = raw != null && raw.startsWith("#") ? raw.substring(1) : raw;
        ResourceLocation rl = parseOrWarn(file, idx, stripped, "biome_tag");
        return rl == null ? null : TagKey.create(Registries.BIOME, rl);
    }

    /** Parses an id; a malformed value drops the constraint with a warning, it never kills the rule. */
    @Nullable
    private static ResourceLocation parseOrWarn(ResourceLocation file, int idx,
                                                @Nullable String raw, String field) {
        if (raw == null) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(raw);
        if (rl == null) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} invalid {} '{}' - constraint ignored",
                    file, idx, field, raw);
        }
        return rl;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
