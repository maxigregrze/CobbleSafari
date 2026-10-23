package maxigregrze.cobblesafari.item.ultraloot;

import maxigregrze.cobblesafari.entity.projectile.XurkitreeBombEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;

public class XurkitreeBombItem extends UltraBombItem {

    public XurkitreeBombItem(Properties properties) {
        super(properties, "xurkitree_bomb");
    }

    @Override
    protected ThrowableItemProjectile createProjectile(Level level, LivingEntity thrower) {
        return new XurkitreeBombEntity(level, thrower);
    }

    @Override
    protected ThrowableItemProjectile createProjectile(Level level, double x, double y, double z) {
        return new XurkitreeBombEntity(level, x, y, z);
    }
}
