package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@code base_fire_1} (plan 107 § 6.2, revised plan 109): a flat shadow <b>per player</b> that
 * tracks then freezes; 1 s later a flame column erupts (damage + fire). Pursuit and column
 * durations <b>halved</b>. The cycle repeats 3–5 times without pause, with a
 * delay only at the end.
 */
public class FireShadowAttack implements CsBossAttack {

    private static final int FREEZE_AT = 60;      // 3 s pursuit (halved)
    private static final int COLUMN_START = 80;   // +1 s after freeze
    private static final int COLUMN_END = 110;    // column 1.5 s (halved)
    private static final int DISCARD_AT = 110;
    private static final int CYCLE_LEN = 112;
    private static final int END_DELAY = 40;      // 2 s at end of attack
    private static final int NOMINAL_CYCLES = 4;  // ±25% ⇒ 3–5
    private static final double COLUMN_RADIUS = 1.0;
    private static final double COLUMN_HEIGHT = 4.0;
    private static final float FIRE_DAMAGE = 1.0F;
    private static final int FIRE_TICKS = 60;

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private final List<Shadow> shadows = new ArrayList<>();
    private int cycles;
    private int tick;
    private boolean done;

    private record Shadow(AttackShadowEntity entity, UUID target) {}

    public FireShadowAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.done = false;
        this.shadows.clear();
        this.cycles = CsBossAttackLib.varyOccurrences(NOMINAL_CYCLES, rng);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        int cycleTick = tick % CYCLE_LEN;
        int cycleIndex = tick / CYCLE_LEN;

        if (cycleIndex < cycles && cycleTick == 0) {
            spawnCycle(level, session);
        }
        if (cycleIndex < cycles) {
            driveCycle(level, session, boss, cycleTick);
        }

        if (tick >= cycles * CYCLE_LEN + END_DELAY) {
            done = true;
        }
        tick++;
    }

    private void spawnCycle(ServerLevel level, BossBattleSession session) {
        shadows.clear();
        for (ServerPlayer p : session.aliveParticipants(level)) {
            AttackShadowEntity e = AttackShadowEntity.spawn(level, p.getX(), p.getY(), p.getZ(), session.getId());
            session.trackAttackEntity(e);
            shadows.add(new Shadow(e, p.getUUID()));
            CsBossAttackLib.sound(level, p.getX(), p.getY(), p.getZ(),
                    "minecraft:block.fire.ambient", net.minecraft.sounds.SoundSource.HOSTILE, 0.8F, 1.2F);
        }
    }

    private void driveCycle(ServerLevel level, BossBattleSession session, CsBossEntity boss, int cycleTick) {
        // Small rising flames from the shadow before the column (telegraph).
        if (cycleTick < COLUMN_START) {
            for (Shadow s : shadows) {
                if (s.entity.isAlive()) {
                    CsBossAttackLib.risingTelegraph(level, s.entity.getX(), s.entity.getY(), s.entity.getZ(),
                            net.minecraft.core.particles.ParticleTypes.FLAME);
                }
            }
        }
        if (cycleTick < FREEZE_AT) {
            for (Shadow s : shadows) {
                if (s.entity.isAlive()
                        && level.getPlayerByUUID(s.target) instanceof ServerPlayer p && p.isAlive()) {
                    CsBossAttackLib.chase(s.entity, p.getX(), p.getY(), p.getZ(), CsBossAttackLib.CHASE_SPEED);
                }
            }
        } else if (cycleTick == FREEZE_AT) {
            boss.triggerAttackAnimation();
        } else if (cycleTick >= COLUMN_START && cycleTick < COLUMN_END) {
            for (Shadow s : shadows) {
                if (!s.entity.isAlive()) {
                    continue;
                }
                double x = s.entity.getX();
                double y = s.entity.getY();
                double z = s.entity.getZ();
                if (cycleTick == COLUMN_START) {
                    CsBossAttackLib.sound(level, x, y, z,
                            "cobblemon:move.eruption.actor", net.minecraft.sounds.SoundSource.HOSTILE, 1.3F, 1.0F);
                }
                CsBossAttackLib.flameColumn(level, x, y, z, COLUMN_HEIGHT);
                if (cycleTick % 5 == 0) {
                    CsBossAttackLib.damagePlayersInColumn(level, session, x, y, z,
                            COLUMN_RADIUS, COLUMN_HEIGHT, FIRE_DAMAGE, FIRE_TICKS);
                }
            }
        } else if (cycleTick == DISCARD_AT) {
            for (Shadow s : shadows) {
                if (s.entity.isAlive()) {
                    s.entity.discard();
                }
            }
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
