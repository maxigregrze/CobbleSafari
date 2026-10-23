package maxigregrze.cobblesafari.entity.projectile;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Stellar Canon shell (action plan 192, section 7.2): snowball ballistics, sticks where it lands and
 * sends up campfire signal smoke for 16 seconds.
 */
public class CelesteelaProjectileEntity extends StickyProjectileEntity {

    private static final double GRAVITY = 0.03;
    private static final double DRAG = 0.99;
    private static final int SMOKE_TICKS = 320;
    private static final float SMOKE_CHANCE = 0.3f;

    public CelesteelaProjectileEntity(EntityType<? extends CelesteelaProjectileEntity> type, Level level) {
        super(type, level);
    }

    public CelesteelaProjectileEntity(Level level, LivingEntity shooter) {
        this(ModEntities.CELESTEELA_PROJECTILE, level);
        initFromShooter(shooter);
    }

    @Override
    protected Vec3 nextFlightMotion(Vec3 motion) {
        return motion.scale(DRAG).add(0.0, -GRAVITY, 0.0);
    }

    @Override
    protected int stuckLifetimeTicks() {
        return SMOKE_TICKS;
    }

    @Override
    protected void stuckTick() {
        if (level().isClientSide() && random.nextFloat() < SMOKE_CHANCE) {
            level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                    getX() + (random.nextDouble() - 0.5) * 0.2, getY() + 0.2, getZ() + (random.nextDouble() - 0.5) * 0.2,
                    0.0, 0.07, 0.0);
        }
    }
}
