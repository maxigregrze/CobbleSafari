package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackGiratinaOrbEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * {@code distortion_4} (plan 113, Type A) : aveuglement 10 s à tous les joueurs ; pour chaque joueur,
 * une orbe de Giratina (lueur rouge, flotte 1 bloc au‑dessus du sol) apparaît toutes les 2 s pendant
 * 10 s et le suit à vitesse de marche. Au contact : poison 10 s + 6 dégâts.
 */
public class DistortionOrbAttack implements CsBossAttack {

    private static final int BLINDNESS_TICKS = 200; // 10 s
    private static final int POISON_TICKS = 200;    // 10 s
    private static final int SPAWN_INTERVAL = 40;   // une orbe / 2 s
    private static final int WAVES = 5;             // sur 10 s
    private static final int END_DELAY = 120;
    private static final double WALK_SPEED = 0.2;
    private static final double FLOAT_HEIGHT = 1.0;
    private static final float DAMAGE = 6.0F;

    private final String id;
    private final List<Orb> orbs = new ArrayList<>();
    private int tick;
    private int wavesSpawned;
    private boolean done;

    private static final class Orb {
        final AttackGiratinaOrbEntity entity;
        final UUID target;

        Orb(AttackGiratinaOrbEntity entity, UUID target) {
            this.entity = entity;
            this.target = target;
        }
    }

    public DistortionOrbAttack(String id) {
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
        this.orbs.clear();
        CsBossAttackLib.applyEffectToAll(level, session, MobEffects.BLINDNESS, BLINDNESS_TICKS, 0);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (wavesSpawned < WAVES && tick == wavesSpawned * SPAWN_INTERVAL) {
            for (ServerPlayer p : session.aliveParticipants(level)) {
                AttackGiratinaOrbEntity orb = AttackGiratinaOrbEntity.spawn(level,
                        boss.getX(), boss.getY() + 1.0, boss.getZ(), session.getId());
                session.trackAttackEntity(orb);
                orbs.add(new Orb(orb, p.getUUID()));
            }
            wavesSpawned++;
        }

        Iterator<Orb> it = orbs.iterator();
        while (it.hasNext()) {
            Orb o = it.next();
            if (!o.entity.isAlive()) {
                it.remove();
                continue;
            }
            if (level.getPlayerByUUID(o.target) instanceof ServerPlayer p && p.isAlive()) {
                CsBossAttackLib.chase(o.entity, p.getX(), p.getY() + FLOAT_HEIGHT, p.getZ(), WALK_SPEED);
                if (o.entity.getBoundingBox().intersects(p.getBoundingBox())) {
                    if (p.hurt(CsBossDamage.bullet(level), DAMAGE)) {
                        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0));
                    }
                    o.entity.discard();
                    it.remove();
                }
            }
        }

        if (wavesSpawned >= WAVES && tick >= (WAVES - 1) * SPAWN_INTERVAL + END_DELAY) {
            done = true;
        }
        tick++;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
