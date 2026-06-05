package maxigregrze.cobblesafari.csboss;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Définition immuable d'un CSBoss, chargée depuis {@code data/cobblesafari/csboss/<bossId>.json}.
 * Voir le plan d'action 100 § 2.
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
        /** Id ou tag d'un CSBoss enchaîné en seconde phase (même session), ou {@code null}. */
        @Nullable String secondPhase,
        /** Si {@code true}, distribue les récompenses de cette phase avant d'enchaîner la suivante. */
        boolean giveRewardsBeforeSecondPhase,
        /**
         * Si {@code true}, le boss tente de jouer deux attaques de catégories différentes en même
         * temps (plan 111). À défaut d'attaques de catégories distinctes, il n'en joue qu'une.
         */
        boolean allowSimultaneousAttacks
) {
    public static final int DEFAULT_SIZE = 5;
    public static final int DEFAULT_COOLDOWN_MIN = 4;
    public static final int DEFAULT_COOLDOWN_MAX = 8;

    /** Mot-clé du moveSet octroyant toutes les attaques enregistrées. */
    public static final String ALL_MOVES = "ALLMOVES";

    public boolean grantsAllMoves() {
        return moveSet.size() == 1 && ALL_MOVES.equalsIgnoreCase(moveSet.get(0));
    }

    public boolean hasCustomMoveSet() {
        return !moveSet.isEmpty();
    }

    /** Nom affiché au-dessus de la barre de boss : {@code displayName} si défini, sinon {@code bossId}. */
    public String effectiveDisplayName() {
        return displayName != null && !displayName.isBlank() ? displayName : bossId;
    }

    /** Espèce des minions de ce boss : {@code minion} si défini, sinon l'espèce du boss. */
    public String effectiveMinionSpecie() {
        return minion != null && !minion.isBlank() ? minion : specie;
    }

    public boolean hasSecondPhase() {
        return secondPhase != null && !secondPhase.isBlank();
    }
}
