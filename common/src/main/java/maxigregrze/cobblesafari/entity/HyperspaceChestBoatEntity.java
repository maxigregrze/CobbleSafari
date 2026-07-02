package maxigregrze.cobblesafari.entity;

import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Hyperspace chest boat. Like a vanilla {@link ChestBoat} but drops the Hyperspace chest-boat item. */
public class HyperspaceChestBoatEntity extends ChestBoat {

    public HyperspaceChestBoatEntity(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
    }

    /** Position constructor used by the chest-boat item when placing on water. */
    public HyperspaceChestBoatEntity(Level level, double x, double y, double z) {
        this(ModEntities.HYPERSPACE_CHEST_BOAT, level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public Item getDropItem() {
        return ModItems.HYPERSPACE_CHEST_BOAT;
    }
}
