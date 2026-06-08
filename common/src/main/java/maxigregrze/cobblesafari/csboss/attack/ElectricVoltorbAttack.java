package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code base_electric_1} (plan 107 § 6.1, revised plan 109): Voltorb Electrodes spawn
 * <b>one per player every 3 s</b> (3–5 waves), chase the nearest player at reduced speed,
 * flash (TNT fuse played <b>once only</b> at flash start), then
 * explode (player damage, no blocks). Time before detonation reduced by 30%. The attack
 * ends 2 s after the last detonation.
 */
public class ElectricVoltorbAttack implements CsBossAttack {

    private static final String VOLTORB = "voltorb";
    private static final double SPAWN_DISTANCE = 3.0;
    private static final double CHASE_SPEED = CsBossAttackLib.CHASE_SPEED * 0.5;
    private static final int WAVE_INTERVAL = 60;   // one wave every 3 s
    private static final int NOMINAL_WAVES = 4;    // ±25% ⇒ 3–5
    // Single Electrode cycle (from spawn), −30% vs 160 t: 120→84 (pursuit), 40→28 (flash).
    private static final int CHASE_TICKS = 84;
    private static final int FLASH_TICKS = 28;
    private static final int DETONATE_AGE = CHASE_TICKS + FLASH_TICKS; // 112
    private static final int FLASH_SEGMENT = 7;
    private static final int END_DELAY = 40;       // 2 s after last detonation
    private static final double EXPLOSION_RADIUS = 3.0;
    private static final float EXPLOSION_DAMAGE = 12.0F;

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private final List<Electrode> electrodes = new ArrayList<>();
    private int waves;
    private int tick;
    private int wavesSpawned;
    private boolean done;

    private static final class Electrode {
        final CsBossMinionEntity minion;
        final int spawnTick;
        boolean detonated;

        Electrode(CsBossMinionEntity minion, int spawnTick) {
            this.minion = minion;
            this.spawnTick = spawnTick;
        }
    }

    public ElectricVoltorbAttack(String id) {
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
        this.electrodes.clear();
        this.waves = CsBossAttackLib.varyOccurrences(NOMINAL_WAVES, rng);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }

        if (wavesSpawned < waves && tick == wavesSpawned * WAVE_INTERVAL) {
            spawnWave(level, session, boss);
            wavesSpawned++;
        }

        for (Electrode e : electrodes) {
            if (e.detonated || !e.minion.isAlive()) {
                continue;
            }
            driveElectrode(level, session, boss, e);
        }

        int lastWaveTick = (waves - 1) * WAVE_INTERVAL;
        if (tick >= lastWaveTick + DETONATE_AGE + END_DELAY) {
            done = true;
        }
        tick++;
    }

    private void spawnWave(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        double y = session.getTriggerPos().getY();
        List<ServerPlayer> alive = session.aliveParticipants(level);
        for (int i = 0; i < alive.size(); i++) {
            double angle = Math.toRadians(i * (360.0 / Math.max(1, alive.size())));
            Vec3 pos = new Vec3(boss.getX() + Math.cos(angle) * SPAWN_DISTANCE, y,
                    boss.getZ() + Math.sin(angle) * SPAWN_DISTANCE);
            CsBossMinionEntity m = session.spawnFixedMinion(level, pos, VOLTORB, 1);
            electrodes.add(new Electrode(m, tick));
        }
        if (!alive.isEmpty()) {
            CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                    "cobblemon:pokemon.voltorb.cry", SoundSource.HOSTILE, 1.4F, 1.0F);
        }
    }

    private void driveElectrode(ServerLevel level, BossBattleSession session, CsBossEntity boss, Electrode e) {
        int age = tick - e.spawnTick;
        if (age < CHASE_TICKS) {
            ServerPlayer target = CsBossAttackLib.nearestAlive(session, level, e.minion.position());
            if (target != null) {
                CsBossAttackLib.chase(e.minion, target.getX(), target.getY(), target.getZ(), CHASE_SPEED);
                e.minion.faceTarget(target.position());
            }
        } else if (age < DETONATE_AGE) {
            int into = age - CHASE_TICKS;
            if (into == 0) {
                // TNT fuse once only, at flash start.
                level.playSound(null, e.minion.getX(), e.minion.getY(), e.minion.getZ(),
                        net.minecraft.sounds.SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.2F);
            }
            boolean flashOn = (into / FLASH_SEGMENT) % 2 == 0;
            e.minion.setFlashing(flashOn);
            if (flashOn && into % FLASH_SEGMENT == 0) {
                CsBossAttackLib.whiteFlash(level, e.minion.getX(), e.minion.getY() + 0.5, e.minion.getZ());
            }
        } else {
            // Detonation.
            boss.triggerAttackAnimation();
            Vec3 center = new Vec3(e.minion.getX(), e.minion.getY() + 0.5, e.minion.getZ());
            CsBossAttackLib.nonBlockExplosion(level, session, center, EXPLOSION_RADIUS, EXPLOSION_DAMAGE);
            CsBossAttackLib.sound(level, center.x, center.y, center.z,
                    "cobblemon:impact.electric", SoundSource.HOSTILE, 1.2F, 1.0F);
            e.minion.discard();
            e.detonated = true;
        }
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.TARGETED;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
