package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDigdirtEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@code base_ground_1} (plan 113, Type A): identical to {@code base_fire_1}, but the tracking entity
 * is a dirt mound (model {@code attack_digdirt}, meteorite texture) instead of the flat shadow.
 */
public class GroundShadowAttack implements CsBossAttack {

    private static final int FREEZE_AT = 60;
    private static final int COLUMN_START = 80;
    private static final int COLUMN_END = 110;
    private static final int DISCARD_AT = 110;
    private static final int CYCLE_LEN = 112;
    private static final int END_DELAY = 16;      // ≈240 t total (2 cycles)
    private static final int CYCLES = 2;          // deterministic (2*112+16 = 240)
    private static final double COLUMN_RADIUS = 1.0;
    private static final double COLUMN_HEIGHT = 4.0;
    private static final float FIRE_DAMAGE = 1.0F;
    private static final int FIRE_TICKS = 60;

    private final String id;
    private final List<Mound> mounds = new ArrayList<>();
    private int cycles;
    private int tick;
    private boolean done;

    private record Mound(AttackDigdirtEntity entity, UUID target) {}

    public GroundShadowAttack(String id) {
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
        this.done = false;
        this.mounds.clear();
        this.cycles = CYCLES;
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
        mounds.clear();
        for (ServerPlayer p : session.aliveParticipants(level)) {
            AttackDigdirtEntity e = AttackDigdirtEntity.spawn(level, p.getX(), p.getY(), p.getZ(), session.getId());
            session.trackAttackEntity(e);
            mounds.add(new Mound(e, p.getUUID()));
            CsBossAttackLib.sound(level, p.getX(), p.getY(), p.getZ(),
                    "minecraft:block.rooted_dirt.break", net.minecraft.sounds.SoundSource.HOSTILE, 0.8F, 1.0F);
        }
    }

    private void driveCycle(ServerLevel level, BossBattleSession session, CsBossEntity boss, int cycleTick) {
        if (cycleTick < COLUMN_START) {
            for (Mound m : mounds) {
                if (m.entity.isAlive()) {
                    CsBossAttackLib.risingTelegraph(level, m.entity.getX(), m.entity.getY(), m.entity.getZ(),
                            net.minecraft.core.particles.ParticleTypes.FLAME);
                }
            }
        }
        if (cycleTick < FREEZE_AT) {
            for (Mound m : mounds) {
                if (m.entity.isAlive()
                        && level.getPlayerByUUID(m.target) instanceof ServerPlayer p && p.isAlive()) {
                    CsBossAttackLib.chase(m.entity, p.getX(), p.getY(), p.getZ(), CsBossAttackLib.CHASE_SPEED);
                }
            }
        } else if (cycleTick == FREEZE_AT) {
            boss.triggerAttackAnimation();
        } else if (cycleTick >= COLUMN_START && cycleTick < COLUMN_END) {
            for (Mound m : mounds) {
                if (!m.entity.isAlive()) {
                    continue;
                }
                double x = m.entity.getX();
                double y = m.entity.getY();
                double z = m.entity.getZ();
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
            for (Mound m : mounds) {
                if (m.entity.isAlive()) {
                    m.entity.discard();
                }
            }
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
