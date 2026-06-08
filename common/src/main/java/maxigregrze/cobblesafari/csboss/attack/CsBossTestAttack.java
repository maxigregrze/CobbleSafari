package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Reference {@code test} attack (plan 100 § 12.2/12.3): volleys of 8 bullets in the
 * 8 compass directions, 5 ticks between each volley, +22.5° rotation per volley,
 * then 80 ticks of waiting. The 18 typed {@code base_<type>_1} attacks inherit as-is.
 */
public class CsBossTestAttack implements CsBossAttack {

    private static final int VOLLEY_COUNT = 8;
    private static final int VOLLEY_DELAY = 5;       // ticks between volleys
    private static final int BULLETS_PER_VOLLEY = 8; // 8 compass directions
    private static final double ROTATION_STEP_DEG = 22.5;
    private static final double BULLET_SPEED = 0.6;  // blocks/tick
    private static final int TAIL_DELAY = 80;        // wait after the last volley

    private final String id;
    private int tick = -1;
    private int volleysFired = 0;
    private boolean done = false;

    public CsBossTestAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = -1;
        this.volleysFired = 0;
        this.done = false;
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        tick++;

        if (volleysFired < VOLLEY_COUNT && tick == volleysFired * VOLLEY_DELAY) {
            fireVolley(level, session, boss, volleysFired);
            volleysFired++;
        }

        int lastVolleyTick = (VOLLEY_COUNT - 1) * VOLLEY_DELAY;
        if (volleysFired >= VOLLEY_COUNT && tick >= lastVolleyTick + TAIL_DELAY) {
            done = true;
        }
    }

    private void fireVolley(ServerLevel level, BossBattleSession session, CsBossEntity boss, int volleyIndex) {
        boss.triggerAttackAnimation();
        double baseRad = Math.toRadians(volleyIndex * ROTATION_STEP_DEG);
        // Vertical anchor on the trigger block: the bullet (5 tall) covers blockY .. blockY+5.
        double anchorY = session.getTriggerPos().getY();
        Vec3 origin = new Vec3(boss.getX(), anchorY, boss.getZ());
        for (int i = 0; i < BULLETS_PER_VOLLEY; i++) {
            double angle = baseRad + Math.toRadians(i * (360.0 / BULLETS_PER_VOLLEY));
            Vec3 vel = new Vec3(Math.cos(angle) * BULLET_SPEED, 0, Math.sin(angle) * BULLET_SPEED);
            session.trySpawnBullet(level, origin, vel);
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
