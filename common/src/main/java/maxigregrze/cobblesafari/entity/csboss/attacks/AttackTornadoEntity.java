package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Tornade de {@code base_flying_4} (plan 199 § 10.3) : visuel du modèle {@code whirlwind_display}
 * empilé sur 3 étages. Déplacement, contact et prise par un porteur sont pilotés par
 * {@code WhirlwindChaseAttack}.
 */
public class AttackTornadoEntity extends AbstractAttackEntity {

    private static final int LIFESPAN = 300; // filet anti-fuite (l'attaque dure 240 t)

    private static final EntityDataAccessor<Float> DATA_ALPHA =
            SynchedEntityData.defineId(AttackTornadoEntity.class, EntityDataSerializers.FLOAT);

    public AttackTornadoEntity(EntityType<? extends AttackTornadoEntity> type, Level level) {
        super(type, level);
    }

    public static AttackTornadoEntity spawn(ServerLevel level, double x, double y, double z, int sessionId) {
        AttackTornadoEntity e = new AttackTornadoEntity(ModEntities.ATTACK_TORNADO, level);
        e.setSessionId(sessionId);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ALPHA, 1.0F);
    }

    public float getAlpha() {
        return this.entityData.get(DATA_ALPHA);
    }

    public void setAlpha(float alpha) {
        this.entityData.set(DATA_ALPHA, alpha);
    }

    @Override
    protected int maxLifespan() {
        return LIFESPAN;
    }

    @Override
    protected void clientTick() {
        for (int i = 0; i < 2; i++) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            double r = 0.6 + this.random.nextDouble() * 0.8;
            this.level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(a) * r, getY() + 0.1,
                    getZ() + Math.sin(a) * r, -Math.sin(a) * 0.15, 0.05, Math.cos(a) * 0.15);
        }
        if (this.random.nextInt(4) == 0) {
            this.level().addParticle(ParticleTypes.SMALL_GUST, getX(), getY() + 1.0 + this.random.nextDouble() * 2.0,
                    getZ(), 0.0, 0.0, 0.0);
        }
    }
}
