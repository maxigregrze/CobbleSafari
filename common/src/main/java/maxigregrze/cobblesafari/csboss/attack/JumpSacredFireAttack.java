package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@code jump_3} — Feu Sacré (plan 199 § 13.3, AREA). Deux spirales d'Archimède de colonnes de feu
 * rouge et bleu partent du boss (la seconde tourne en sens inverse), avec 5 blocs entre deux spires.
 * Chaque colonne : alerte 1 s, éruption 0,5 s (5 + brûlure, une fois par joueur).
 */
public class JumpSacredFireAttack implements CsBossAttack {

    private static final int SPIRAL_A_START = 0;
    private static final int SPIRAL_B_START = 115;
    private static final int EMIT_TICKS = 95;
    private static final int COLUMNS_PER_TICK = 4; // spirale jusqu'aux coins du carré en ~95 ticks
    private static final double ARM_SPACING = 5.0;
    private static final double COLUMN_STEP = 2.0;
    private static final double START_RADIUS = 2.0;
    private static final int WARN_TICKS = 20;
    private static final int ERUPT_TICKS = 10;
    private static final double COLUMN_RADIUS = 1.0;
    private static final double COLUMN_HEIGHT = 3.0;
    private static final float DAMAGE = 5.0F; // base 5 + brûlure
    private static final int FIRE_TICKS = 60;
    private static final int DURATION = 240;

    private static final class Column {
        final double x;
        final double z;
        final int start;
        final boolean blue;
        final Set<UUID> hit = new HashSet<>();

        Column(double x, double z, int start, boolean blue) {
            this.x = x;
            this.z = z;
            this.start = start;
            this.blue = blue;
        }
    }

    /** Avancement d'une spirale : angle courant, sens de rotation, rayon atteint. */
    private static final class Spiral {
        final double startAngle;
        final double sign;
        double theta;

        Spiral(double startAngle, double sign) {
            this.startAngle = startAngle;
            this.sign = sign;
        }

        double radius() {
            return START_RADIUS + ARM_SPACING / (Math.PI * 2.0) * theta;
        }
    }

    private final String id;
    private final List<Column> columns = new ArrayList<>();
    private Spiral spiralA;
    private Spiral spiralB;
    private double cx;
    private double cz;
    private double floorY;
    private int emitted;
    private int tick;
    private boolean done;

    public JumpSacredFireAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.AREA;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.done = false;
        this.columns.clear();
        this.emitted = 0;
        this.cx = boss.getX();
        this.cz = boss.getZ();
        this.floorY = session.getFloorY();
        double start = level.getRandom().nextDouble() * Math.PI * 2.0;
        this.spiralA = new Spiral(start, 1.0);
        this.spiralB = new Spiral(start + Math.PI, -1.0);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        // La spirale monte jusqu'aux coins du carré ; les colonnes hors du carré sont sautées.
        double half = session.getPlayerRadius();
        double maxRadius = half * Math.sqrt(2.0);
        net.minecraft.world.phys.Vec3 arenaCenter = session.getArenaCenter();
        if (tick >= SPIRAL_A_START && tick < SPIRAL_A_START + EMIT_TICKS) {
            emit(spiralA, maxRadius, arenaCenter, half);
        }
        if (tick >= SPIRAL_B_START && tick < SPIRAL_B_START + EMIT_TICKS) {
            emit(spiralB, maxRadius, arenaCenter, half);
        }
        if (tick == SPIRAL_A_START || tick == SPIRAL_B_START) {
            boss.triggerAttackAnimation();
        }
        if (tick % 20 == 0) {
            CsBossAttackLib.sound(level, cx, floorY + 1.0, cz,
                    "cobblemon:move.fireblast.actor", SoundSource.HOSTILE, 1.2F, 1.3F);
        }
        Iterator<Column> it = columns.iterator();
        while (it.hasNext()) {
            Column c = it.next();
            int age = tick - c.start;
            if (age >= WARN_TICKS + ERUPT_TICKS) {
                it.remove();
                continue;
            }
            SimpleParticleType flame = c.blue ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
            if (age < WARN_TICKS) {
                if (age % 4 == 0) {
                    level.sendParticles(flame, c.x, floorY + 0.1, c.z, 1, 0.2, 0.0, 0.2, 0.0);
                }
            } else {
                if (age % 2 == 0) {
                    for (double dy = 0.0; dy < COLUMN_HEIGHT; dy += 1.0) {
                        level.sendParticles(flame, c.x, floorY + dy, c.z, 3, 0.3, 0.3, 0.3, 0.03);
                    }
                }
                CsBossAttackLib.damagePlayersInColumnOnce(level, session, c.x, floorY, c.z,
                        COLUMN_RADIUS, COLUMN_HEIGHT, level.damageSources().inFire(), DAMAGE, FIRE_TICKS, c.hit);
            }
        }
        if (++tick >= DURATION) {
            done = true;
        }
    }

    /** Pose {@code COLUMNS_PER_TICK} colonnes le long du bras, espacées de {@code COLUMN_STEP}. */
    private void emit(Spiral s, double maxRadius, net.minecraft.world.phys.Vec3 c, double half) {
        for (int i = 0; i < COLUMNS_PER_TICK; i++) {
            double r = s.radius();
            if (r > maxRadius) {
                return;
            }
            double angle = s.startAngle + s.sign * s.theta;
            double x = cx + Math.cos(angle) * r;
            double z = cz + Math.sin(angle) * r;
            if (Math.abs(x - c.x) <= half && Math.abs(z - c.z) <= half) {
                columns.add(new Column(x, z, tick, (emitted++ % 2) == 1));
            }
            double b = ARM_SPACING / (Math.PI * 2.0);
            s.theta += COLUMN_STEP / Math.sqrt(r * r + b * b);
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
