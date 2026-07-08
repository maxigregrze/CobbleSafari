package maxigregrze.cobblesafari.csmusic;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Compiled, immutable AND-combination of play conditions for a {@link CsMusicRule}. A null field
 * means "don't care". Evaluated server-side against a player each tick.
 *
 * <p>The {@code battle} axis is a single enum (never / any / in_battle / wild / npc / pvp) rather
 * than a boolean plus a separate type: folding the battle <i>type</i> into the same field makes
 * contradictory or redundant combinations impossible to express.</p>
 */
public record CsMusicCondition(
        @Nullable String dimension,
        @Nullable String biome,
        @Nullable String biomeTag,
        BattleMode battle,
        @Nullable String species,
        @Nullable String form
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

    public boolean matches(ServerPlayer player) {
        if (dimension != null
                && !player.level().dimension().location().toString().equals(dimension)) {
            return false;
        }
        if (biome != null || biomeTag != null) {
            Holder<Biome> holder = player.serverLevel().getBiome(player.blockPosition());
            if (biome != null) {
                ResourceLocation rl = ResourceLocation.tryParse(biome);
                if (rl == null || !holder.is(ResourceKey.create(Registries.BIOME, rl))) {
                    return false;
                }
            }
            if (biomeTag != null) {
                String raw = biomeTag.startsWith("#") ? biomeTag.substring(1) : biomeTag;
                ResourceLocation rl = ResourceLocation.tryParse(raw);
                if (rl == null || !holder.is(TagKey.create(Registries.BIOME, rl))) {
                    return false;
                }
            }
        }

        BattleMusicTracker.BattleCtx ctx = BattleMusicTracker.of(player.getUUID());
        boolean inBattle = ctx != null;
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
                if (!inBattle || ctx.kind() != battle.requiredKind()) {
                    return false;
                }
            }
        }

        if (species != null && (ctx == null || !species.equals(ctx.species()))) {
            return false;
        }
        return form == null
                || (ctx != null && ctx.form() != null && form.equalsIgnoreCase(ctx.form()));
    }

    /**
     * True if this rule can only ever match during a battle — i.e. a battle-only {@code battle} mode,
     * or a species/form constraint (only known for wild battles). Used to decide whether a matching
     * rule should suppress Cobblemon's native battle music (§ battle suppression).
     */
    public boolean requiresBattle() {
        return battle.requiresBattle() || species != null || form != null;
    }
}
