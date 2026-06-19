package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Burrowing dirt pile (base_ground_1): ground-attack equivalent of the shadow, rendered
 * with the {@code attack_digdirt} model (meteorite texture). Driven by the attack (follows the player).
 * Also supports optional scale, vibration, dirt particles and alpha (defaults neutral).
 */
public class AttackDigdirtEntity extends AbstractAttackEntity {

    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(AttackDigdirtEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_VIBRATING =
            SynchedEntityData.defineId(AttackDigdirtEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DIRT_PARTICLES =
            SynchedEntityData.defineId(AttackDigdirtEntity.class, EntityDataSerializers.BOOLEAN);
    /** {@code true} ⇒ rendered with dirt texture (base_ground_2) instead of meteorite texture. */
    private static final EntityDataAccessor<Boolean> DATA_DIRT_MODEL =
            SynchedEntityData.defineId(AttackDigdirtEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_ALPHA =
            SynchedEntityData.defineId(AttackDigdirtEntity.class, EntityDataSerializers.FLOAT);

    public AttackDigdirtEntity(EntityType<? extends AttackDigdirtEntity> type, Level level) {
        super(type, level);
    }

    public static AttackDigdirtEntity spawn(ServerLevel level, double x, double y, double z, int sessionId) {
        AttackDigdirtEntity e = new AttackDigdirtEntity(ModEntities.ATTACK_DIGDIRT, level);
        e.setSessionId(sessionId);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SCALE, 1.0F);
        builder.define(DATA_VIBRATING, false);
        builder.define(DATA_DIRT_PARTICLES, false);
        builder.define(DATA_DIRT_MODEL, false);
        builder.define(DATA_ALPHA, 1.0F);
    }

    public float getRenderScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public void setRenderScale(float scale) {
        this.entityData.set(DATA_SCALE, scale);
    }

    public boolean isVibrating() {
        return this.entityData.get(DATA_VIBRATING);
    }

    public void setVibrating(boolean vibrating) {
        this.entityData.set(DATA_VIBRATING, vibrating);
    }

    public boolean emitsDirtParticles() {
        return this.entityData.get(DATA_DIRT_PARTICLES);
    }

    public void setDirtParticles(boolean dirtParticles) {
        this.entityData.set(DATA_DIRT_PARTICLES, dirtParticles);
    }

    public boolean usesDirtModel() {
        return this.entityData.get(DATA_DIRT_MODEL);
    }

    public void setDirtModel(boolean dirtModel) {
        this.entityData.set(DATA_DIRT_MODEL, dirtModel);
    }

    public float getAlpha() {
        return this.entityData.get(DATA_ALPHA);
    }

    public void setAlpha(float alpha) {
        this.entityData.set(DATA_ALPHA, alpha);
    }

    @Override
    protected void clientTick() {
        if (!emitsDirtParticles()) {
            return;
        }
        BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
        for (int i = 0; i < 3; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 2.5;
            double oz = (this.random.nextDouble() - 0.5) * 2.5;
            this.level().addParticle(dirt,
                    this.getX() + ox, this.getY() + 0.1, this.getZ() + oz,
                    0.0, 0.04, 0.0);
        }
    }

    @Override
    protected int maxLifespan() {
        return 400;
    }
}
