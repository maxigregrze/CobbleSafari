package maxigregrze.cobblesafari.entity.carrier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/**
 * Trajectoire d'un {@link CarrierEntity} : position du porteur à chaque tick écoulé.
 * Sérialisable pour qu'un trajet reprenne après une déconnexion (plan 199 § 6.4).
 */
public interface CarrierMotion {

    /** Position du porteur au tick {@code elapsed} (0 = départ, {@link #duration()} = arrivée). */
    Vec3 position(int elapsed);

    /** Durée totale du trajet, en ticks. */
    int duration();

    /** Identifiant de type (clé de {@link CarrierMotions}). */
    String type();

    /** Paramètres du mouvement, relus par {@link CarrierMotions#load}. */
    CompoundTag save();

    /** Courbe d'accélération/décélération douce, {@code t} ∈ [0, 1]. */
    static double easeInOutCubic(double t) {
        return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
    }

    static void putVec(CompoundTag tag, String key, Vec3 v) {
        CompoundTag c = new CompoundTag();
        c.putDouble("X", v.x);
        c.putDouble("Y", v.y);
        c.putDouble("Z", v.z);
        tag.put(key, c);
    }

    static Vec3 getVec(CompoundTag tag, String key) {
        CompoundTag c = tag.getCompound(key);
        return new Vec3(c.getDouble("X"), c.getDouble("Y"), c.getDouble("Z"));
    }
}
