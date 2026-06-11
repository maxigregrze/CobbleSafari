package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@code base_ghost_1} (plan 107/108 § 3, revised plan 109): a large 3×3 shadow <b>per player</b>,
 * which tracks its target (acceleration 0 → run) then freezes; a {@code csboss_minion} (boss
 * minion species) erupts from the ground to mid-shadow, damages on contact (8), then shadow + minion
 * dissipate. The cycle repeats 2–5 times without pause, with a delay only at the end.
 */
public class GhostShadowAttack implements CsBossAttack {

    private static final int FOLLOW_END = 70;    // 3.5 s of pursuit (compressed)
    private static final int RAMP_TICKS = 40;    // acceleration 0 → run over 2 s
    private static final int RISE_START = 80;    // 0.5 s after freeze
    private static final int RISE_END = 90;      // minion rise (0.5 s)
    private static final int FADE_END = 100;     // fade (0.5 s) + discard
    private static final int CYCLE_LEN = 112;
    private static final int END_DELAY = 16;     // ≈240 t total (2 cycles)
    private static final int CYCLES = 2;         // deterministic (2*112+16 = 240)
    private static final double RUN_SPEED = 0.30;
    private static final double MINION_DROP = 2.0;
    private static final float MINION_DAMAGE = 8.0F;

    private final String id;
    private final List<Ghost> ghosts = new ArrayList<>();
    private int cycles;
    private int tick;
    private boolean done;

    private static final class Ghost {
        final AttackShadowEntity shadow;
        final UUID target;
        CsBossMinionEntity minion;
        double shadowY;

        Ghost(AttackShadowEntity shadow, UUID target) {
            this.shadow = shadow;
            this.target = target;
        }
    }

    public GhostShadowAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.done = false;
        this.ghosts.clear();
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
        ghosts.clear();
        for (ServerPlayer p : session.aliveParticipants(level)) {
            AttackShadowEntity shadow = AttackShadowEntity.spawn(level,
                    boss.getX(), session.getTriggerPos().getY(), boss.getZ(), session.getId(), true);
            session.trackAttackEntity(shadow);
            ghosts.add(new Ghost(shadow, p.getUUID()));
        }
    }

    private void driveCycle(ServerLevel level, BossBattleSession session, int cycleTick) {
        for (Ghost g : ghosts) {
            if (!g.shadow.isAlive()) {
                continue;
            }
            if (cycleTick < FOLLOW_END) {
                double speed = RUN_SPEED * Math.min(1.0, cycleTick / (double) RAMP_TICKS);
                if (level.getPlayerByUUID(g.target) instanceof ServerPlayer p && p.isAlive()) {
                    CsBossAttackLib.chase(g.shadow, p.getX(), p.getY(), p.getZ(), speed);
                }
            } else if (cycleTick == RISE_START) {
                g.shadowY = g.shadow.getY();
                Vec3 pos = new Vec3(g.shadow.getX(), g.shadowY - MINION_DROP, g.shadow.getZ());
                g.minion = session.spawnMinion(level, pos);
                g.minion.resizeToHeight(2.5); // hitbox ~2.5 blocks regardless of species
                g.minion.setAlpha(0.0F); // masqué 1 tick pour éviter le flash à l'échelle boss (plan 126 § 5)
                // Face the player from the very first frame it starts emerging from the shadow.
                if (level.getPlayerByUUID(g.target) instanceof ServerPlayer tp) {
                    g.minion.faceTargetInstant(tp.position());
                }

                CsBossAttackLib.sound(level, pos.x, pos.y, pos.z,
                        "minecraft:entity.vex.charge", SoundSource.HOSTILE, 1.2F, 0.7F);
            } else if (cycleTick == RISE_START + 1) {
                if (g.minion != null && g.minion.isAlive()) {
                    g.minion.setAlpha(1.0F);
                }
            } else if (cycleTick > RISE_START && cycleTick < RISE_END) {
                if (g.minion != null && g.minion.isAlive()) {
                    double progress = (cycleTick - RISE_START) / (double) (RISE_END - RISE_START);
                    g.minion.setPos(g.shadow.getX(), g.shadowY - MINION_DROP + MINION_DROP * progress, g.shadow.getZ());
                }
            } else if (cycleTick >= RISE_END && cycleTick < FADE_END) {
                float alpha = 1.0F - (cycleTick - RISE_END) / (float) (FADE_END - RISE_END);
                g.shadow.setAlpha(alpha);
                if (g.minion != null && g.minion.isAlive()) {
                    g.minion.setAlpha(alpha);
                }
            } else if (cycleTick == FADE_END) {
                g.shadow.discard();
                if (g.minion != null && g.minion.isAlive()) {
                    g.minion.discard();
                }
            }

            if (g.minion != null && g.minion.isAlive() && cycleTick >= RISE_START && cycleTick < FADE_END) {
                // Minion always faces the player it follows.
                if (level.getPlayerByUUID(g.target) instanceof ServerPlayer tp) {
                    g.minion.faceTarget(tp.position());
                }
                for (ServerPlayer p : session.aliveParticipants(level)) {
                    if (p.getBoundingBox().intersects(g.minion.getBoundingBox())) {
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
