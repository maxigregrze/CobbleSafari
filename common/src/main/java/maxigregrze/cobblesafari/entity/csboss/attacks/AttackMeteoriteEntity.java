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
 * Falling rock: driven by the attack (fast descent). Variant
 * {@code METEORITE} (rock texture, flame trail) or {@code DRACO} (draco-meteor texture,
 * soul flame trail). Rendered via the corresponding block model.
 */
public class AttackMeteoriteEntity extends AbstractAttackEntity {

    private static final EntityDataAccessor<Boolean> DATA_DRACO =
            SynchedEntityData.defineId(AttackMeteoriteEntity.class, EntityDataSerializers.BOOLEAN);
    /** Render scale (1 = normal). Enables 0→1 growth (base_rock_3). */
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(AttackMeteoriteEntity.class, EntityDataSerializers.FLOAT);
    /** Tumble angle (degrees) applied on all 3 axes at render time. */
    private static final EntityDataAccessor<Float> DATA_SPIN =
            SynchedEntityData.defineId(AttackMeteoriteEntity.class, EntityDataSerializers.FLOAT);

    public AttackMeteoriteEntity(EntityType<? extends AttackMeteoriteEntity> type, Level level) {
        super(type, level);
    }

    public static AttackMeteoriteEntity spawn(ServerLevel level, double x, double y, double z,
                                              int sessionId, boolean draco) {
        AttackMeteoriteEntity e = new AttackMeteoriteEntity(ModEntities.ATTACK_METEORITE, level);
        e.setSessionId(sessionId);
        e.setDraco(draco);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DRACO, false);
        builder.define(DATA_SCALE, 1.0F);
        builder.define(DATA_SPIN, 0.0F);
    }

    public boolean isDraco() {
        return this.entityData.get(DATA_DRACO);
    }

    public void setDraco(boolean draco) {
        this.entityData.set(DATA_DRACO, draco);
    }

    public float getRenderScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public void setRenderScale(float scale) {
        this.entityData.set(DATA_SCALE, scale);
    }

    public float getSpin() {
        return this.entityData.get(DATA_SPIN);
    }

    public void setSpin(float spin) {
        this.entityData.set(DATA_SPIN, spin);
    }

    @Override
    protected void clientTick() {
        // Particle trail following the fall (emitted each tick on the client).
        var particle = isDraco() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        for (int i = 0; i < 4; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 1.2;
            double oz = (this.random.nextDouble() - 0.5) * 1.2;
            this.level().addParticle(particle,
                    this.getX() + ox, this.getY() + 0.5 + this.random.nextDouble(), this.getZ() + oz,
                    0.0, 0.05, 0.0);
        }
    }

    @Override
    protected int maxLifespan() {
        return 100;
    }
}
