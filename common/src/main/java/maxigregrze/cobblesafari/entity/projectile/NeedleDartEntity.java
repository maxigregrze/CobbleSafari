package maxigregrze.cobblesafari.entity.projectile;

import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Needle Dart projectile (action plan 192, section 4.2). Arrow damage is speed × base damage, so the
 * +50 % speed alone yields the +50 % damage: the base damage stays vanilla's 2.0.
 */
public class NeedleDartEntity extends AbstractArrow {

    /** Same as {@link ThrownRedChainEntity}: the throwable default (0.03) divided by four. */
    private static final double GRAVITY = 0.0075;
    private static final float SPEED_MULTIPLIER = 1.5f;

    public NeedleDartEntity(EntityType<? extends NeedleDartEntity> type, Level level) {
        super(type, level);
    }

    public NeedleDartEntity(Level level, LivingEntity owner, ItemStack pickup, @Nullable ItemStack weapon) {
        super(ModEntities.POIPOLE_ARROW, owner, level, pickup, weapon);
    }

    public NeedleDartEntity(Level level, double x, double y, double z, ItemStack pickup) {
        super(ModEntities.POIPOLE_ARROW, x, y, z, level, pickup, null);
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity * SPEED_MULTIPLIER, inaccuracy);
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.POIPOLE_ARROW);
    }
}
