package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowballEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * {@code base_ghost_4} (Type C / SPREAD): the boss aims in a random direction and fires a 2×2 shadow
 * ball, then rotates 18° (clockwise or counter-clockwise, chosen randomly at the start and held
 * until the end) before the next shot. One full turn (360°) = 20 shots in 5 s; the boss does
 * two, i.e. 40 shots over 10 s (a spiral sweep).
 */
public class GhostShadowballAttack implements CsBossAttack {

    private static final int THROWS = 40; // 2 rounds of 20 shots
    private static final int INTERVAL = 6; // one shot every 6 ticks (40*6 = 240 t)
    private static final double STEP_DEG = 18.0; // 360° / 20 shots
    private static final double SPEED = 0.4; // ball speed
    private static final double SPAWN_Y_OFFSET = 0.5; // ball base above ground

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private double baseAngle; // first-shot direction (random)
    private double stepRad; // ±18° per shot (random clockwise / counter-clockwise)
    private int tick;
    private int thrown;
    private boolean done;

    public GhostShadowballAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.SPREAD;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.thrown = 0;
        this.done = false;
        this.baseAngle = rng.nextDouble() * Math.PI * 2.0;
        this.stepRad = Math.toRadians(STEP_DEG) * (rng.nextBoolean() ? 1.0 : -1.0);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (thrown < THROWS && tick == thrown * INTERVAL) {
            throwBall(level, session, boss);
            thrown++;
        }
        if (tick >= THROWS * INTERVAL) {
            done = true; // balls already fired finish on their own
        }
        tick++;
    }

    private void throwBall(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        double theta = baseAngle + stepRad * thrown;
        Vec3 velocity = new Vec3(Math.cos(theta), 0, Math.sin(theta)).scale(SPEED);
        double y = session.getTriggerPos().getY() + SPAWN_Y_OFFSET;
        AttackShadowballEntity ball = AttackShadowballEntity.spawn(level,
                boss.getX(), y, boss.getZ(), session.getId(), velocity);
        ball.setMaxTravel(CsBossAttackLib.areaReach(session));
        session.trackAttackEntity(ball);
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), y, boss.getZ(),
                "cobblemon:move.shadowball.actor", SoundSource.HOSTILE, 1.1F, 0.8F);
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
