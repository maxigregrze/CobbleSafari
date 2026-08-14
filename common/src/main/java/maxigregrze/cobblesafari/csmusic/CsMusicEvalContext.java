package maxigregrze.cobblesafari.csmusic;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Per-player, per-tick scratch space for condition evaluation. Axes that depend only on the player
 * (the biome holder, the areas containing them) are resolved lazily here and reused across every
 * rule of the sweep, instead of being recomputed once per rule.
 *
 * <p>Strictly local to a single evaluation pass: never stored, so there is nothing to invalidate.</p>
 */
public final class CsMusicEvalContext {

    private final ServerPlayer player;
    private Holder<Biome> biome;
    private long dayTime = -1L;
    private CsMusicWeather weather;
    private List<CsMusicArea> areasHere;
    private Set<String> areaIds;
    private Set<String> areaTags;

    public CsMusicEvalContext(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer player() {
        return player;
    }

    public Holder<Biome> biome() {
        if (biome == null) {
            biome = player.serverLevel().getBiome(player.blockPosition());
        }
        return biome;
    }

    /**
     * The time of day the player <i>perceives</i>, normalised to {@code [0, 24000)}: the dimension
     * type's fixed time when it declares one (the sky is frozen there, so the world clock would drive
     * a change nothing on screen explains), otherwise the level's day time. Computed once.
     */
    public long dayTime() {
        if (dayTime < 0L) {
            ServerLevel level = player.serverLevel();
            long raw = level.dimensionType().fixedTime().orElse(level.getDayTime());
            dayTime = Math.floorMod(raw, (long) CsMusicTimeRange.DAY_LENGTH);
        }
        return dayTime;
    }

    /**
     * Weather where the player stands, for the {@code when.weather} axis. Reuses the memoized biome
     * holder, so a rule constraining both the biome and the weather resolves the biome once. Computed
     * once per sweep - the player does not move within one.
     */
    public CsMusicWeather weather() {
        if (weather == null) {
            weather = CsMusicWeather.current(player.serverLevel(), biome(), player.blockPosition());
        }
        return weather;
    }

    /** Activated areas of the player's dimension containing their block position. Computed once. */
    public List<CsMusicArea> areasHere() {
        if (areasHere == null) {
            resolveAreas();
        }
        return areasHere;
    }

    /** Ids of {@link #areasHere()}, for the {@code when.area} axis. */
    public Set<String> areaIds() {
        if (areasHere == null) {
            resolveAreas();
        }
        return areaIds;
    }

    /** Union of the tags of {@link #areasHere()}, for the {@code when.area_tag} axis. */
    public Set<String> areaTags() {
        if (areasHere == null) {
            resolveAreas();
        }
        return areaTags;
    }

    private void resolveAreas() {
        int x = player.getBlockX();
        int y = player.getBlockY();
        int z = player.getBlockZ();
        List<CsMusicArea> hits = null;
        Set<String> ids = null;
        Set<String> tags = null;
        for (CsMusicArea area : CsMusicAreaStore.areasIn(player.serverLevel())) {
            if (!area.activated() || !area.contains(x, y, z)) {
                continue;
            }
            if (hits == null) {
                hits = new ArrayList<>(2);
                ids = new HashSet<>(4);
            }
            hits.add(area);
            ids.add(area.id());
            if (!area.tags().isEmpty()) {
                if (tags == null) {
                    tags = new HashSet<>(4);
                }
                tags.addAll(area.tags());
            }
        }
        // The dominant case is "no area at all": allocate nothing and fall back on the shared
        // immutable singletons. This context is built per player and per sweep.
        areasHere = hits == null ? List.of() : hits;
        areaIds = ids == null ? Set.of() : ids;
        areaTags = tags == null ? Set.of() : tags;
    }
}
