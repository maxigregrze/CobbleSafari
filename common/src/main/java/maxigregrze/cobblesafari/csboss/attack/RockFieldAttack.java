package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackMeteoriteEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.underground.logic.PerlinNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@code base_rock_4} (Type B): selects ~30% of the surface via Perlin noise (same
 * method as {@code base_electric_2}) and drops meteors there (like {@code base_rock_2}). Two waves
 * (at 0 s and 6 s) over ~12 s, each fired simultaneously.
 */
public class RockFieldAttack implements CsBossAttack {

    private static final int WAVES = 2;
    private static final int WAVE_INTERVAL = 120; // 2 waves at 0 s and 6 s
    private static final int METEOR_AT = 10;
    private static final int FALL_TICKS = 20;
    private static final int IMPACT_AT = METEOR_AT + FALL_TICKS; // 30
    private static final int END_DELAY = 90; // 120 + 30 + 90 = 240
    private static final double FALL_HEIGHT = 20.0;
    private static final float METEOR_DAMAGE = 18.0F;
    private static final double PERLIN_SCALE = 0.2;
    // Perlin (normalized) peaks around ~0.85; lower threshold ⇒ more coverage (~30 %).
    private static final double COVERAGE_THRESHOLD = 0.6;

    private final String id;
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

    public RockFieldAttack(String id) {
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
        PerlinNoise noise = new PerlinNoise(level.getRandom().nextLong());
        for (BlockPos surface : surfaces) {
            double n = noise.noise2dNormalized(surface.getX() * PERLIN_SCALE, surface.getZ() * PERLIN_SCALE);
            if (n > COVERAGE_THRESHOLD) {
                AttackShadowEntity shadow = AttackShadowEntity.spawn(level,
                        surface.getX() + 0.5, surface.getY() + 1.0, surface.getZ() + 0.5, session.getId());
                session.trackAttackEntity(shadow);
                rocks.add(new Rock(shadow, tick));
            }
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.rockthrow.target", net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 1.0F);
    }

    private void driveRock(ServerLevel level, BossBattleSession session, Rock r, int age) {
        if (age < METEOR_AT) {
            CsBossAttackLib.meteorTelegraph(level, r.shadow.getX(), r.shadow.getY(), r.shadow.getZ(),
                    CsBossAttackLib.METEOR_DUST);
        } else if (age == METEOR_AT) {
            AttackMeteoriteEntity meteor = AttackMeteoriteEntity.spawn(level,
                    r.shadow.getX(), r.shadow.getY() + FALL_HEIGHT, r.shadow.getZ(), session.getId(), false);
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
                    level.setBlockAndUpdate(pos, ModBlocks.METEORITE.defaultBlockState());
                }
            }
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
