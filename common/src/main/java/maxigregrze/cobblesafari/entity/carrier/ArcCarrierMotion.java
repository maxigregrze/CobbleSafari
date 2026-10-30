package maxigregrze.cobblesafari.entity.carrier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/**
 * Arc de {@code from} à {@code to} : interpolation adoucie, plus une bosse de {@code lift} blocs
 * en Y (montée puis dépose). Utilisé par le retour des joueurs en fin de combat CSBoss.
 */
public record ArcCarrierMotion(Vec3 from, Vec3 to, double lift, int duration) implements CarrierMotion {

    public static final String TYPE = "arc";

    @Override
    public Vec3 position(int elapsed) {
        double t = Math.min(1.0, Math.max(0.0, elapsed / (double) Math.max(1, duration)));
        double e = CarrierMotion.easeInOutCubic(t);
        Vec3 base = from.add(to.subtract(from).scale(e));
        return base.add(0.0, lift * Math.sin(Math.PI * t), 0.0);
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CarrierMotion.putVec(tag, "From", from);
        CarrierMotion.putVec(tag, "To", to);
        tag.putDouble("Lift", lift);
        tag.putInt("Duration", duration);
        return tag;
    }

    static ArcCarrierMotion load(CompoundTag tag) {
        return new ArcCarrierMotion(CarrierMotion.getVec(tag, "From"), CarrierMotion.getVec(tag, "To"),
                tag.getDouble("Lift"), Math.max(1, tag.getInt("Duration")));
    }
}
