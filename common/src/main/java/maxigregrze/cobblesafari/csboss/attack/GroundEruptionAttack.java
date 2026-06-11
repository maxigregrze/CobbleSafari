package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDigdirtEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@code base_ground_2} (plan 126 § 2) : comme {@code base_ghost_1}, mais la grande ombre est
 * remplacée par un tas digdirt ×3 vibrant avec particules terre, puis éruption minion.
 */
public class GroundEruptionAttack implements CsBossAttack {

    private static final int FOLLOW_END = 70;
    private static final int RAMP_TICKS = 40;
    private static final int RISE_START = 80;
    private static final int RISE_END = 90;
    private static final int FADE_END = 100;
    private static final int CYCLE_LEN = 112;
    private static final int END_DELAY = 16;
    private static final int CYCLES = 2;
    private static final double RUN_SPEED = 0.30;
    private static final double MINION_DROP = 2.0;
    private static final float MINION_DAMAGE = 8.0F;
    private static final float DIGDIRT_SCALE = 3.0F;

    private final String id;
    private final List<Mound> mounds = new ArrayList<>();
    private int cycles;
    private int tick;
    private boolean done;

    private static final class Mound {
        final AttackDigdirtEntity digdirt;
        final UUID target;
        CsBossMinionEntity minion;
        double moundY;

        Mound(AttackDigdirtEntity digdirt, UUID target) {
            this.digdirt = digdirt;
            this.target = target;
        }
    }

    public GroundEruptionAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
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
            spawnCycle(level, session, boss);
        }
        if (cycleIndex < cycles) {
            driveCycle(level, session, cycleTick);
        }

        if (tick >= cycles * CYCLE_LEN + END_DELAY) {
            done = true;
        }
        tick++;
    }

    private void spawnCycle(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        mounds.clear();
        double floorY = session.getTriggerPos().getY();
        for (ServerPlayer p : session.aliveParticipants(level)) {
            AttackDigdirtEntity digdirt = AttackDigdirtEntity.spawn(level,
                    boss.getX(), floorY, boss.getZ(), session.getId());
            digdirt.setRenderScale(DIGDIRT_SCALE);
            digdirt.setDirtParticles(true);
            digdirt.setDirtModel(true); // texture de terre (base_ground_2) au lieu de la météorite
            session.trackAttackEntity(digdirt);
            mounds.add(new Mound(digdirt, p.getUUID()));
            CsBossAttackLib.sound(level, boss.getX(), floorY, boss.getZ(),
                    "minecraft:block.rooted_dirt.break", SoundSource.HOSTILE, 1.0F, 0.9F);
        }
    }

    private void driveCycle(ServerLevel level, BossBattleSession session, int cycleTick) {
        for (Mound m : mounds) {
            if (!m.digdirt.isAlive()) {
                continue;
            }
            if (cycleTick < FOLLOW_END) {
                double speed = RUN_SPEED * Math.min(1.0, cycleTick / (double) RAMP_TICKS);
                if (level.getPlayerByUUID(m.target) instanceof ServerPlayer p && p.isAlive()) {
                    CsBossAttackLib.chase(m.digdirt, p.getX(), p.getY(), p.getZ(), speed);
                }
            } else if (cycleTick == FOLLOW_END) {
                m.digdirt.setVibrating(true);
            } else if (cycleTick == RISE_START) {
                m.moundY = m.digdirt.getY();
                Vec3 pos = new Vec3(m.digdirt.getX(), m.moundY - MINION_DROP, m.digdirt.getZ());
                m.minion = session.spawnMinion(level, pos);
                m.minion.resizeToHeight(2.5);
                m.minion.setAlpha(0.0F);
                if (level.getPlayerByUUID(m.target) instanceof ServerPlayer tp) {
                    m.minion.faceTargetInstant(tp.position());
                }
                CsBossAttackLib.sound(level, pos.x, pos.y, pos.z,
                        "minecraft:entity.vex.charge", SoundSource.HOSTILE, 1.2F, 0.7F);
            } else if (cycleTick == RISE_START + 1) {
                if (m.minion != null && m.minion.isAlive()) {
                    m.minion.setAlpha(1.0F);
                }
            } else if (cycleTick > RISE_START && cycleTick < RISE_END) {
                if (m.minion != null && m.minion.isAlive()) {
                    double progress = (cycleTick - RISE_START) / (double) (RISE_END - RISE_START);
                    m.minion.setPos(m.digdirt.getX(), m.moundY - MINION_DROP + MINION_DROP * progress, m.digdirt.getZ());
                }
            } else if (cycleTick >= RISE_END && cycleTick < FADE_END) {
                float alpha = 1.0F - (cycleTick - RISE_END) / (float) (FADE_END - RISE_END);
                m.digdirt.setAlpha(alpha);
                if (m.minion != null && m.minion.isAlive()) {
                    m.minion.setAlpha(alpha);
                }
            } else if (cycleTick == FADE_END) {
                m.digdirt.discard();
                if (m.minion != null && m.minion.isAlive()) {
                    m.minion.discard();
                }
            }

            if (m.minion != null && m.minion.isAlive() && cycleTick >= RISE_START && cycleTick < FADE_END) {
                if (level.getPlayerByUUID(m.target) instanceof ServerPlayer tp) {
                    m.minion.faceTarget(tp.position());
                }
                for (ServerPlayer p : session.aliveParticipants(level)) {
                    if (p.getBoundingBox().intersects(m.minion.getBoundingBox())) {
                        p.hurt(CsBossDamage.bullet(level), MINION_DAMAGE);
                    }
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
