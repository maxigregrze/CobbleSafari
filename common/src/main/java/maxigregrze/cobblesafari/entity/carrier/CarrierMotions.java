package maxigregrze.cobblesafari.entity.carrier;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/** Relecture NBT des {@link CarrierMotion} par type. */
public final class CarrierMotions {

    private CarrierMotions() {}

    public static CompoundTag save(CarrierMotion motion) {
        CompoundTag tag = motion.save();
        tag.putString("Type", motion.type());
        return tag;
    }

    @Nullable
    public static CarrierMotion load(CompoundTag tag) {
        return switch (tag.getString("Type")) {
            case ArcCarrierMotion.TYPE -> ArcCarrierMotion.load(tag);
            case OrbitCarrierMotion.TYPE -> OrbitCarrierMotion.load(tag);
            default -> null;
        };
    }
}
