package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Portail d'invocation (plan 122 § 3.2) : disque plat 5×5 de 3 couches texturées
 * ({@code csboss_spawnportal_type{T}_layer{1..3}}). Apparaît au-dessus du centre d'arène, à la
 * hauteur d'apparition du boss, et y reste <b>tout le combat</b>. Son échelle ({@link #getAnim()})
 * est pilotée serveur par {@link maxigregrze.cobblesafari.csboss.BossBattleManager} :
 * 0 → 1 (ouverture), 1 (combat), 1 → 0 (fermeture). Durée de vie gérée par la session, pas par le
 * TTL d'{@link AbstractAttackEntity}.
 */
public class CsBossPortalEntity extends AbstractAttackEntity {

    private static final EntityDataAccessor<Integer> DATA_PORTAL_TYPE =
            SynchedEntityData.defineId(CsBossPortalEntity.class, EntityDataSerializers.INT);
    /** Échelle 0..1 pilotée serveur (ouverture / combat / fermeture) → rendu client. */
    private static final EntityDataAccessor<Float> DATA_ANIM =
            SynchedEntityData.defineId(CsBossPortalEntity.class, EntityDataSerializers.FLOAT);

    public CsBossPortalEntity(EntityType<? extends CsBossPortalEntity> type, Level level) {
        super(type, level);
    }

    public static CsBossPortalEntity spawn(ServerLevel level, double x, double y, double z,
                                           int sessionId, int portalType) {
        CsBossPortalEntity e = new CsBossPortalEntity(ModEntities.CSBOSS_PORTAL, level);
        e.setSessionId(sessionId);
        e.setPortalType(portalType);
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
        return Integer.MAX_VALUE; // durée de vie gérée par la session (close / loss / finalize)
    }
}
