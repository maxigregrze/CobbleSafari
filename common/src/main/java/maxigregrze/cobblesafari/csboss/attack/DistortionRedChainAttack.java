package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackRedChainEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * {@code distortion_5} (Type C / SPREAD): for 5 s, red particles appear ~3 blocks around the boss
 * and slowly converge toward its center (implosion telegraph). Then the boss fires 6 red chains
 * at once (spaced every 60°), very slow (1 block/s), parallel to the ground like the Red Chain
 * item projectile; each participant crossed receives the <i>Red Shackled</i> effect.
 */
public class DistortionRedChainAttack implements CsBossAttack {

    private static final int PARTICLE_PHASE = 100; // 5 s particle implosion
    private static final int CHAIN_COUNT = 6; // 6 directions, 60° apart
    private static final double CHAIN_SPEED = 0.25; // 5 blocks/s (×5)
    private static final double CHAIN_Y_OFFSET = 0.3;

    private static final double RING_RADIUS = 3.0;
    private static final int RINGS = 3; // concurrent rings phase-shifted
    private static final int POINTS = 16; // points per ring
    private static final int CYCLE = 25; // ticks for a ring to shrink from 3 to 0 blocks
    private static final double CENTER_HEIGHT = 1.0;
    private static final DustParticleOptions RED_DUST =
            new DustParticleOptions(new Vector3f(0.85f, 0.05f, 0.10f), 3.0f); // large particles

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private int chainTravel;
    private int tick;
    private boolean chainsFired;
    private boolean done;

    public DistortionRedChainAttack(String id) {
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
        this.chainsFired = false;
        this.done = false;
        this.chainTravel = (int) Math.ceil(CsBossAttackLib.areaReach(session) / CHAIN_SPEED);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (tick < PARTICLE_PHASE) {
            emitImplosion(level, session, boss);
        } else if (!chainsFired) {
            fireChains(level, session, boss);
            chainsFired = true;
        }
        if (tick >= PARTICLE_PHASE + chainTravel) {
            done = true; // chains already fired finish on their own
        }
        tick++;
    }

    /** Red particle rings contracting toward the boss (implosion effect). */
    private void emitImplosion(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        double cx = boss.getX();
        double cy = session.getTriggerPos().getY() + CENTER_HEIGHT;
        double cz = boss.getZ();
        for (int r = 0; r < RINGS; r++) {
            double phase = ((tick + (double) r * CYCLE / RINGS) % CYCLE) / CYCLE;
            double radius = RING_RADIUS * (1.0 - phase);
            for (int i = 0; i < POINTS; i++) {
                double angle = (i / (double) POINTS) * Math.PI * 2.0 + rng.nextDouble() * 0.2;
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;
                double y = cy + (rng.nextDouble() * 2.0 - 1.0) * 0.8;
                level.sendParticles(RED_DUST, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private void fireChains(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        double base = rng.nextDouble() * Math.PI * 2.0;
        double y = session.getTriggerPos().getY() + CHAIN_Y_OFFSET;
        for (int k = 0; k < CHAIN_COUNT; k++) {
            double theta = base + k * (Math.PI * 2.0 / CHAIN_COUNT);
            double dirX = Math.cos(theta);
            double dirZ = Math.sin(theta);
            Vec3 velocity = new Vec3(dirX, 0, dirZ).scale(CHAIN_SPEED);
            float yaw = (float) Math.toDegrees(Math.atan2(dirX, dirZ));
            AttackRedChainEntity chain = AttackRedChainEntity.spawn(level,
                    boss.getX(), y, boss.getZ(), session.getId(), velocity, yaw);
            chain.setMaxTravel(CsBossAttackLib.areaReach(session));
            session.trackAttackEntity(chain);
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), y, boss.getZ(),
                "cobblemon:move.spiritshackle.actor", SoundSource.HOSTILE, 1.3F, 0.7F);
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
