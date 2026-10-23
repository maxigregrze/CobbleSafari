package maxigregrze.cobblesafari.item.ultraloot;

import maxigregrze.cobblesafari.entity.projectile.BlacephalonBombEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;

public class BlacephalonBombItem extends UltraBombItem {

    public BlacephalonBombItem(Properties properties) {
        super(properties, "blacephalon_bomb");
    }

    @Override
    protected ThrowableItemProjectile createProjectile(Level level, LivingEntity thrower) {
        return new BlacephalonBombEntity(level, thrower);
    }

    @Override
    protected ThrowableItemProjectile createProjectile(Level level, double x, double y, double z) {
        return new BlacephalonBombEntity(level, x, y, z);
    }
}
