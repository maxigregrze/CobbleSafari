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
}
