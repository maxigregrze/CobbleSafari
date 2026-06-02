package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Attaque de référence {@code test} (plan 100 § 12.2/12.3) : volées de 8 bullets dans les
 * 8 directions de boussole, 5 ticks entre chaque volée, +22,5° de rotation par volée,
 * puis 80 ticks d'attente. Les 18 attaques typées {@code base_<type>_1} en héritent telles quelles.
 */
public class CsBossTestAttack implements CsBossAttack {

    private static final int VOLLEY_COUNT = 8;
    private static final int VOLLEY_DELAY = 5;       // ticks entre volées
    private static final int BULLETS_PER_VOLLEY = 8; // 8 directions de boussole
    private static final double ROTATION_STEP_DEG = 22.5;
    private static final double BULLET_SPEED = 0.6;  // blocs/tick
    private static final int TAIL_DELAY = 80;        // attente après la dernière volée

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
        // Ancrage vertical sur le bloc trigger : la bullet (5 de haut) couvre blockY .. blockY+5.
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
