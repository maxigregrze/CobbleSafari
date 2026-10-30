package maxigregrze.cobblesafari.csboss;

import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import maxigregrze.cobblesafari.init.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * Disparition / réapparition « à la Safari » (retours plan 199) : même clignotement que la fuite d'un
 * Pokémon du Safari ({@code SafariStateManager#startFadeAndDespawn}) et même son, piloté tick par tick
 * par l'attaque. Règle commune à toute attaque qui fait disparaître le boss : tant qu'il est caché il
 * ignore les baumes (coup direct et éclaboussure), et les minions marqués « relais »
 * ({@link CsBossMinionEntity#setBalmProxy}) reçoivent les baumes à sa place.
 */
public final class CsBossVanish {

    /** Durée d'un clignotement de fuite ou de réapparition, en ticks. */
    public static final int BLINK_TICKS = 20;

    private CsBossVanish() {}

    /**
     * Visibilité au tick {@code t} d'une fuite (motif Safari : clignote puis disparaît). Les attaques
     * jouent les ticks 0 à {@code BLINK_TICKS - 1} : le dernier est donc déjà caché, sinon l'entité
     * restait visible une fois le clignotement fini.
     */
    public static boolean visibleWhileFleeing(int t) {
        if (t < 0) {
            return true;
        }
        if (t >= BLINK_TICKS - 1) {
            return false;
        }
        return switch (t) {
            case 4, 5, 8, 10, 12, 14 -> false;
            default -> true;
        };
    }

    /** Visibilité au tick {@code t} d'une réapparition : la fuite jouée à l'envers (cachée au tick 0). */
    public static boolean visibleWhileAppearing(int t) {
        if (t < 0) {
            return false;
        }
        return t >= BLINK_TICKS || visibleWhileFleeing(BLINK_TICKS - 1 - t);
    }

    /** Applique la fuite ({@code appearing = false}) ou la réapparition au tick {@code t} ; son au tick 0. */
    public static void blink(ServerLevel level, Entity entity, int t, boolean appearing) {
        entity.setInvisible(!(appearing ? visibleWhileAppearing(t) : visibleWhileFleeing(t)));
        if (t == 0) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    ModSounds.FLEE_SOUND, SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    /** Boss caché (immunisé aux baumes) ou de retour ; toujours visible quand il n'est plus caché. */
    public static void setBossHidden(CsBossEntity boss, boolean hidden) {
        boss.setVanished(hidden);
        if (!hidden) {
            boss.setInvisible(false);
        }
    }
}
