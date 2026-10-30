package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * {@code base_fire_4} — Flamethrower (plan 199 § 11.4, SPREAD ; revu après retours). Un jet de flammes
 * à hauteur des pieds part du boss jusqu'au bord du carré d'arène (coins compris) et balaie 270°, deux
 * fois : le second balayage repart du côté opposé à la fin du premier, en sens inverse (on ne peut pas
 * rester collé derrière le jet). Sauter par-dessus ou rester derrière. 3 par contact (i-frames).
 */
public class FlamethrowerAttack implements CsBossAttack {

    private static final int SWEEP_TICKS = 120;
    private static final int TELEGRAPH = 20;
    private static final double SWEEP_DEG = 270.0;
    private static final double HALF_WIDTH = 0.9;
    private static final double PARTICLE_STEP = 0.7;
    private static final double JET_SPEED = 0.12;
    private static final float DAMAGE = 3.0F; // base 3
    private static final int DURATION = SWEEP_TICKS * 2; // 240

    private final String id;
    private Vec3 origin = Vec3.ZERO;
    private Vec3 center = Vec3.ZERO;
    private double half;
    private double startDeg;
    private int tick;
    private boolean done;

    public FlamethrowerAttack(String id) {
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
        this.origin = new Vec3(boss.getX(), session.getFloorY() + 0.5, boss.getZ());
        this.center = session.getArenaCenter();
        this.half = session.getPlayerRadius();
        this.startDeg = level.getRandom().nextDouble() * 360.0;
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        int sweep = tick / SWEEP_TICKS;
        int t = tick % SWEEP_TICKS;
        double sign = sweep == 0 ? 1.0 : -1.0;
        // Second balayage : côté opposé à l'endroit où le premier s'est arrêté.
        double sweepStart = sweep == 0 ? startDeg : startDeg + SWEEP_DEG + 180.0;
        if (t < TELEGRAPH) {
            if (t % 4 == 0) {
                Vec3 dir = dirOf(sweepStart);
                double len = CsBossAttackLib.distanceToSquareEdge(origin, dir, center, half);
                for (double d = 1.0; d <= len; d += 1.5) {
                    level.sendParticles(ParticleTypes.SMALL_FLAME, origin.x + dir.x * d, origin.y - 0.3,
                            origin.z + dir.z * d, 1, 0.05, 0.0, 0.05, 0.0);
                }
            }
        } else {
            if (t == TELEGRAPH) {
                boss.triggerAttackAnimation();
                CsBossAttackLib.sound(level, origin.x, origin.y, origin.z,
                        "cobblemon:move.flamethrower.actor", SoundSource.HOSTILE, 1.6F, 1.0F);
            } else if (t % 20 == 0) {
                CsBossAttackLib.sound(level, origin.x, origin.y, origin.z,
                        "minecraft:item.firecharge.use", SoundSource.HOSTILE, 1.0F, 0.8F);
            }
            double progress = (t - TELEGRAPH) / (double) (SWEEP_TICKS - TELEGRAPH);
            Vec3 dir = dirOf(sweepStart + sign * SWEEP_DEG * progress);
            double len = CsBossAttackLib.distanceToSquareEdge(origin, dir, center, half);
            jet(level, dir, len);
            damageJet(level, session, dir, len);
        }
        if (++tick >= DURATION) {
            done = true;
        }
    }

    /** Ligne de flammes poussées vers l'extérieur (vitesse de particule le long du jet). */
    private void jet(ServerLevel level, Vec3 dir, double len) {
        for (double d = 1.0; d <= len; d += PARTICLE_STEP) {
            double x = origin.x + dir.x * d;
            double z = origin.z + dir.z * d;
            level.sendParticles(ParticleTypes.FLAME, x, origin.y, z, 0, dir.x, 0.01, dir.z, JET_SPEED);
        }
        if (tick % 3 == 0) {
            double d = 2.0 + level.getRandom().nextDouble() * Math.max(0.0, len - 2.0);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, origin.x + dir.x * d, origin.y + 0.3,
                    origin.z + dir.z * d, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private static Vec3 dirOf(double deg) {
        double a = Math.toRadians(deg);
        return new Vec3(Math.cos(a), 0.0, Math.sin(a));
    }

    private void damageJet(ServerLevel level, BossBattleSession session, Vec3 dir, double len) {
        for (ServerPlayer p : session.aliveParticipants(level)) {
            double wx = p.getX() - origin.x;
            double wy = (p.getY() + 0.3) - origin.y;
            double wz = p.getZ() - origin.z;
            double t = wx * dir.x + wz * dir.z;
            if (t < 0.0 || t > len) {
                continue;
            }
            double dx = wx - dir.x * t;
            double dz = wz - dir.z * t;
            if (dx * dx + dz * dz <= HALF_WIDTH * HALF_WIDTH && Math.abs(wy) <= 0.8) {
                CsBossDamage.hurt(p, session, level.damageSources().inFire(), DAMAGE);
            }
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
