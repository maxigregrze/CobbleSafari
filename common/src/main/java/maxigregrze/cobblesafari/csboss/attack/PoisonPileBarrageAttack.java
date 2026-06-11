package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackPileProjectileEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.PileKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@code base_poison_2} (plan 126 § 3) : ombre figée aux pieds de chaque joueur, puis cube sludge
 * en cloche depuis le boss ; impact = tas éphémère 1×1.
 */
public class PoisonPileBarrageAttack implements CsBossAttack {

    private static final int WAVES = 8;
    private static final int WAVE_INTERVAL = 30;
    private static final int FIRE_DELAY = 16;
    private static final int FLIGHT_TICKS = 30;
    private static final double ARC_HEIGHT = 6.0;
    private static final float PROJECTILE_DAMAGE = 8.0F;
    private static final int DURATION = 240;

    private final String id;
    private final List<Wave> waves = new ArrayList<>();
    private int tick;
    private int wavesSpawned;
    private boolean done;

    private static final class Wave {
        final List<AttackShadowEntity> shadows = new ArrayList<>();
        final List<Vec3> targets = new ArrayList<>();
        final int birthTick;
        boolean fired;

        Wave(int birthTick) {
            this.birthTick = birthTick;
        }
    }

    public PoisonPileBarrageAttack(String id) {
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

        Iterator<Wave> it = waves.iterator();
        while (it.hasNext()) {
            Wave w = it.next();
            int age = tick - w.birthTick;
            if (age >= FIRE_DELAY + FLIGHT_TICKS + 20) {
                for (AttackShadowEntity shadow : w.shadows) {
                    if (shadow.isAlive()) {
                        shadow.discard();
                    }
                }
                it.remove();
                continue;
            }
            if (!w.fired && age == FIRE_DELAY) {
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
        Wave w = new Wave(tick);
        double floorY = session.getTriggerPos().getY();
        for (ServerPlayer p : session.aliveParticipants(level)) {
            Vec3 target = new Vec3(p.getX(), floorY, p.getZ());
            AttackShadowEntity shadow = AttackShadowEntity.spawn(level,
                    target.x, target.y, target.z, session.getId(), false);
            session.trackAttackEntity(shadow);
            w.shadows.add(shadow);
            w.targets.add(target);
        }
        waves.add(w);
    }

    private void fireWave(ServerLevel level, BossBattleSession session, CsBossEntity boss, Wave w) {
        boss.triggerAttackAnimation();
        Vec3 origin = new Vec3(boss.getX(), session.getTriggerPos().getY() + 1.5, boss.getZ());
        for (int i = 0; i < w.targets.size(); i++) {
            Vec3 target = w.targets.get(i);
            AttackPileProjectileEntity projectile = AttackPileProjectileEntity.spawn(
                    level, origin, target, session.getId(),
                    PileKind.SLUDGE, FLIGHT_TICKS, ARC_HEIGHT, 1, PROJECTILE_DAMAGE);
            session.trackAttackEntity(projectile);
            if (i == 0) {
                CsBossAttackLib.sound(level, origin.x, origin.y, origin.z,
                        "cobblemon:move.sludgebomb.actor", SoundSource.HOSTILE, 1.2F, 1.0F);
            }
        }
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
