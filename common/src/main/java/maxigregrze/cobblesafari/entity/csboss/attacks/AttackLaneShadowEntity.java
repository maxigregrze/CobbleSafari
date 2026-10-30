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
 * Ombre « couloir » étirable (retours plan 199) : l'ombre d'attaque vanilla du mod étirée en bande
 * le long d'une trajectoire (bouts arrondis), pour télégraphier le piqué de {@code base_flying_1}
 * sans particules. Origine = début du couloir ; cap en degrés (0 = +X), longueur et largeur en blocs.
 */
public class AttackLaneShadowEntity extends AbstractAttackEntity {

    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(AttackLaneShadowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WIDTH =
            SynchedEntityData.defineId(AttackLaneShadowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEADING =
            SynchedEntityData.defineId(AttackLaneShadowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ALPHA =
            SynchedEntityData.defineId(AttackLaneShadowEntity.class, EntityDataSerializers.FLOAT);

    private static final int LIFESPAN = 200;

    public AttackLaneShadowEntity(EntityType<? extends AttackLaneShadowEntity> type, Level level) {
        super(type, level);
    }

    public static AttackLaneShadowEntity spawn(ServerLevel level, double x, double floorY, double z, int sessionId,
                                               float headingDeg, float length, float width) {
        AttackLaneShadowEntity e = new AttackLaneShadowEntity(ModEntities.ATTACK_LANE_SHADOW, level);
        e.setSessionId(sessionId);
        e.entityData.set(DATA_HEADING, headingDeg);
        e.entityData.set(DATA_LENGTH, length);
        e.entityData.set(DATA_WIDTH, width);
        e.moveTo(x, floorY, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LENGTH, 1.0F);
        builder.define(DATA_WIDTH, 1.5F);
        builder.define(DATA_HEADING, 0.0F);
        builder.define(DATA_ALPHA, 0.0F);
    }

    public float getLength() {
        return this.entityData.get(DATA_LENGTH);
    }

    public float getWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    public float getHeading() {
        return this.entityData.get(DATA_HEADING);
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
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(getLength());
    }
}
