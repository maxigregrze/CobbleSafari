package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDistortionStemCoreEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDistortionStemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.UUID;

/**
 * {@code distortion_1} (plan 107 § 6.5, revised): 4 invisible cores planted in the boss frame
 * (relative to its orientation), which spread (≤ 20/direction) by growing columns of
 * semi-transparent stems (≤ 5/core). The boss spins slowly on itself: the entire structure
 * orbits around it. Stems deal 5 hearts + knockback on contact.
 */
public class DistortionStemAttack implements CsBossAttack {

    private static final int CORE_SEED = 19;      // 1 + 19 = 20 cores max/direction
    private static final double UNDER_BOSS = 2.0;
    private static final double FORWARD = 1.5;
    private static final int DURATION = 240;             // ≈12 s
    private static final float SPIN_DEG_PER_TICK = 1.0F; // slow boss rotation
    private static final float STEM_DAMAGE = 10.0F;      // 5 hearts
    private static final double KNOCKBACK = 0.7;

    private final String id;
    private int tick;
    private boolean done;

    public DistortionStemAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public boolean controlsBossRotation() {
        return true;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.done = false;
        boss.triggerAttackAnimation();
        // 4 directions relative to boss orientation (0/90/180/270°), 1.5 blocks forward, 2 below.
        for (int dir = 0; dir < 4; dir++) {
            AttackDistortionStemCoreEntity core = AttackDistortionStemCoreEntity.spawn(
                    level, session.getId(), boss, dir * 90.0, FORWARD, -UNDER_BOSS, CORE_SEED);
            session.trackAttackEntity(core);
        }
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.shadowball.actor", SoundSource.HOSTILE, 1.6F, 0.6F);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }

        // Slow boss spin (drags the entire structure attached to its frame).
        float yaw = boss.getYRot() + SPIN_DEG_PER_TICK;
        boss.setYRot(yaw);
        boss.setYHeadRot(yaw);
        boss.yBodyRot = yaw;

        // Periodic distortion hum.
        if (tick % 30 == 0) {
            CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                    "minecraft:block.portal.ambient", SoundSource.HOSTILE, 1.2F, 0.5F);
        }

        damageOnContact(level, session, boss);

        if (++tick >= DURATION) {
            for (UUID u : new ArrayList<>(session.getActiveAttackEntities())) {
                Entity e = level.getEntity(u);
                if (e != null) {
                    e.discard();
                }
            }
            session.getActiveAttackEntities().clear();
            done = true;
        }
    }

    /** Deals 5 hearts + knockback to participants in contact with a stem (vanilla i-frames = anti-spam). */
    private void damageOnContact(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        for (UUID u : new ArrayList<>(session.getActiveAttackEntities())) {
            if (!(level.getEntity(u) instanceof AttackDistortionStemEntity stem) || !stem.isAlive()) {
                continue;
            }
            for (ServerPlayer p : session.aliveParticipants(level)) {
                if (!p.getBoundingBox().intersects(stem.getBoundingBox())) {
                    continue;
                }
                if (p.hurt(CsBossDamage.bullet(level), STEM_DAMAGE)) {
                    p.knockback(KNOCKBACK, boss.getX() - p.getX(), boss.getZ() - p.getZ());
                }
            }
        }
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.SPREAD;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
