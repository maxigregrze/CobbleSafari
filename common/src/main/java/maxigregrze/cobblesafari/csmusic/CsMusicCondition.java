package maxigregrze.cobblesafari.csmusic;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * Compiled, immutable AND-combination of play conditions for a {@link CsMusicRule}. A null field
 * means "don't care". Evaluated server-side against a player each arbitration sweep.
 *
 * <p>Every axis is <b>compiled at load time</b>: dimension, biome and biome tag are resolved keys,
 * never strings reparsed per evaluation - {@code ResourceKey.create} and {@code TagKey.create} both
 * hit weak intern tables, which has no business running in the sweep.</p>
 *
 * <p>The {@code battle} axis is a single enum (never / any / in_battle / wild / npc / pvp) rather
 * than a boolean plus a separate type: folding the battle <i>type</i> into the same field makes
 * contradictory or redundant combinations impossible to express. The {@code structure} axis is
 * likewise a single compiled filter (any / id / tag).</p>
 *
 * <p>The {@code area} / {@code areaTag} axes are the only ones with nothing to compile: an area id
 * is world data, not a {@code ResourceLocation}, so it never hits an intern table. They are matched
 * against the areas the player currently stands in, resolved once per sweep by the context.</p>
 *
 * <p>The {@code timeRange} and {@code weather} axes read values memoized by the context. The former
 * is the cheapest axis of all and is tested right after the dimension; the latter needs the biome
 * holder, so it sits just after the biome axes and reuses whatever they resolved.</p>
 */
public record CsMusicCondition(
        @Nullable ResourceKey<Level> dimension,
        @Nullable CsMusicTimeRange timeRange,
        @Nullable ResourceKey<Biome> biome,
        @Nullable TagKey<Biome> biomeTag,
        @Nullable Set<CsMusicWeather> weather,
        @Nullable String area,
        @Nullable String areaTag,
        BattleMode battle,
        @Nullable String species,
        @Nullable String form,
        @Nullable CsMusicStructureFilter structure,
        @Nullable CsMusicPiecePattern structurePiece
) {
    /**
     * Where, relative to a Cobblemon battle, a rule is allowed to play.
     * {@link #WILD}/{@link #NPC}/{@link #PVP} additionally constrain the battle type.
     */
    public enum BattleMode {
        /** Only outside battle. */
        NEVER,
        /** Plays regardless of battle state (the default when the field is omitted). */
        ANY,
        /** Only during a battle, any type. */
        IN_BATTLE,
        /** Only during a wild battle. */
        WILD,
        /** Only during an NPC battle. */
        NPC,
        /** Only during a PvP battle. */
        PVP;

        /** Parses the json value; {@code null}/blank ⇒ {@link #ANY}; returns {@code null} if invalid. */
        @Nullable
        public static BattleMode fromJson(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return ANY;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "never" -> NEVER;
                case "any", "also" -> ANY;
                case "in_battle", "battle" -> IN_BATTLE;
                case "wild" -> WILD;
                case "npc" -> NPC;
                case "pvp" -> PVP;
                default -> null;
            };
        }

        @Nullable
        BattleMusicTracker.Kind requiredKind() {
            return switch (this) {
                case WILD -> BattleMusicTracker.Kind.WILD;
                case NPC -> BattleMusicTracker.Kind.NPC;
                case PVP -> BattleMusicTracker.Kind.PVP;
                default -> null;
            };
        }

        /** True if this mode can only ever be satisfied while in a battle. */
        boolean requiresBattle() {
            return this == IN_BATTLE || this == WILD || this == NPC || this == PVP;
        }
    }

    public boolean matches(CsMusicEvalContext ctx) {
        ServerPlayer player = ctx.player();
        if (dimension != null && !player.level().dimension().equals(dimension)) {
            return false;
        }
        // Cheapest axis of all: one memoized field read per player and per sweep, no world access.
        if (timeRange != null && !timeRange.matches(ctx.dayTime())) {
            return false;
        }
        if (biome != null || biomeTag != null) {
            Holder<Biome> holder = ctx.biome();
            if (biome != null && !holder.is(biome)) {
                return false;
            }
            if (biomeTag != null && !holder.is(biomeTag)) {
                return false;
            }
        }
        // Right after the biome axes: this one reads the same holder, so a rule constraining both
        // pays for it once. Still memoized per sweep, so a weather-only rule pays it once too.
        if (weather != null && !weather.contains(ctx.weather())) {
            return false;
        }
        // Set lookups on a per-sweep memoized scan: cheaper than the structure axes below, which
        // are the only ones that can touch chunk storage.
        if (area != null && !ctx.areaIds().contains(area)) {
            return false;
        }
        if (areaTag != null && !ctx.areaTags().contains(areaTag)) {
            return false;
        }

        BattleMusicTracker.BattleCtx battleCtx = BattleMusicTracker.of(player.getUUID());
        boolean inBattle = battleCtx != null;
        switch (battle) {
            case NEVER -> {
                if (inBattle) {
                    return false;
                }
            }
            case ANY -> { /* no battle constraint */ }
            case IN_BATTLE -> {
                if (!inBattle) {
                    return false;
                }
            }
            case WILD, NPC, PVP -> {
                if (!inBattle || battleCtx.kind() != battle.requiredKind()) {
                    return false;
                }
            }
        }

        if (species != null && (battleCtx == null || !species.equals(battleCtx.species()))) {
            return false;
        }
        if (form != null
                && (battleCtx == null || battleCtx.form() == null
                        || !form.equalsIgnoreCase(battleCtx.form()))) {
            return false;
        }
        // Evaluated last: the only axes that can touch chunk storage (memoized per chunk).
        if (structurePiece != null) {
            // matchesPiece applies the structure filter itself, so it is not tested twice.
            return CsMusicStructureTracker.matchesPiece(player, structure, structurePiece);
        }
        return structure == null || CsMusicStructureTracker.matches(player, structure);
    }

    /**
     * True if this rule can only ever match during a battle - i.e. a battle-only {@code battle} mode,
     * or a species/form constraint (only known for wild battles). Used to decide whether a matching
     * rule should suppress Cobblemon's native battle music (§ battle suppression).
     */
    public boolean requiresBattle() {
        return battle.requiresBattle() || species != null || form != null;
    }
}
