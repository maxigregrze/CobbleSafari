package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.csboss.CsBossDefinition;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Summon portal: flat 5×5 disc of 3 textured layers
 * ({@code csboss_spawnportal_type{T}_layer{1..3}}). Appears above the arena center at the boss
 * spawn height and stays there <b>for the entire fight</b>. Its scale ({@link #getAnim()})
 * is server-driven by {@link maxigregrze.cobblesafari.csboss.BossBattleManager}:
 * 0 → 1 (opening), 1 (combat), 1 → 0 (closing). Lifetime managed by the session, not by
 * {@link AbstractAttackEntity}'s TTL.
 */
public class CsBossPortalEntity extends AbstractAttackEntity {

    private static final EntityDataAccessor<Integer> DATA_PORTAL_TYPE =
            SynchedEntityData.defineId(CsBossPortalEntity.class, EntityDataSerializers.INT);
    /** Server-driven scale 0..1 (opening / combat / closing) → client rendering. */
    private static final EntityDataAccessor<Float> DATA_ANIM =
            SynchedEntityData.defineId(CsBossPortalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PORTAL_SIZE =
            SynchedEntityData.defineId(CsBossPortalEntity.class, EntityDataSerializers.FLOAT);

    public CsBossPortalEntity(EntityType<? extends CsBossPortalEntity> type, Level level) {
        super(type, level);
    }

    public static CsBossPortalEntity spawn(ServerLevel level, double x, double y, double z,
                                           int sessionId, int portalType, float portalSize) {
        CsBossPortalEntity e = new CsBossPortalEntity(ModEntities.CSBOSS_PORTAL, level);
        e.setSessionId(sessionId);
        e.setPortalType(portalType);
        e.setPortalSize(portalSize);
        e.setAnim(0.0F);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PORTAL_TYPE, 1);
        builder.define(DATA_ANIM, 0.0F);
        builder.define(DATA_PORTAL_SIZE, (float) CsBossDefinition.DEFAULT_PORTAL_SIZE);
    }

    public float getPortalSize() {
        return this.entityData.get(DATA_PORTAL_SIZE);
    }

    public void setPortalSize(float portalSize) {
        this.entityData.set(DATA_PORTAL_SIZE, Math.max(0.0F, portalSize));
    }

    public int getPortalType() {
        return this.entityData.get(DATA_PORTAL_TYPE);
    }

    public void setPortalType(int portalType) {
        this.entityData.set(DATA_PORTAL_TYPE, Math.max(1, portalType));
    }

    public float getAnim() {
        return this.entityData.get(DATA_ANIM);
    }

    public void setAnim(float anim) {
        this.entityData.set(DATA_ANIM, Mth.clamp(anim, 0.0F, 1.0F));
    }

    @Override
    protected int maxLifespan() {
        return Integer.MAX_VALUE; // lifetime managed by the session (close / loss / finalize)
    }
}
