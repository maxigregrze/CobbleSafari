package maxigregrze.cobblesafari.entity.projectile;

import maxigregrze.cobblesafari.csboss.BossBattleManager;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownBalmEntity extends ThrowableItemProjectile {

    private static final int TRAIL_COLOR = 0xFBE57A;

    public ThrownBalmEntity(EntityType<? extends ThrownBalmEntity> type, Level level) {
        super(type, level);
    }

    public ThrownBalmEntity(Level level, LivingEntity thrower) {
        super(ModEntities.THROWN_BALM, thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BALM;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.2;
            double oy = (random.nextDouble() - 0.5) * 0.2;
            double oz = (random.nextDouble() - 0.5) * 0.2;
            level().addParticle(
                    ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, TRAIL_COLOR),
                    getX() + ox, getY() + oy, getZ() + oz,
                    0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide() && result.getEntity() instanceof CsBossEntity boss) {
            BossBattleManager.onBalmHit(boss);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            spawnImpactParticles();
            discard();
        }
    }

    private void spawnImpactParticles() {
        if (level() instanceof ServerLevel serverLevel) {
            ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, getItem());
            serverLevel.sendParticles(particle, getX(), getY(), getZ(), 12, 0.15, 0.15, 0.15, 0.25);
        }
    }
}
