package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Distortion flower (plan 113, distortion_2/3): rendered with the block model
 * {@code distortion_flower_carpet}. 2 s after spawning, it is replaced by a column of
 * vertical stems (6-block-high wall) that stays active for 10 s.
 */
public class AttackDistortionFlowerEntity extends AbstractAttackEntity {

    private static final int BLOOM_AT = 40;       // 2 s before growing the wall
    private static final int STEM_STACKS = 5;     // 5 + base = 6 stems high
    private boolean bloomed;

    public AttackDistortionFlowerEntity(EntityType<? extends AttackDistortionFlowerEntity> type, Level level) {
        super(type, level);
    }

    public static AttackDistortionFlowerEntity spawn(ServerLevel level, double x, double y, double z, int sessionId) {
        AttackDistortionFlowerEntity e = new AttackDistortionFlowerEntity(ModEntities.ATTACK_DISTORTION_FLOWER, level);
        e.setSessionId(sessionId);
        e.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        if (!bloomed && this.age >= BLOOM_AT) {
            bloomed = true;
            AttackDistortionStemEntity stem = AttackDistortionStemEntity.spawnVertical(
                    level, this.sessionId, getX(), getY(), getZ(), STEM_STACKS);
            BossBattleSession session = session();
            if (session != null) {
                session.trackAttackEntity(stem);
            }
            discard();
        }
    }

    @Override
    protected int maxLifespan() {
        return 60;
    }
}
