package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackMeteoriteEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Shared base for meteor showers (plan 107 § 6.3/6.4). In waves, shadows track a
 * player for 3 s, freeze for 1 s, then a meteor falls (20 blocks) and, on impact (0.5 s), places an
 * ephemeral block where the shadow was. {@code base_rock_1} and {@code base_dragon_1} differ only
 * in their parameters (cadence, count, variant, block, final cooldown).
 */
public class MeteorShowerAttack implements CsBossAttack {

    // Shadow lifecycle (offsets from spawn).
    private static final int FOLLOW_TICKS = 60;   // 3 s of pursuit
    private static final int FREEZE_TICKS = 20;   // 1 s immobile
    private static final int FALL_TICKS = 20;     // fall 2× slower (10→20), visible longer
    private static final int IMPACT_AT = 90;      // impact unchanged (block placement)
    private static final int SPAWN_AT = IMPACT_AT - FALL_TICKS; // 70: meteor appears earlier
    private static final double FALL_HEIGHT = 20.0;

    private static final float METEOR_DAMAGE = 18.0F; // damage from a meteor falling on a player

    private final String id;
    private final int spawnInterval;
    private final int nominalWaves;
    private final boolean perPlayer;
    private final boolean draco;
    private final Block block;
    private final int endDelay;

    private final RandomSource rng = RandomSource.create();
    private final List<Shadow> shadows = new ArrayList<>();
    private int waves;
    private int tick;
    private int wavesSpawned;
    private boolean done;

    private static final class Shadow {
        final AttackShadowEntity entity;
        final int birthTick;
        UUID target;
        AttackMeteoriteEntity meteor;
        boolean damaged;

        Shadow(AttackShadowEntity entity, int birthTick, UUID target) {
            this.entity = entity;
            this.birthTick = birthTick;
            this.target = target;
        }
    }

    public MeteorShowerAttack(String id, int spawnInterval, int nominalWaves, boolean perPlayer,
                              boolean draco, Block block, int endDelay) {
        this.id = id;
        this.spawnInterval = spawnInterval;
        this.nominalWaves = nominalWaves;
        this.perPlayer = perPlayer;
        this.draco = draco;
        this.block = block;
        this.endDelay = endDelay;
    }

    /** {@code base_rock_1}: 1 wave/s (~10 waves ±25%), 1 shadow guaranteed/player, meteorite block (10 s). */
    public static MeteorShowerAttack rock(String id) {
        return new MeteorShowerAttack(id, 20, 10, true, false, ModBlocks.METEORITE, 100);
    }

    /** {@code base_dragon_1}: like {@code rock} but ×1.5 longer (~15 waves ±25%), draco meteorite. */
    public static MeteorShowerAttack draco(String id) {
        return new MeteorShowerAttack(id, 20, 15, true, true, ModBlocks.DRACO_METEORITE, 100);
    }

    public String id() {
        return id;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.wavesSpawned = 0;
        this.done = false;
        this.shadows.clear();
        this.waves = CsBossAttackLib.varyOccurrences(nominalWaves, rng);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }

        // Wave spawning.
        if (wavesSpawned < waves && tick == wavesSpawned * spawnInterval) {
            spawnWave(level, session);
            wavesSpawned++;
        }

        // Drive each shadow's lifecycle.
        Iterator<Shadow> it = shadows.iterator();
        while (it.hasNext()) {
            Shadow s = it.next();
            if (!s.entity.isAlive()) {
                it.remove();
                continue;
            }
            int age = tick - s.birthTick;
            driveShadow(level, session, s, age);
            if (age >= IMPACT_AT) {
                it.remove();
            }
        }

