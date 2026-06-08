package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@code base_ghost_2} (Type C / SPREAD): 5 times in a row (within ~10 s) the boss fires a phantom
 * volume — a 5×5×1 cloud of purple particles travelling in a random direction. Inside it ride 5
 * minions facing the travel direction: one in the centre and four (up/down/left/right) that spin
 * around it on the forward axis. Contact deals damage.
 */
public class GhostCrossAttack implements CsBossAttack {

    private static final int VOLLEYS = 10;
    private static final int INTERVAL = 20;        // one volley per second over 10 s
    private static final int FORMATION_LIFE = 60;  // 3 s of travel each
    private static final double SPEED = 0.35;
    private static final double RADIUS = 2.0;      // satellite distance ⇒ ~5-block span
    private static final double ROT_DEG_PER_TICK = 8.0;
    private static final double CENTER_HEIGHT = 2.5; // above the trigger floor
    private static final double MINION_HEIGHT = 1.2;
    private static final double HALF_THICK = 0.5;  // 1 block thin along the travel axis
    private static final double HIT_RADIUS = 2.5;
    private static final float DAMAGE = 8.0F;
    private static final int PARTICLES_PER_TICK = 14;
    /** Purple. */
    private static final DustParticleOptions PURPLE_DUST =
            new DustParticleOptions(new Vector3f(0.60f, 0.18f, 0.82f), 1.3f);

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private final List<Formation> formations = new ArrayList<>();
    private int tick;
    private int sent;
    private boolean done;

    private static final class Formation {
        final double dirX;
        final double dirZ;
        final double spawnX;
        final double spawnZ;
        final double baseY;
        final int birthTick;
        final CsBossMinionEntity[] minions;
        final Set<UUID> hit = new HashSet<>();

        Formation(double dirX, double dirZ, double spawnX, double spawnZ, double baseY,
                  int birthTick, CsBossMinionEntity[] minions) {
            this.dirX = dirX;
            this.dirZ = dirZ;
            this.spawnX = spawnX;
            this.spawnZ = spawnZ;
            this.baseY = baseY;
            this.birthTick = birthTick;
            this.minions = minions;
        }
    }

    public GhostCrossAttack(String id) {
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
        this.sent = 0;
        this.done = false;
        this.formations.clear();
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (sent < VOLLEYS && tick == sent * INTERVAL) {
            spawnFormation(level, session, boss);
            sent++;
        }

        Iterator<Formation> it = formations.iterator();
        while (it.hasNext()) {
            Formation f = it.next();
            int age = tick - f.birthTick;
            if (age >= FORMATION_LIFE) {
                discard(f);
                it.remove();
                continue;
            }
            driveFormation(level, session, f, age);
        }

        if (sent >= VOLLEYS && formations.isEmpty()) {
            done = true;
        }
        tick++;
    }

    private void spawnFormation(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        double theta = rng.nextDouble() * Math.PI * 2.0;
        double dirX = Math.cos(theta);
        double dirZ = Math.sin(theta);
        double baseY = session.getTriggerPos().getY() + CENTER_HEIGHT;
        Vec3 spawn = new Vec3(boss.getX(), baseY, boss.getZ());
        CsBossMinionEntity[] minions = new CsBossMinionEntity[5];
        for (int i = 0; i < minions.length; i++) {
            CsBossMinionEntity m = session.spawnMinion(level, spawn);
            m.resizeToHeight(MINION_HEIGHT);
            m.faceTargetInstant(new Vec3(spawn.x + dirX, spawn.y, spawn.z + dirZ));
            minions[i] = m;
        }
        formations.add(new Formation(dirX, dirZ, boss.getX(), boss.getZ(), baseY, tick, minions));
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.shadowball.actor", SoundSource.HOSTILE, 1.3F, 0.7F);
    }

    private void driveFormation(ServerLevel level, BossBattleSession session, Formation f, int age) {
        double cx = f.spawnX + f.dirX * SPEED * age;
        double cz = f.spawnZ + f.dirZ * SPEED * age;
        double cy = f.baseY;
        // Forward axis and the two axes of the perpendicular plane (up = world Y, right = horizontal).
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = new Vec3(f.dirZ, 0, -f.dirX);
        Vec3 face = new Vec3(f.dirX, 0, f.dirZ);

        // Centre minion.
        placeMinion(f.minions[0], cx, cy, cz, face);
        // 4 satellites spinning around the forward axis.
        double rot = Math.toRadians(age * ROT_DEG_PER_TICK);
        for (int k = 0; k < 4; k++) {
            double phi = rot + k * (Math.PI / 2.0);
            double ox = (up.x * Math.cos(phi) + right.x * Math.sin(phi)) * RADIUS;
            double oy = (up.y * Math.cos(phi) + right.y * Math.sin(phi)) * RADIUS;
            double oz = (up.z * Math.cos(phi) + right.z * Math.sin(phi)) * RADIUS;
            placeMinion(f.minions[k + 1], cx + ox, cy + oy, cz + oz, face);
        }

        spawnParticles(level, cx, cy, cz, up, right, face);
        damage(level, session, f, cx, cy, cz);
    }

    private void placeMinion(CsBossMinionEntity m, double x, double y, double z, Vec3 face) {
        if (m == null || !m.isAlive()) {
            return;
        }
        m.setPos(x, y - MINION_HEIGHT * 0.5, z);
        m.faceTarget(new Vec3(x + face.x, y, z + face.z));
    }

    private void spawnParticles(ServerLevel level, double cx, double cy, double cz,
                                Vec3 up, Vec3 right, Vec3 face) {
        for (int n = 0; n < PARTICLES_PER_TICK; n++) {
            double u = (rng.nextDouble() * 2.0 - 1.0) * RADIUS;   // vertical spread
            double v = (rng.nextDouble() * 2.0 - 1.0) * RADIUS;   // lateral spread
            double w = (rng.nextDouble() * 2.0 - 1.0) * HALF_THICK; // thin along travel
            double x = cx + up.x * u + right.x * v + face.x * w;
            double y = cy + up.y * u + right.y * v + face.y * w;
            double z = cz + up.z * u + right.z * v + face.z * w;
            level.sendParticles(PURPLE_DUST, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void damage(ServerLevel level, BossBattleSession session, Formation f,
                        double cx, double cy, double cz) {
        double r2 = HIT_RADIUS * HIT_RADIUS;
        for (ServerPlayer p : session.aliveParticipants(level)) {
            if (f.hit.contains(p.getUUID())) {
                continue;
            }
            double dx = p.getX() - cx;
            double dy = (p.getY() + p.getBbHeight() * 0.5) - cy;
            double dz = p.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz <= r2 && p.hurt(CsBossDamage.bullet(level), DAMAGE)) {
                f.hit.add(p.getUUID());
            }
        }
    }

    private void discard(Formation f) {
        for (CsBossMinionEntity m : f.minions) {
            if (m != null && m.isAlive()) {
                m.discard();
            }
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
