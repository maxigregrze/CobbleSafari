package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackMeteoriteEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@code base_dragon_3} (Type B / AREA): covers ~10% of the arena surface — chosen at <b>random</b>
 * (no Perlin noise) — with shadows that each drop a falling draco meteorite. Repeated 4 times over
 * roughly 10 s. Each impact places a draco-meteorite block.
 */
public class DracoMeteorFieldAttack implements CsBossAttack {

    private static final int WAVES = 2;
    private static final int WAVE_INTERVAL = 50;   // 2 waves
    private static final int METEOR_AT = 10;
    private static final int FALL_TICKS = 20;
    private static final int IMPACT_AT = METEOR_AT + FALL_TICKS; // 30
    private static final int END_DELAY = 30;
    private static final double FALL_HEIGHT = 20.0;
    private static final float METEOR_DAMAGE = 18.0F;
    private static final double COVERAGE = 0.10;   // ~10 % of the surface, picked at random

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private final List<Rock> rocks = new ArrayList<>();
    private int tick;
    private int wavesSpawned;
    private boolean done;

    private static final class Rock {
        final AttackShadowEntity shadow;
        final int birthTick;
        AttackMeteoriteEntity meteor;
        boolean damaged;

        Rock(AttackShadowEntity shadow, int birthTick) {
            this.shadow = shadow;
            this.birthTick = birthTick;
        }
    }

    public DracoMeteorFieldAttack(String id) {
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
        this.wavesSpawned = 0;
        this.done = false;
        this.rocks.clear();
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (wavesSpawned < WAVES && tick == wavesSpawned * WAVE_INTERVAL) {
            spawnWave(level, session, boss);
            wavesSpawned++;
        }

        Iterator<Rock> it = rocks.iterator();
        while (it.hasNext()) {
            Rock r = it.next();
            if (!r.shadow.isAlive()) {
                it.remove();
                continue;
            }
            int age = tick - r.birthTick;
            driveRock(level, session, r, age);
            if (age >= IMPACT_AT) {
                it.remove();
            }
        }

        if (wavesSpawned >= WAVES && rocks.isEmpty()
                && tick >= (WAVES - 1) * WAVE_INTERVAL + IMPACT_AT + END_DELAY) {
            done = true;
        }
        tick++;
    }

    private void spawnWave(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        List<BlockPos> surfaces = CsBossSurfaceScanner.scanSurface(level, session);
        for (BlockPos surface : surfaces) {
            if (rng.nextDouble() >= COVERAGE) {
                continue;
            }
            AttackShadowEntity shadow = AttackShadowEntity.spawn(level,
                    surface.getX() + 0.5, surface.getY() + 1.0, surface.getZ() + 0.5, session.getId());
            session.trackAttackEntity(shadow);
            rocks.add(new Rock(shadow, tick));
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.dragonclaw.target", net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 0.8F);
    }

    private void driveRock(ServerLevel level, BossBattleSession session, Rock r, int age) {
        if (age < METEOR_AT) {
            CsBossAttackLib.meteorTelegraph(level, r.shadow.getX(), r.shadow.getY(), r.shadow.getZ(),
                    CsBossAttackLib.DRACO_DUST);
        } else if (age == METEOR_AT) {
            AttackMeteoriteEntity meteor = AttackMeteoriteEntity.spawn(level,
                    r.shadow.getX(), r.shadow.getY() + FALL_HEIGHT, r.shadow.getZ(), session.getId(), true);
            session.trackAttackEntity(meteor);
            r.meteor = meteor;
        } else if (age > METEOR_AT && age <= IMPACT_AT) {
            if (r.meteor != null && r.meteor.isAlive()) {
                double prevProgress = Math.min(1.0, (age - 1 - METEOR_AT) / (double) FALL_TICKS);
                double progress = Math.min(1.0, (age - METEOR_AT) / (double) FALL_TICKS);
                double prevY = r.shadow.getY() + FALL_HEIGHT * (1.0 - prevProgress);
                double newY = r.shadow.getY() + FALL_HEIGHT * (1.0 - progress);
                r.meteor.setPos(r.shadow.getX(), newY, r.shadow.getZ());
                if (!r.damaged && CsBossAttackLib.meteorSweepHit(level, session, r.meteor, prevY, newY, METEOR_DAMAGE)) {
                    r.damaged = true;
                }
            }
            if (age >= IMPACT_AT) {
                BlockPos pos = BlockPos.containing(r.shadow.getX(), r.shadow.getY(), r.shadow.getZ());
                if (r.meteor != null && r.meteor.isAlive()) {
                    r.meteor.discard();
                }
                r.shadow.discard();
                if (level.getBlockState(pos).canBeReplaced()) {
                    level.setBlockAndUpdate(pos, ModBlocks.DRACO_METEORITE.defaultBlockState());
                }
            }
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
