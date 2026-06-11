package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackPileProjectileEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.PileKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@code base_ground_3} (plan 126 § 4) : 10 points au sol par vague, ombre large, cube mud en cloche ;
 * impact = 3×3 de tas éphémères.
 */
public class MudPileFieldAttack implements CsBossAttack {

    private static final int WAVES = 2;
    private static final int WAVE_INTERVAL = 120;
    private static final int POINTS = 10;
    private static final double MIN_DIST = 5.0;
    private static final int TELEGRAPH = 40;
    private static final int FLIGHT_TICKS = 36;
    private static final double ARC_HEIGHT = 9.0;
    private static final float PROJECTILE_DAMAGE = 8.0F;
    private static final int DURATION = 240;

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private final List<FieldWave> waves = new ArrayList<>();
    private int tick;
    private int wavesSpawned;
    private boolean done;

    private static final class FieldWave {
        final List<AttackShadowEntity> shadows = new ArrayList<>();
        final List<Vec3> targets = new ArrayList<>();
        final int birthTick;
        boolean fired;

        FieldWave(int birthTick) {
            this.birthTick = birthTick;
        }
    }

    public MudPileFieldAttack(String id) {
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
        this.waves.clear();
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }

        if (wavesSpawned < WAVES && tick == wavesSpawned * WAVE_INTERVAL) {
            spawnWave(level, session);
            wavesSpawned++;
        }

        Iterator<FieldWave> it = waves.iterator();
        while (it.hasNext()) {
            FieldWave w = it.next();
            int age = tick - w.birthTick;
            if (age >= TELEGRAPH + FLIGHT_TICKS + 20) {
                for (AttackShadowEntity shadow : w.shadows) {
                    if (shadow.isAlive()) {
                        shadow.discard();
                    }
                }
                it.remove();
                continue;
            }
            if (!w.fired && age == TELEGRAPH) {
                fireWave(level, session, boss, w);
                w.fired = true;
            }
        }

        if (tick >= DURATION) {
            done = true;
        }
        tick++;
    }

    private void spawnWave(ServerLevel level, BossBattleSession session) {
        FieldWave w = new FieldWave(tick);
        double floorY = session.getTriggerPos().getY();
        Vec3 center = new Vec3(session.getArenaCenter().x, floorY, session.getArenaCenter().z);
        List<Vec3> points = CsBossAttackLib.scatterPoints(center, session.getPlayerRadius(),
                POINTS, MIN_DIST, rng);
        for (Vec3 point : points) {
            AttackShadowEntity shadow = AttackShadowEntity.spawn(level,
                    point.x, point.y, point.z, session.getId(), true);
            session.trackAttackEntity(shadow);
            w.shadows.add(shadow);
            w.targets.add(point);
        }
        waves.add(w);
    }

    private void fireWave(ServerLevel level, BossBattleSession session, CsBossEntity boss, FieldWave w) {
        boss.triggerAttackAnimation();
        Vec3 origin = new Vec3(boss.getX(), session.getTriggerPos().getY() + 1.5, boss.getZ());
        for (int i = 0; i < w.targets.size(); i++) {
            Vec3 target = w.targets.get(i);
            AttackPileProjectileEntity projectile = AttackPileProjectileEntity.spawn(
                    level, origin, target, session.getId(),
                    PileKind.MUD, FLIGHT_TICKS, ARC_HEIGHT, 3, PROJECTILE_DAMAGE);
            session.trackAttackEntity(projectile);
            if (i == 0) {
                CsBossAttackLib.sound(level, origin.x, origin.y, origin.z,
                        "cobblemon:move.earthquake.actor", SoundSource.HOSTILE, 1.3F, 1.0F);
            }
        }
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.AREA;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
