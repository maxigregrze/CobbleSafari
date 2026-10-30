package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Zone colorée statique au sol (retours plan 199) : même rendu que l'onde de choc
 * ({@link AttackShockwaveEntity}) mais immobile, entre deux rayons, à opacité réglable. Purement
 * visuelle : l'attaque qui la pose gère seule dégâts et timing (Arc-en-ciel, Prescience).
 */
public class AttackZoneRingEntity extends AbstractAttackEntity {

    private static final EntityDataAccessor<Float> DATA_INNER =
            SynchedEntityData.defineId(AttackZoneRingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_OUTER =
            SynchedEntityData.defineId(AttackZoneRingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(AttackZoneRingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ALPHA =
            SynchedEntityData.defineId(AttackZoneRingEntity.class, EntityDataSerializers.FLOAT);

    private int lifespan = 300;

    public AttackZoneRingEntity(EntityType<? extends AttackZoneRingEntity> type, Level level) {
        super(type, level);
    }

    public static AttackZoneRingEntity spawn(ServerLevel level, double x, double floorY, double z, int sessionId,
                                             float inner, float outer, int colorRgb, float alpha, int lifespan) {
        AttackZoneRingEntity e = new AttackZoneRingEntity(ModEntities.ATTACK_ZONE_RING, level);
        e.setSessionId(sessionId);
        e.lifespan = lifespan;
        e.entityData.set(DATA_INNER, inner);
        e.entityData.set(DATA_OUTER, outer);
        e.entityData.set(DATA_COLOR, colorRgb);
        e.entityData.set(DATA_ALPHA, alpha);
        e.moveTo(x, floorY, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_INNER, 0.0F);
        builder.define(DATA_OUTER, 1.0F);
        builder.define(DATA_COLOR, 0xFFFFFF);
        builder.define(DATA_ALPHA, 0.5F);
    }

    public float getInner() {
        return this.entityData.get(DATA_INNER);
    }

    public float getOuter() {
        return this.entityData.get(DATA_OUTER);
    }

    public int getColorRgb() {
        return this.entityData.get(DATA_COLOR);
    }

    public float getAlpha() {
        return this.entityData.get(DATA_ALPHA);
    }

    public void setAlpha(float alpha) {
        this.entityData.set(DATA_ALPHA, alpha);
    }

    @Override
    protected int maxLifespan() {
        return lifespan;
    }

    /** Grande zone : jamais coupée par le frustum quand son centre sort de l'écran. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(getOuter());
    }
}