        int lastSpawnTick = (waves - 1) * spawnInterval;
        if (wavesSpawned >= waves && shadows.isEmpty() && tick >= lastSpawnTick + endDelay) {
            done = true;
        }
        tick++;
    }

    private void spawnWave(ServerLevel level, BossBattleSession session) {
        List<ServerPlayer> alive = session.aliveParticipants(level);
        if (alive.isEmpty()) {
            return;
        }
        if (perPlayer) {
            for (ServerPlayer p : alive) {
                spawnShadow(level, session, p);
            }
        } else {
            int count = (alive.size() + 1) / 2; // ceil(players/2)
            for (int i = 0; i < count; i++) {
                spawnShadow(level, session, alive.get(rng.nextInt(alive.size())));
            }
        }
    }

    private void spawnShadow(ServerLevel level, BossBattleSession session, ServerPlayer target) {
        AttackShadowEntity e = AttackShadowEntity.spawn(level, target.getX(), target.getY(), target.getZ(),
                session.getId());
        session.trackAttackEntity(e);
        shadows.add(new Shadow(e, tick, target.getUUID()));
        // Audio cue of imminent attack (visible/audible as soon as the shadow appears).
        CsBossAttackLib.sound(level, target.getX(), target.getY(), target.getZ(),
                draco ? "cobblemon:move.dragonclaw.target" : "cobblemon:move.rockthrow.target",
                net.minecraft.sounds.SoundSource.HOSTILE, 0.8F, 1.0F);
    }

    private void driveShadow(ServerLevel level, BossBattleSession session, Shadow s, int age) {
        if (age < SPAWN_AT) {
            // Colored dust falling from the sky above the shadow: telegraph before the meteor.
            CsBossAttackLib.meteorTelegraph(level, s.entity.getX(), s.entity.getY(), s.entity.getZ(),
                    draco ? CsBossAttackLib.DRACO_DUST : CsBossAttackLib.METEOR_DUST);
        }
        if (age < FOLLOW_TICKS) {
            if (level.getPlayerByUUID(s.target) instanceof ServerPlayer p && p.isAlive()) {
                CsBossAttackLib.chase(s.entity, p.getX(), p.getY(), p.getZ(), CsBossAttackLib.CHASE_SPEED);
            }
        } else if (age == SPAWN_AT) {
            AttackMeteoriteEntity meteor = AttackMeteoriteEntity.spawn(level,
                    s.entity.getX(), s.entity.getY() + FALL_HEIGHT, s.entity.getZ(), session.getId(), draco);
            session.trackAttackEntity(meteor);
            s.meteor = meteor;
            CsBossAttackLib.sound(level, s.entity.getX(), s.entity.getY(), s.entity.getZ(),
                    draco ? "cobblemon:move.dragonclaw.actor" : "cobblemon:move.rockthrow.actor",
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.3F, draco ? 0.8F : 1.0F);
        } else if (age > SPAWN_AT && age <= IMPACT_AT) {
            if (s.meteor != null && s.meteor.isAlive()) {
                double prevProgress = Math.min(1.0, (age - 1 - SPAWN_AT) / (double) FALL_TICKS);
                double progress = Math.min(1.0, (age - SPAWN_AT) / (double) FALL_TICKS);
                double prevY = s.entity.getY() + FALL_HEIGHT * (1.0 - prevProgress);
                double newY = s.entity.getY() + FALL_HEIGHT * (1.0 - progress);
                s.meteor.setPos(s.entity.getX(), newY, s.entity.getZ());
                // Swept damage over the entire segment traveled this tick (to ground on last tick).
                if (!s.damaged && CsBossAttackLib.meteorSweepHit(level, session, s.meteor, prevY, newY, METEOR_DAMAGE)) {
                    s.damaged = true;
                }
            }
            if (age >= IMPACT_AT) {
                impact(level, s);
            }
        }
    }

    private void impact(ServerLevel level, Shadow s) {
        BlockPos pos = BlockPos.containing(s.entity.getX(), s.entity.getY(), s.entity.getZ());
        if (s.meteor != null && s.meteor.isAlive()) {
            s.meteor.discard();
        }
        s.entity.discard();
        if (level.getBlockState(pos).canBeReplaced()) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
        }
        CsBossAttackLib.sound(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                draco ? "cobblemon:impact.dragon" : "cobblemon:impact.rock",
                net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 1.0F);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                net.minecraft.sounds.SoundSource.HOSTILE, 0.8F, 0.8F);
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
