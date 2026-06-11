package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackBeamEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * {@code base_dragon_2} (Type C / SPREAD): from the boss (centered like {@code distortion_1}),
 * 8 beacon beams (#cc55ba) reach 30 blocks out in the N/NE/E/SE/… directions. They continuously
 * <b>swing up and down</b> ±45° (around the axis perpendicular to their facing — cardinals starting
 * at +45°, diagonals at −45°) <b>while rotating</b> around the world Y axis, for the same duration as
 * {@code distortion_1} (at a reduced speed). Beacon-texture beams (plus particles) show them; contact
 * deals damage.
 */
public class DragonBeamAttack implements CsBossAttack {

    private static final int BEAMS = 8;
    private static final int BEAM_LENGTH = 30;
    private static final double BEAM_HALF_WIDTH = 1.0;
    private static final float BEAM_DAMAGE = 8.0F;        // sweeping beams fill space (i-frame gated)
    private static final double SWING_DEG = 45.0;
    private static final int SWING_PERIOD = 100;          // ticks for a full up→down→up cycle
    private static final int DURATION = 240;              // ≈12 s
    private static final float SPIN_DEG_PER_TICK = 0.5F;  // reduced spin speed
    private static final double PARTICLE_STEP = 1.0;
    private static final int BEAM_COLOR = 0xFFCC55BA;
    /** #cc55ba. */
    private static final DustParticleOptions BEAM_DUST =
            new DustParticleOptions(new Vector3f(0.800f, 0.333f, 0.729f), 1.6f);

    private final String id;
    private final AttackBeamEntity[] beams = new AttackBeamEntity[BEAMS];
    private double originX;
    private double originY;
    private double originZ;
    private int tick;
    private boolean done;

    public DragonBeamAttack(String id) {
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
        this.done = false;
        // Centered on the boss (frozen during the attack), like distortion_1; 1 block over the arena floor.
        this.originX = boss.getX();
        this.originY = session.getTriggerPos().getY() + 1.0;
        this.originZ = boss.getZ();
        for (int i = 0; i < BEAMS; i++) {
            AttackBeamEntity beam = AttackBeamEntity.spawn(level, originX, originY, originZ,
                    session.getId(), BEAM_LENGTH, BEAM_COLOR);
            beam.setDirection(beamDir(i));
            session.trackAttackEntity(beam);
            beams[i] = beam;
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, originX, originY, originZ,
                "cobblemon:move.dragonpulse.actor", SoundSource.HOSTILE, 1.6F, 0.7F);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        for (int i = 0; i < BEAMS; i++) {
            Vec3 dir = beamDir(i);
            if (beams[i] != null && beams[i].isAlive()) {
                beams[i].setDirection(dir);
            }
            if (tick % 2 == 0) {
                drawBeam(level, dir);
            }
            damageBeam(level, session, dir);
        }

        if (tick % 20 == 0) {
            CsBossAttackLib.sound(level, originX, originY, originZ,
                    "minecraft:block.beacon.ambient", SoundSource.HOSTILE, 1.2F, 0.6F);
        }

        if (++tick >= DURATION) {
            for (AttackBeamEntity beam : beams) {
                if (beam != null && beam.isAlive()) {
                    beam.discard();
                }
            }
            done = true;
        }
    }

    /**
     * Current aim of beam {@code i}: a compass heading rotating around world Y, with a continuous
     * vertical (pitch) oscillation; cardinals and diagonals oscillate in opposite phase.
     */
    private Vec3 beamDir(int i) {
        double yawDeg = i * 45.0 + tick * SPIN_DEG_PER_TICK;
        double sign = (i % 2 == 0) ? 1.0 : -1.0; // cardinals start at +45°, diagonals at −45°
        double pitchDeg = sign * SWING_DEG * Math.cos(2.0 * Math.PI * tick / SWING_PERIOD);
        double a = Math.toRadians(yawDeg);
        double p = Math.toRadians(pitchDeg);
        return new Vec3(Math.sin(a) * Math.cos(p), Math.sin(p), -Math.cos(a) * Math.cos(p));
    }

    private void drawBeam(ServerLevel level, Vec3 dir) {
        for (double d = 1.0; d <= BEAM_LENGTH; d += PARTICLE_STEP) {
            level.sendParticles(BEAM_DUST, originX + dir.x * d, originY + dir.y * d, originZ + dir.z * d,
                    1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private void damageBeam(ServerLevel level, BossBattleSession session, Vec3 dir) {
        for (ServerPlayer p : session.aliveParticipants(level)) {
            double wx = p.getX() - originX;
            double wy = (p.getY() + p.getBbHeight() * 0.5) - originY;
            double wz = p.getZ() - originZ;
            double t = wx * dir.x + wy * dir.y + wz * dir.z; // projection along the beam
            if (t < 0.0 || t > BEAM_LENGTH) {
                continue;
            }
            double dx = wx - dir.x * t;
            double dy = wy - dir.y * t;
            double dz = wz - dir.z * t;
            if (dx * dx + dy * dy + dz * dz <= BEAM_HALF_WIDTH * BEAM_HALF_WIDTH) {
                p.hurt(CsBossDamage.bullet(level), BEAM_DAMAGE);
            }
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
