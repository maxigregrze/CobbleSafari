package maxigregrze.cobblesafari.entity;

import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Hyperspace boat. Behaves like a vanilla {@link Boat} but drops the Hyperspace boat item instead of
 * resolving a drop from the (unused) wood variant. The variant stays at its default; the renderer
 * supplies the Hyperspace texture directly, so the variant is never consulted for appearance.
 */
public class HyperspaceBoatEntity extends Boat {

    public HyperspaceBoatEntity(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
    }

    /** Position constructor used by the boat item when placing on water. */
    public HyperspaceBoatEntity(Level level, double x, double y, double z) {
        this(ModEntities.HYPERSPACE_BOAT, level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public Item getDropItem() {
        return ModItems.HYPERSPACE_BOAT;
    }
}
