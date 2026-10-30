package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackZoneRingEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@code jump_5} — Prescience (plan 199 § 13.5, TARGETED ; revu après retours). Toutes les secondes,
 * la position de chaque joueur est marquée d'un disque rose au sol qui se densifie ; 2 s plus tard,
 * une frappe psychique s'y abat (4 dégâts dans un rayon de 2,5) : il faut continuer à bouger.
 */
public class JumpFutureSightAttack implements CsBossAttack {

    private static final int RECORD_EVERY = 20;
    private static final int LAST_RECORD = 180;
    private static final int DELAY = 40;
    private static final float RADIUS = 2.5F;
    private static final double HIT_HEIGHT = 3.0;
    private static final float DAMAGE = 4.0F; // base 4
    private static final int MARK_COLOR = 0xFF5CC8;
    private static final float MARK_ALPHA_START = 0.15F;
    private static final float MARK_ALPHA_END = 0.6F;
    private static final int DURATION = 240;

    private record Mark(Vec3 pos, int strikeAt, AttackZoneRingEntity disc) {}

    private final String id;
    private final List<Mark> marks = new ArrayList<>();
    private int tick;
    private boolean done;

    public JumpFutureSightAttack(String id) {
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
        this.marks.clear();
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (tick % RECORD_EVERY == 0 && tick <= LAST_RECORD) {
            for (ServerPlayer p : session.aliveParticipants(level)) {
                AttackZoneRingEntity disc = session.trackAttackEntity(AttackZoneRingEntity.spawn(level,
                        p.getX(), p.getY(), p.getZ(), session.getId(), 0.0F, RADIUS, MARK_COLOR,
                        MARK_ALPHA_START, DELAY + 5));
                marks.add(new Mark(p.position(), tick + DELAY, disc));
            }
            if (tick == 0) {
                boss.triggerAttackAnimation();
                CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                        "cobblemon:move.psychic.actor", SoundSource.HOSTILE, 1.6F, 1.0F);
            }
        }
        Iterator<Mark> it = marks.iterator();
        while (it.hasNext()) {
            Mark m = it.next();
            if (tick >= m.strikeAt) {
                strike(level, session, m.pos);
                if (m.disc.isAlive()) {
                    m.disc.discard();
                }
                it.remove();
            } else if (m.disc.isAlive()) {
                float f = 1.0F - (m.strikeAt - tick) / (float) DELAY;
                m.disc.setAlpha(MARK_ALPHA_START + (MARK_ALPHA_END - MARK_ALPHA_START) * f);
            }
        }
        if (++tick >= DURATION) {
            for (Mark m : marks) {
                if (m.disc.isAlive()) {
                    m.disc.discard();
                }
            }
            marks.clear();
            done = true;
        }
    }

    private void strike(ServerLevel level, BossBattleSession session, Vec3 at) {
        CsBossAttackLib.snowstorm(level, "cobblemon:fireblast_target", at);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.0, at.z, 20, 0.8, 1.0, 0.8, 0.08);
        CsBossAttackLib.sound(level, at.x, at.y, at.z, "cobblemon:move.psychic.target", SoundSource.HOSTILE, 1.2F, 1.0F);
        for (ServerPlayer p : session.aliveParticipants(level)) {
            double dx = p.getX() - at.x;
            double dz = p.getZ() - at.z;
            if (dx * dx + dz * dz <= RADIUS * RADIUS && Math.abs(p.getY() - at.y) <= HIT_HEIGHT) {
                CsBossDamage.hurt(p, session, DAMAGE);
            }
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
