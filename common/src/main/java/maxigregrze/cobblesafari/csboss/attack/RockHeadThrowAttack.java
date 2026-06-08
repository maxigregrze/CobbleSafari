package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackMeteoriteEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * {@code base_rock_3} (plan 113, Type A): for each player, a meteor spawns near the middle of the
 * arena (slightly off-centered toward a world axis), grows (scale 0 → 1), spins on all 3 axes
 * (speed 0 → fast over 3 s), then is thrown in an arc toward the player; it places a meteorite block
 * on ground impact. Repeated 3–5 times.
 */
public class RockHeadThrowAttack implements CsBossAttack {

    private static final int GROW_TICKS = 10;
    private static final int SPIN_TICKS = 60;     // 3 s spin-up in place
    private static final int THROW_TICKS = 30;    // arc toward player
    private static final int IMPACT_AT = SPIN_TICKS + THROW_TICKS; // 90
    private static final int WAVE_INTERVAL = IMPACT_AT;
    private static final int NOMINAL_WAVES = 4;   // ±25% ⇒ 3–5
    private static final float MAX_SPIN = 25.0F;  // deg/tick at max
    private static final double SPAWN_HEIGHT = 3.0;  // above the trigger floor, over the arena center
    private static final double CENTER_OFFSET = 1.5; // slight off-center toward one world axis
    private static final double ARC = 2.5;
    private static final float METEOR_DAMAGE = 18.0F;

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private final List<Rock> rocks = new ArrayList<>();
    private int waves;
    private int tick;
    private int wavesSpawned;
    private boolean done;

    private static final class Rock {
        final AttackMeteoriteEntity meteor;
        final UUID target;
        final int birthTick;
        final double headX;
        final double headZ;
        float spin;
        double tx;
        double ty;
        double tz;
        double startX;
        double startY;
        double startZ;
        boolean thrown;
        boolean damaged;

        Rock(AttackMeteoriteEntity meteor, UUID target, int birthTick, double headX, double headZ) {
            this.meteor = meteor;
            this.target = target;
            this.birthTick = birthTick;
            this.headX = headX;
            this.headZ = headZ;
        }
    }

    public RockHeadThrowAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.TARGETED;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.wavesSpawned = 0;
        this.done = false;
        this.rocks.clear();
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

        Iterator<Rock> it = rocks.iterator();
        while (it.hasNext()) {
            Rock r = it.next();
            if (!r.meteor.isAlive()) {
                it.remove();
                continue;
            }
            int age = tick - r.birthTick;
            driveRock(level, session, r, age);
            if (age >= IMPACT_AT) {
                it.remove();
            }
        }

        if (wavesSpawned >= waves && rocks.isEmpty() && tick >= (waves - 1) * WAVE_INTERVAL + IMPACT_AT) {
            done = true;
        }
        tick++;
    }

    private void spawnWave(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        List<ServerPlayer> alive = session.aliveParticipants(level);
        net.minecraft.world.phys.Vec3 center = session.getArenaCenter();
        double headY = session.getTriggerPos().getY() + SPAWN_HEIGHT;
        // Slight off-center offsets toward the four world axes (+X, -X, +Z, -Z).
        double[][] axes = {{CENTER_OFFSET, 0}, {-CENTER_OFFSET, 0}, {0, CENTER_OFFSET}, {0, -CENTER_OFFSET}};
        for (int i = 0; i < alive.size(); i++) {
            double[] off = axes[rng.nextInt(axes.length)];
            double hx = center.x + off[0];
            double hz = center.z + off[1];
            AttackMeteoriteEntity meteor = AttackMeteoriteEntity.spawn(level, hx, headY, hz, session.getId(), false);
            meteor.setRenderScale(0.0F);
            session.trackAttackEntity(meteor);
            rocks.add(new Rock(meteor, alive.get(i).getUUID(), tick, hx, hz));
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.rockthrow.actor", net.minecraft.sounds.SoundSource.HOSTILE, 1.3F, 0.8F);
    }

    private void driveRock(ServerLevel level, BossBattleSession session, Rock r, int age) {
        double headY = session.getTriggerPos().getY() + SPAWN_HEIGHT;
        if (age < SPIN_TICKS) {
            // Growth + tumble in place above the arena center.
            float scale = Math.min(1.0F, age / (float) GROW_TICKS);
            float spinSpeed = MAX_SPIN * (age / (float) SPIN_TICKS);
            r.spin += spinSpeed;
            r.meteor.setRenderScale(scale);
            r.meteor.setSpin(r.spin);
            r.meteor.setPos(r.headX, headY, r.headZ);
        } else if (age == SPIN_TICKS) {
            // Capture target (player ground position) and start point.
            r.startX = r.meteor.getX();
            r.startY = r.meteor.getY();
            r.startZ = r.meteor.getZ();
            if (level.getPlayerByUUID(r.target) instanceof ServerPlayer p) {
                r.tx = p.getX();
                r.ty = p.getY();
                r.tz = p.getZ();
            } else {
                r.tx = r.startX;
                r.ty = session.getTriggerPos().getY();
                r.tz = r.startZ;
            }
            r.thrown = true;
        } else if (age < IMPACT_AT && r.thrown) {
            double progress = (age - SPIN_TICKS) / (double) THROW_TICKS;
            double x = r.startX + (r.tx - r.startX) * progress;
            double z = r.startZ + (r.tz - r.startZ) * progress;
            double y = r.startY + (r.ty - r.startY) * progress + ARC * 4.0 * progress * (1.0 - progress);
            r.spin += MAX_SPIN;
            r.meteor.setSpin(r.spin);
            double prevY = r.meteor.getY();
            r.meteor.setPos(x, y, z);
            if (!r.damaged && CsBossAttackLib.meteorSweepHit(level, session, r.meteor, prevY, y, METEOR_DAMAGE)) {
                r.damaged = true;
            }
        } else if (age >= IMPACT_AT) {
            BlockPos pos = BlockPos.containing(r.tx, r.ty, r.tz);
            r.meteor.discard();
            if (level.getBlockState(pos).canBeReplaced()) {
                level.setBlockAndUpdate(pos, ModBlocks.METEORITE.defaultBlockState());
            }
            CsBossAttackLib.sound(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    "cobblemon:impact.rock", net.minecraft.sounds.SoundSource.HOSTILE, 1.3F, 1.0F);
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
