package maxigregrze.cobblesafari.entity.carrier;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Réglages d'un {@link CarrierEntity}.
 *
 * @param allowDismount   {@code false} : Maj ne libère pas le passager (mixin dédié)
 * @param trail           particule émise côté serveur à chaque tick, {@code null} = aucune
 * @param releaseVelocity impulsion unique appliquée au passager à la dépose ({@link Vec3#ZERO} = aucune)
 * @param clearFireOnGrab éteint le passager à la prise
 * @param onRelease       callback serveur à la dépose (non sauvegardé)
 */
public record CarrierOptions(boolean allowDismount, @Nullable SimpleParticleType trail, Vec3 releaseVelocity,
                             boolean clearFireOnGrab, @Nullable Runnable onRelease) {

    public static CarrierOptions locked() {
        return new CarrierOptions(false, null, Vec3.ZERO, false, null);
    }
}
