package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackWaveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * {@code base_water_2} (plan 110): <b>projectile</b> attack (not per player). Every 2 s (4–5 times)
 * the boss fires an <b>X</b>: the same arc trio of waves in 4 directions, 90° apart, oriented from a
 * random base angle. Each trio's center wave (3 blocks wide) faces its launch direction, the two side
 * waves are flush with its edges but tilted 25° to form an arc. 8 damage + strong knockback on contact.
 */
public class WaterWaveAttack implements CsBossAttack {

    private static final int WAVE_INTERVAL = 38;  // 5*38 + 50 = 240 t
    private static final int WAVE_LIFESPAN = 50;
    private static final int WAVES = 6;           // deterministic
    private static final double SPEED = 0.375;     // 25 % slower
    private static final int CROSS_DIRECTIONS = 4; // X shape: 4 trios, 90° apart
    private static final double SIDE_ANGLE_DEG = 25.0;
    private static final double HALF_WIDTH = 1.5;  // half wall width (3 blocks)
    private static final double SPAWN_Y_OFFSET = 1.0;
    private static final double STEP_DEG = 10.0;   // each volley rotates 10° from the previous (wave)

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private int waves;
    private int tick;
    private int wavesSpawned;
    private double baseAngle;  // random heading of the first volley
    private double stepRad;    // ±10° per volley (random clockwise / anti-clockwise)
    private boolean done;

    public WaterWaveAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.wavesSpawned = 0;
        this.done = false;
        this.waves = WAVES;
        // Première volée dans une direction aléatoire, puis rotation de ±10° à chaque volée (vague).
        this.baseAngle = rng.nextDouble() * Math.PI * 2.0;
        this.stepRad = Math.toRadians(STEP_DEG) * (rng.nextBoolean() ? 1.0 : -1.0);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (wavesSpawned < waves && tick == wavesSpawned * WAVE_INTERVAL) {
            sendCross(level, session, boss);
            wavesSpawned++;
        }
        if (tick >= (waves - 1) * WAVE_INTERVAL + WAVE_LIFESPAN) {
            done = true;
        }
        tick++;
    }

    private void sendCross(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        // X shape: the same trio fired in 4 directions 90° apart. The base angle rotates ±10°
        // per volley from the initial random heading, sweeping the X like a wave.
        double base = baseAngle + stepRad * wavesSpawned;
        for (int k = 0; k < CROSS_DIRECTIONS; k++) {
            sendTrio(level, session, boss, base + k * (Math.PI / 2.0));
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), session.getTriggerPos().getY() + SPAWN_Y_OFFSET, boss.getZ(),
                "cobblemon:move.waterpulse.actor", SoundSource.HOSTILE, 1.4F, 0.9F);
    }

    private void sendTrio(ServerLevel level, BossBattleSession session, CsBossEntity boss, double theta) {
        // Heading (F) and right perpendicular (P) in the horizontal plane.
        Vec3 f = new Vec3(Math.cos(theta), 0, Math.sin(theta));
        Vec3 p = new Vec3(Math.sin(theta), 0, -Math.cos(theta));
        double cos = Math.cos(Math.toRadians(SIDE_ANGLE_DEG));
        double sin = Math.sin(Math.toRadians(SIDE_ANGLE_DEG));
        double offLat = HALF_WIDTH + HALF_WIDTH * cos; // inner side edge flush with center edge
        double offFwd = HALF_WIDTH * sin;

        Vec3 origin = new Vec3(boss.getX(), session.getTriggerPos().getY() + SPAWN_Y_OFFSET, boss.getZ());
        Vec3 velocity = f.scale(SPEED);

        // Center: facing F, advanced 1 block + 5/16 forward relative to side waves.
        spawnWave(level, session, origin.add(f.scale(1.0 + 5.0 / 16.0)), velocity, f);
        // Right: offset +offLat*P (+offFwd*F), tilted +25° toward P.
        Vec3 rightDir = f.scale(cos).add(p.scale(sin));
        Vec3 rightPos = origin.add(p.scale(offLat)).add(f.scale(offFwd));
        spawnWave(level, session, rightPos, velocity, rightDir);
        // Left: symmetric.
        Vec3 leftDir = f.scale(cos).subtract(p.scale(sin));
        Vec3 leftPos = origin.subtract(p.scale(offLat)).add(f.scale(offFwd));
        spawnWave(level, session, leftPos, velocity, leftDir);
    }

    private void spawnWave(ServerLevel level, BossBattleSession session, Vec3 pos, Vec3 velocity, Vec3 facing) {
        float yaw = (float) Math.toDegrees(Math.atan2(facing.x, facing.z));
        AttackWaveEntity wave = AttackWaveEntity.spawn(level, pos.x, pos.y, pos.z, session.getId(), velocity, yaw);
        session.trackAttackEntity(wave);
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.SPREAD;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
