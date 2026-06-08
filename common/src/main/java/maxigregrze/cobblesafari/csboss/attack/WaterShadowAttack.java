package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * {@code base_water_1} (plan 110): aquatic variant of {@code base_fire_1}. A shadow <b>per
 * player</b> that tracks then freezes; 1 s later a bubble column erupts (8 damage,
 * <b>no fire</b>). The next wave appears as soon as the previous one <b>stops moving</b> (on freeze),
 * so cycles overlap. +20% occurrences vs fire.
 */
public class WaterShadowAttack implements CsBossAttack {

    private static final int FREEZE_AT = 60;      // shadow freezes (and triggers next wave)
    private static final int COLUMN_START = 80;   // +1 s after freeze
    private static final int COLUMN_END = 110;    // column 1.5 s
    private static final int DISCARD_AT = 110;
    private static final int SPAWN_INTERVAL = FREEZE_AT; // wave as soon as previous freezes
    private static final int END_DELAY = 40;
    private static final int NOMINAL_WAVES = 5;   // +20% vs fire (4); ±25% ⇒ ~4–6
    private static final double COLUMN_RADIUS = 1.0;
    private static final double COLUMN_HEIGHT = 4.0;
    private static final float WATER_DAMAGE = 8.0F;

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private final List<Shadow> shadows = new ArrayList<>();
    private int waves;
    private int tick;
    private int wavesSpawned;
    private boolean done;

    private static final class Shadow {
        final AttackShadowEntity entity;
        final UUID target;
        final int birthTick;

        Shadow(AttackShadowEntity entity, UUID target, int birthTick) {
            this.entity = entity;
            this.target = target;
            this.birthTick = birthTick;
        }
    }

    public WaterShadowAttack(String id) {
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
        this.shadows.clear();
        this.waves = CsBossAttackLib.varyOccurrences(NOMINAL_WAVES, rng);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (wavesSpawned < waves && tick == wavesSpawned * SPAWN_INTERVAL) {
            for (ServerPlayer p : session.aliveParticipants(level)) {
                AttackShadowEntity e = AttackShadowEntity.spawn(level, p.getX(), p.getY(), p.getZ(), session.getId());
                session.trackAttackEntity(e);
                shadows.add(new Shadow(e, p.getUUID(), tick));
                CsBossAttackLib.sound(level, p.getX(), p.getY(), p.getZ(),
                        "minecraft:entity.generic.splash", SoundSource.HOSTILE, 0.8F, 1.2F);
            }
            wavesSpawned++;
        }

        Iterator<Shadow> it = shadows.iterator();
        while (it.hasNext()) {
            Shadow s = it.next();
            if (!s.entity.isAlive()) {
                it.remove();
                continue;
            }
            int age = tick - s.birthTick;
            driveShadow(level, session, boss, s, age);
            if (age >= DISCARD_AT) {
                it.remove();
            }
        }

        if (wavesSpawned >= waves && shadows.isEmpty()
                && tick >= (waves - 1) * SPAWN_INTERVAL + DISCARD_AT + END_DELAY) {
            done = true;
        }
        tick++;
    }

    private void driveShadow(ServerLevel level, BossBattleSession session, CsBossEntity boss, Shadow s, int age) {
        // Small rising bubbles from the shadow before the column (telegraph).
        if (age < COLUMN_START) {
            CsBossAttackLib.risingTelegraph(level, s.entity.getX(), s.entity.getY(), s.entity.getZ(),
                    net.minecraft.core.particles.ParticleTypes.BUBBLE_COLUMN_UP);
        }
        if (age < FREEZE_AT) {
            if (level.getPlayerByUUID(s.target) instanceof ServerPlayer p && p.isAlive()) {
                CsBossAttackLib.chase(s.entity, p.getX(), p.getY(), p.getZ(), CsBossAttackLib.CHASE_SPEED);
            }
        } else if (age == FREEZE_AT) {
            boss.triggerAttackAnimation();
        } else if (age >= COLUMN_START && age < COLUMN_END) {
            double x = s.entity.getX();
            double y = s.entity.getY();
            double z = s.entity.getZ();
            if (age == COLUMN_START) {
                CsBossAttackLib.sound(level, x, y, z,
                        "cobblemon:move.waterpulse.actor", SoundSource.HOSTILE, 1.3F, 1.0F);
            }
            CsBossAttackLib.bubbleColumn(level, x, y, z, COLUMN_HEIGHT);
            if (age % 5 == 0) {
                CsBossAttackLib.damagePlayersInColumn(level, session, x, y, z,
                        COLUMN_RADIUS, COLUMN_HEIGHT, CsBossDamage.bullet(level), WATER_DAMAGE, 0);
            }
        } else if (age == DISCARD_AT) {
            s.entity.discard();
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
