package maxigregrze.cobblesafari.entity.carrier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Orbite autour d'un centre (éventuellement mobile côté serveur) : {@code turns} tours de rayon
 * {@code radius}, avec une montée linéaire de {@code rise} blocs. Seul le dernier centre connu est
 * sauvegardé : après reprise, l'orbite se poursuit autour d'un centre fixe.
 */
public final class OrbitCarrierMotion implements CarrierMotion {

    public static final String TYPE = "orbit";

    private final Supplier<Vec3> center;
    private final double radius;
    private final double startAngle;
    private final double turns;
    private final double baseY;
    private final double rise;
    private final int duration;
    private Vec3 lastCenter;

    public OrbitCarrierMotion(Supplier<Vec3> center, double radius, double startAngle, double turns,
                              double baseY, double rise, int duration) {
        this.center = center;
        this.radius = radius;
        this.startAngle = startAngle;
        this.turns = turns;
        this.baseY = baseY;
        this.rise = rise;
        this.duration = Math.max(1, duration);
        this.lastCenter = center.get();
    }

    @Override
    public Vec3 position(int elapsed) {
        Vec3 c = center.get();
        if (c != null) {
            lastCenter = c;
        }
        double t = Math.min(1.0, Math.max(0.0, elapsed / (double) duration));
        double angle = startAngle + Math.PI * 2.0 * turns * t;
        return new Vec3(lastCenter.x + Math.cos(angle) * radius, baseY + rise * t,
                lastCenter.z + Math.sin(angle) * radius);
    }

    @Override
    public int duration() {
        return duration;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CarrierMotion.putVec(tag, "Center", lastCenter);
        tag.putDouble("Radius", radius);
        tag.putDouble("StartAngle", startAngle);
        tag.putDouble("Turns", turns);
        tag.putDouble("BaseY", baseY);
        tag.putDouble("Rise", rise);
        tag.putInt("Duration", duration);
        return tag;
    }

    static OrbitCarrierMotion load(CompoundTag tag) {
        Vec3 fixed = CarrierMotion.getVec(tag, "Center");
        return new OrbitCarrierMotion(() -> fixed, tag.getDouble("Radius"), tag.getDouble("StartAngle"),
                tag.getDouble("Turns"), tag.getDouble("BaseY"), tag.getDouble("Rise"), tag.getInt("Duration"));
    }
}
