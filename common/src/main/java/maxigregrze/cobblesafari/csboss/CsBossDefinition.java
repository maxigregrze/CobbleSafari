package maxigregrze.cobblesafari.csboss;

import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Immutable CSBoss definition, loaded from {@code data/cobblesafari/csboss/<bossId>.json}.
 *
 */
public record CsBossDefinition(
        String bossId,
        @Nullable String displayName,
        List<String> tags,
        int maximumDuration,
        int minimumDuration,
        String specie,
        @Nullable String minion,
        int size,
        List<String> moveSet,
        int moveCooldownMin,
        int moveCooldownMax,
        boolean isStatic,
        @Nullable ResourceLocation uniqueReward,
        ResourceLocation rewards,
        @Nullable String music,
        BossEvent.BossBarOverlay healthStyle,
        BossEvent.BossBarColor healthColor,
        /** Id or tag of a CSBoss chained as second phase (same session), or {@code null}. */
        @Nullable String secondPhase,
        /** If {@code true}, grants this phase's rewards before chaining to the next. */
        boolean giveRewardsBeforeSecondPhase,
        /**
         * If {@code true}, the boss tries to play two attacks of different categories at once
         *. If no distinct categories are available, only one is played.
         */
        boolean allowSimultaneousAttacks,
        /** Visual variant of the summon portal: selects csboss_spawnportal_type{N}_layer{1..3}. */
        int portalType,
        /** Multiplicateur de taille du portail (défaut = échelle de rendu actuelle). */
        double portalSize,
        /** Hauteur d'apparition du portail au-dessus de l'ancre, en blocs (défaut = ENTRANCE_HEIGHT). */
        double portalDistance
) {
    public static final int DEFAULT_SIZE = 5;
    public static final int DEFAULT_COOLDOWN_MIN = 4;
    public static final int DEFAULT_COOLDOWN_MAX = 8;
    public static final int DEFAULT_PORTAL_TYPE = 1;
    public static final double DEFAULT_PORTAL_SIZE = 2.0;
    public static final double DEFAULT_PORTAL_DISTANCE = CsBossEntity.ENTRANCE_HEIGHT;

    /** moveSet keyword granting all registered attacks. */
    public static final String ALL_MOVES = "ALLMOVES";

    public boolean grantsAllMoves() {
        return moveSet.size() == 1 && ALL_MOVES.equalsIgnoreCase(moveSet.get(0));
    }

    public boolean hasCustomMoveSet() {
        return !moveSet.isEmpty();
    }

    /** Name shown above the boss bar: {@code displayName} if set, otherwise {@code bossId}. */
    public String effectiveDisplayName() {
        return displayName != null && !displayName.isBlank() ? displayName : bossId;
    }

    /** Minion species for this boss: {@code minion} if set, otherwise the boss species. */
    public String effectiveMinionSpecie() {
        return minion != null && !minion.isBlank() ? minion : specie;
    }

    public boolean hasSecondPhase() {
        return secondPhase != null && !secondPhase.isBlank();
    }
}
