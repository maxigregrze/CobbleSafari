package maxigregrze.cobblesafari.csmusic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.Registry;
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
 * Loads trigger rules from the definition files {@code data/<ns>/csmusic/<SUBDIR>/*.json}. A file
 * holds a list of cumulable rules; each rule proposes a track when its {@link CsMusicCondition}
 * matches a player.
 *
 * <p>Every condition constant is resolved here, once, so the per-sweep evaluation stays free of
 * parsing and interning.</p>
 *
 * <p>Validation follows one rule: <b>malformed syntax is fatal, an unresolved reference is a
 * warning</b>. A value the loader cannot interpret rejects the <b>whole file</b> - dropping the
 * constraint instead would widen an AND of constraints, i.e. make the rule play where it should
 * not. A value that is well formed but designates nothing right now (an unknown music id, an empty
 * pool, a biome tag that does not exist) only warns: another datapack may still provide it, so its
 * validity must not depend on load order.</p>
 */
public final class CsMusicTriggerDataLoader {

    private static final Gson GSON = new GsonBuilder().create();
    /** Subdirectory of {@code csmusic/} holding definition files - shared so the two loaders agree. */
    public static final String SUBDIR = "definition";
    private static final String PREFIX = "csmusic/" + SUBDIR;

    private CsMusicTriggerDataLoader() {}

    /**
     * Reads whatever the server currently exposes, and uses its registries for
     * {@link #warnUnresolved}. On the reload path both are only correct once the server has swapped
     * its {@code resources} field and rebound its tags, which is why this runs on the tick
     * following a datapack reload rather than inside it - see {@code DimensionEvents#onDatapackReload}.
     */
    public static void load(MinecraftServer server) {
        ResourceManager manager = server.getResourceManager();
        CsMusicTriggerRegistry.clear();
        List<CsMusicRule> rules = new ArrayList<>();

        Map<ResourceLocation, Resource> resources =
                manager.listResources(PREFIX, id -> id.getPath().endsWith(".json"));
        int files = 0;
        int rejected = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            // Staged per file: rules used to be appended as they compiled, so a file that failed
            // halfway left its first rules behind. Rejection has to be all-or-nothing.
            List<CsMusicRule> staged = new ArrayList<>();
            int ruleCount = 0;
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                CsMusicTriggerFileData data = GSON.fromJson(reader, CsMusicTriggerFileData.class);
                if (data == null || data.rules == null) {
                    continue;
                }
                ruleCount = data.rules.size();
                int idx = 0;
                for (CsMusicTriggerFileData.Rule raw : data.rules) {
                    staged.add(compile(entry.getKey(), idx++, raw));
                }
            } catch (CsMusicRuleException e) {
                CobbleSafari.LOGGER.error("[CSMusic] {} rule#{} invalid {} '{}' - THE WHOLE FILE IS REJECTED,"
                                + " none of its {} rule(s) will load",
                        entry.getKey(), e.ruleIndex(), e.field(), e.value(), ruleCount);
                rejected++;
                continue;
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[CSMusic] Failed to load definition file {}", entry.getKey(), e);
                rejected++;
                continue;
            }
            for (int i = 0; i < staged.size(); i++) {
                warnUnresolved(entry.getKey(), i, staged.get(i), server);
            }
            rules.addAll(staged);
            files++;
        }

        CsMusicTriggerRegistry.addAll(rules);
        CobbleSafari.LOGGER.info("[CSMusic] Loaded {} trigger rule(s) from {} file(s), {} file(s) rejected",
                rules.size(), files, rejected);
    }

    private static CsMusicRule compile(ResourceLocation file, int idx, CsMusicTriggerFileData.Rule raw) {
        if (raw == null) {
            throw new CsMusicRuleException(idx, "rule", "null");
        }
        boolean hasMusic = raw.music != null && !raw.music.isBlank();
        boolean hasTag = raw.tag != null && !raw.tag.isBlank();
        if (hasMusic == hasTag) {
            throw new CsMusicRuleException(idx, "music/tag",
                    hasMusic ? "both set" : "neither set");
        }
        if (raw.priority < 0) {
            throw new CsMusicRuleException(idx, "priority", String.valueOf(raw.priority));
        }

        CsMusicTriggerFileData.When w = raw.when != null ? raw.when : new CsMusicTriggerFileData.When();
        CsMusicCondition.BattleMode battle = CsMusicCondition.BattleMode.fromJson(w.battle);
        if (battle == null) {
            throw new CsMusicRuleException(idx, "battle", w.battle);
        }

        CsMusicTimeRange timeRange = null;
        if (w.time_range != null) {
            timeRange = CsMusicTimeRange.fromJson(w.time_range);
            if (timeRange == null) {
                throw new CsMusicRuleException(idx, "time_range",
                        Arrays.toString(w.time_range)
                                + " (expected [start, end] in [0, " + CsMusicTimeRange.DAY_LENGTH
                                + "] with start != end)");
            }
        }

        String rawStructure = blankToNull(w.structure);
        CsMusicStructureFilter structure = null;
        if (rawStructure != null) {
            structure = CsMusicStructureFilter.fromJson(rawStructure);
            if (structure == null) {
                throw new CsMusicRuleException(idx, "structure", rawStructure);
            }
        }

        String rawPiece = blankToNull(w.structure_piece);
        CsMusicPiecePattern structurePiece = null;
        if (rawPiece != null) {
            structurePiece = CsMusicPiecePattern.fromJson(rawPiece);
            if (structurePiece == null) {
                throw new CsMusicRuleException(idx, "structure_piece", rawPiece);
            }
        }

        // Area axes carry no compilation step: an area id is world data, not a ResourceLocation.
        // They are deliberately not cross-checked here - areas live in the world save and are read
        // lazily per dimension, so no such check is possible at server start (see /csmusic current).
        String area = blankToNull(w.area);
        String areaTag = blankToNull(w.area_tag);
        if (areaTag != null) {
            areaTag = areaTag.toLowerCase(Locale.ROOT);
        }

        CsMusicCondition cond = new CsMusicCondition(
                dimensionKey(idx, blankToNull(w.dimension)),
                timeRange,
                biomeKey(idx, blankToNull(w.biome)),
                biomeTagKey(idx, blankToNull(w.biome_tag)),
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
        return new CsMusicRule(source, musicId, poolTag, raw.priority, cond);
    }

    /**
     * Well-formed values that designate nothing. None of these rejects the file: a target can be
     * supplied by another datapack, or by a mod absent from one test profile.
     *
     * <p>Runs after compilation, once the registry of initialisation files is populated - the two
     * loaders are called in that order, at startup and on reload alike.</p>
     */
    private static void warnUnresolved(ResourceLocation file, int idx, CsMusicRule rule, MinecraftServer server) {
        if (rule.musicId() != null && !CsMusicRegistry.has(rule.musicId())) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} targets unknown music '{}' - it will never play",
                    file, idx, rule.musicId());
        }
        if (rule.poolTag() != null && CsMusicRegistry.byTag(rule.poolTag()).isEmpty()) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} targets empty tag pool '{}' - it will never play",
                    file, idx, rule.poolTag());
        }

        CsMusicCondition cond = rule.condition();
        Registry<Biome> biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
        if (cond.biome() != null && !biomes.containsKey(cond.biome())) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} biome '{}' is not registered -"
                    + " this rule will never match", file, idx, cond.biome().location());
        }
        if (cond.biomeTag() != null) {
            var tag = biomes.getTag(cond.biomeTag());
            if (tag.isEmpty()) {
                CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} biome tag '#{}' does not exist -"
                                + " this rule will never match. Biome tags live in"
                                + " data/<namespace>/tags/worldgen/biome/, not data/<namespace>/tags/biome/",
                        file, idx, cond.biomeTag().location());
            } else if (tag.get().size() == 0) {
                CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} biome tag '#{}' is empty -"
                        + " this rule will never match", file, idx, cond.biomeTag().location());
            }
        }
        if (cond.dimension() != null && !server.levelKeys().contains(cond.dimension())) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} rule#{} dimension '{}' is not loaded -"
                    + " this rule will never match", file, idx, cond.dimension().location());
        }
    }

    /**
     * Compiles the {@code when.weather} list. An unknown entry is fatal; an empty list yields
     * {@code null}, i.e. no constraint.
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
                throw new CsMusicRuleException(idx, "weather",
                        entry + " (expected none / rain / snow / thunder)");
            }
            set.add(parsed);
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
    private static ResourceKey<Level> dimensionKey(int idx, @Nullable String raw) {
        ResourceLocation rl = parseOrThrow(idx, raw, "dimension");
        return rl == null ? null : ResourceKey.create(Registries.DIMENSION, rl);
    }

    @Nullable
    private static ResourceKey<Biome> biomeKey(int idx, @Nullable String raw) {
        ResourceLocation rl = parseOrThrow(idx, raw, "biome");
        return rl == null ? null : ResourceKey.create(Registries.BIOME, rl);
    }

    @Nullable
    private static TagKey<Biome> biomeTagKey(int idx, @Nullable String raw) {
        String stripped = raw != null && raw.startsWith("#") ? raw.substring(1) : raw;
        ResourceLocation rl = parseOrThrow(idx, stripped, "biome_tag");
        return rl == null ? null : TagKey.create(Registries.BIOME, rl);
    }

    /** Parses an id; a malformed value rejects the file rather than silently widening the rule. */
    @Nullable
    private static ResourceLocation parseOrThrow(int idx, @Nullable String raw, String field) {
        if (raw == null) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(raw);
        if (rl == null) {
            throw new CsMusicRuleException(idx, field, raw);
        }
        return rl;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
