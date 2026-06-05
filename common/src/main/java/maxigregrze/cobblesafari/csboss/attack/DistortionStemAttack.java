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
 * {@code distortion_1} (plan 107 § 6.5, révisé) : 4 cores invisibles plantés dans le repère du boss
 * (relatif à son orientation), qui se propagent (≤ 20/direction) en faisant pousser des colonnes de
 * tiges semi‑transparentes (≤ 5/core). Le boss tourne lentement sur lui‑même : toute la structure
 * orbite autour de lui. Les tiges infligent 5 cœurs + knockback au contact.
 */
public class DistortionStemAttack implements CsBossAttack {

    private static final int CORE_SEED = 19;      // 1 + 19 = 20 cores max/direction
    private static final double UNDER_BOSS = 2.0;
    private static final double FORWARD = 1.5;
    private static final int DURATION = 200;
    private static final float SPIN_DEG_PER_TICK = 2.0F; // rotation lente du boss
    private static final float STEM_DAMAGE = 10.0F;      // 5 cœurs
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
        // 4 directions relatives à l'orientation du boss (0/90/180/270°), 1.5 bloc en avant, 2 sous lui.
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

        // Rotation lente du boss sur lui-même (entraîne toute la structure attachée à son repère).
        float yaw = boss.getYRot() + SPIN_DEG_PER_TICK;
        boss.setYRot(yaw);
        boss.setYHeadRot(yaw);
        boss.yBodyRot = yaw;

        // Bourdonnement périodique de distorsion.
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

    /** Inflige 5 cœurs + knockback aux participants en contact avec une tige (i‑frames vanilla = anti‑spam). */
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
