package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Nœud invisible de propagation (plan 107 § 4.4, révisé § 107.bis). Sa position est exprimée en
 * coordonnées polaires <b>dans le repère du boss</b> ({@code angleDeg} relatif à l'orientation du
 * boss, {@code radius} horizontal, {@code localY} vertical) : recalculée chaque tick, elle suit donc
 * la rotation du boss (toutes les entités tournent autour de lui). Après 5 ticks, le core crée le
 * core suivant (radius + 1) et une colonne de tiges (au‑dessus). Cap horizontal ≤ 20/direction.
 */
public class AttackDistortionStemCoreEntity extends AbstractAttackEntity {

    public static final int STEM_SEED = 4; // 4 ⇒ 5 tiges/colonne avec la base
    private static final int WAIT_TICKS = 5;

    private static final String KEY_ANGLE = "Angle";
    private static final String KEY_RADIUS = "Radius";
    private static final String KEY_LOCAL_Y = "LocalY";
    private static final String KEY_CORES = "Cores";

    private double angleDeg;
    private double radius;
    private double localY;
    private int coresRemaining;
    private boolean propagated;

    public AttackDistortionStemCoreEntity(EntityType<? extends AttackDistortionStemCoreEntity> type, Level level) {
        super(type, level);
    }

    public static AttackDistortionStemCoreEntity spawn(ServerLevel level, int sessionId, CsBossEntity boss,
                                                       double angleDeg, double radius, double localY, int coresRemaining) {
        AttackDistortionStemCoreEntity e =
                new AttackDistortionStemCoreEntity(ModEntities.ATTACK_DISTORTION_STEM_CORE, level);
        e.setSessionId(sessionId);
        e.angleDeg = angleDeg;
        e.radius = radius;
        e.localY = localY;
        e.coresRemaining = coresRemaining;
        double[] p = DistortionFrame.world(boss, angleDeg, radius, localY);
        e.moveTo(p[0], p[1], p[2], 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        CsBossEntity boss = boss(level);
        if (boss == null) {
            return;
        }
        // Suit le repère tournant du boss.
        double[] p = DistortionFrame.world(boss, angleDeg, radius, localY);
        setPos(p[0], p[1], p[2]);

        if (propagated || this.age < WAIT_TICKS) {
            return;
        }
        propagated = true;
        BossBattleSession session = session();

        AttackDistortionStemEntity stem = AttackDistortionStemEntity.spawn(
                level, this.sessionId, boss, angleDeg, radius, localY + 1.0, STEM_SEED);
        if (session != null) {
            session.trackAttackEntity(stem);
        }
        if (coresRemaining > 0) {
            AttackDistortionStemCoreEntity next = AttackDistortionStemCoreEntity.spawn(
                    level, this.sessionId, boss, angleDeg, radius + 1.0, localY, coresRemaining - 1);
            if (session != null) {
                session.trackAttackEntity(next);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.angleDeg = tag.getDouble(KEY_ANGLE);
        this.radius = tag.getDouble(KEY_RADIUS);
        this.localY = tag.getDouble(KEY_LOCAL_Y);
        this.coresRemaining = tag.getInt(KEY_CORES);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble(KEY_ANGLE, this.angleDeg);
        tag.putDouble(KEY_RADIUS, this.radius);
        tag.putDouble(KEY_LOCAL_Y, this.localY);
        tag.putInt(KEY_CORES, this.coresRemaining);
    }

    @Override
    protected int maxLifespan() {
        return 200;
    }
}
