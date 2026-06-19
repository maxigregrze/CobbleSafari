package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * Small ticked automaton representing an attack pattern.
 */
public interface CsBossAttack {

    /** Called once when the pattern starts. */
    default void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        // by default: nothing
    }

    /** Called each server tick while {@link #isDone()} is false. */
    void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss);

    /** {@code true} when the pattern is finished. */
    boolean isDone();

    /**
     * {@code true} if the pattern controls boss orientation itself (the manager does not then
     * force {@code faceTarget}). Used by {@code distortion_1} to spin the boss on itself.
     */
    default boolean controlsBossRotation() {
        return false;
    }

    /**
     * Effect category — used to allow two simultaneous attacks of different
     * categories. By default {@link AttackCategory#SPREAD} ({@code test} case / bullet volleys).
     */
    default AttackCategory category() {
        return AttackCategory.SPREAD;
    }
}
