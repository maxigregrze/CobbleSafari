package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * Petit automate tické représentant un pattern d'attaque (plan 100 § 12).
 */
public interface CsBossAttack {

    /** Appelé une fois au démarrage du pattern. */
    default void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        // par défaut : rien
    }

    /** Appelé chaque tick serveur tant que {@link #isDone()} est faux. */
    void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss);

    /** {@code true} quand le pattern est terminé. */
    boolean isDone();

    /**
     * {@code true} si le pattern pilote lui‑même l'orientation du boss (le manager ne force alors
     * pas {@code faceTarget}). Utilisé par {@code distortion_1} pour faire tourner le boss sur lui‑même.
     */
    default boolean controlsBossRotation() {
        return false;
    }

    /**
     * Catégorie d'effet (plan 111) — utilisée pour autoriser deux attaques simultanées de catégories
     * différentes. Par défaut {@link AttackCategory#SPREAD} (cas {@code test} / volées de bullets).
     */
    default AttackCategory category() {
        return AttackCategory.SPREAD;
    }
}
