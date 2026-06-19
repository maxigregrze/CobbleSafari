package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Flat ground decal: follows a player, driven by the attack.
 * {@code large} variant (3×3, texture {@code attack_shadow_large.png}) and adjustable
 * opacity ({@code alpha}) for the {@code base_ghost_1} fade.
 */
public class AttackShadowEntity extends AbstractAttackEntity {

    private static final EntityDataAccessor<Boolean> DATA_LARGE =
            SynchedEntityData.defineId(AttackShadowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_ALPHA =
            SynchedEntityData.defineId(AttackShadowEntity.class, EntityDataSerializers.FLOAT);

    public AttackShadowEntity(EntityType<? extends AttackShadowEntity> type, Level level) {
        super(type, level);
    }

    public static AttackShadowEntity spawn(ServerLevel level, double x, double y, double z, int sessionId) {
        return spawn(level, x, y, z, sessionId, false);
    }

    public static AttackShadowEntity spawn(ServerLevel level, double x, double y, double z, int sessionId, boolean large) {
        AttackShadowEntity e = new AttackShadowEntity(ModEntities.ATTACK_SHADOW, level);
        e.setSessionId(sessionId);
        e.setLarge(large);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LARGE, false);
        builder.define(DATA_ALPHA, 1.0F);
    }

    public boolean isLarge() {
        return this.entityData.get(DATA_LARGE);
    }

    public void setLarge(boolean large) {
        this.entityData.set(DATA_LARGE, large);
    }

    public float getAlpha() {
        return this.entityData.get(DATA_ALPHA);
    }

    public void setAlpha(float alpha) {
        this.entityData.set(DATA_ALPHA, alpha);
    }

    @Override
    protected int maxLifespan() {
        return 400;
    }
}
