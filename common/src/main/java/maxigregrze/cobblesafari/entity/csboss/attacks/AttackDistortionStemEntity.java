package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Tige de distorsion semi‑transparente (plan 107 § 4.3, révisé). Position en coordonnées polaires
 * dans le repère du boss (cf. {@link DistortionFrame}) : recalculée chaque tick, elle tourne avec le
 * boss. Un tick après son apparition, elle empile une tige au‑dessus (même angle/rayon, {@code localY+1})
 * tant que son budget {@code stacksRemaining} n'est pas épuisé. Cap vertical ≤ 5 tiges/colonne.
 */
public class AttackDistortionStemEntity extends AbstractAttackEntity {

    /** Mur vertical statique (distortion_2) au lieu du mode orbital radial (distortion_1). */
    private static final EntityDataAccessor<Boolean> DATA_VERTICAL =
            SynchedEntityData.defineId(AttackDistortionStemEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String KEY_ANGLE = "Angle";
    private static final String KEY_RADIUS = "Radius";
    private static final String KEY_LOCAL_Y = "LocalY";
    private static final String KEY_STACKS = "Stacks";

    private double angleDeg;
    private double radius;
    private double localY;
    private int stacksRemaining;
    private boolean spawnedChild;

    public AttackDistortionStemEntity(EntityType<? extends AttackDistortionStemEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VERTICAL, false);
    }

    public boolean isVertical() {
        return this.entityData.get(DATA_VERTICAL);
    }

    public static AttackDistortionStemEntity spawn(ServerLevel level, int sessionId, CsBossEntity boss,
                                                   double angleDeg, double radius, double localY, int stacksRemaining) {
        AttackDistortionStemEntity e = new AttackDistortionStemEntity(ModEntities.ATTACK_DISTORTION_STEM, level);
        e.setSessionId(sessionId);
        e.angleDeg = angleDeg;
        e.radius = radius;
        e.localY = localY;
        e.stacksRemaining = stacksRemaining;
        double[] p = DistortionFrame.world(boss, angleDeg, radius, localY);
        e.moveTo(p[0], p[1], p[2], 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    /** Tige verticale statique (mur), empilée vers le haut, position monde fixe. */
    public static AttackDistortionStemEntity spawnVertical(ServerLevel level, int sessionId,
                                                           double x, double y, double z, int stacksRemaining) {
        AttackDistortionStemEntity e = new AttackDistortionStemEntity(ModEntities.ATTACK_DISTORTION_STEM, level);
        e.setSessionId(sessionId);
        e.stacksRemaining = stacksRemaining;
        e.entityData.set(DATA_VERTICAL, true);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        if (isVertical()) {
            // Mur statique : empile une tige 1 bloc au-dessus, position monde fixe.
            if (!spawnedChild && this.age >= 1) {
                spawnedChild = true;
                if (stacksRemaining > 0) {
                    AttackDistortionStemEntity child = spawnVertical(level, this.sessionId,
                            getX(), getY() + 1.0, getZ(), stacksRemaining - 1);
                    BossBattleSession session = session();
                    if (session != null) {
                        session.trackAttackEntity(child);
                    }
                }
            }
            return;
        }

        CsBossEntity boss = boss(level);
        if (boss == null) {
            return;
        }
        double[] p = DistortionFrame.world(boss, angleDeg, radius, localY);
        setPos(p[0], p[1], p[2]);
        // Oriente la tige radialement (vers l'extérieur du boss) pour le rendu « pointe dehors ».
        double dx = p[0] - boss.getX();
        double dz = p[2] - boss.getZ();
        float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        setYRot(yaw);

        if (!spawnedChild && this.age >= 1) {
            spawnedChild = true;
            if (stacksRemaining > 0) {
                AttackDistortionStemEntity child = AttackDistortionStemEntity.spawn(
                        level, this.sessionId, boss, angleDeg, radius, localY + 1.0, stacksRemaining - 1);
                BossBattleSession session = session();
                if (session != null) {
                    session.trackAttackEntity(child);
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.angleDeg = tag.getDouble(KEY_ANGLE);
        this.radius = tag.getDouble(KEY_RADIUS);
        this.localY = tag.getDouble(KEY_LOCAL_Y);
        this.stacksRemaining = tag.getInt(KEY_STACKS);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble(KEY_ANGLE, this.angleDeg);
        tag.putDouble(KEY_RADIUS, this.radius);
        tag.putDouble(KEY_LOCAL_Y, this.localY);
        tag.putInt(KEY_STACKS, this.stacksRemaining);
    }

    @Override
    protected int maxLifespan() {
        return 200;
    }
}
