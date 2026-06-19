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
 * {@code distortion_4} (Type A): one Giratina orb (red glow, floats 1 block above the
 * ground) spawns per player at the very start and follows it at walking speed while always facing it,
 * until it despawns ({@link #ORB_LIFETIME}). Darkness lasts 1 s longer than that despawn time. On
 * contact with the player it follows the orb deals 18 damage and vanishes (it is hard to avoid).
 */
public class DistortionOrbAttack implements CsBossAttack {

    private static final int ORB_LIFETIME = 240; // ≈12 s before the orbs despawn
    private static final int DARKNESS_TICKS = ORB_LIFETIME + 20; // 1 s longer than the orbs
    private static final double WALK_SPEED = 0.16; // slightly slower follow
    private static final double FLOAT_HEIGHT = 1.0;
    private static final double HIT_RADIUS = 1.2; // horizontal contact with the followed player
    private static final float HIT_DAMAGE = 18.0F; // hard to avoid ⇒ high damage

    private final String id;
    private final List<Orb> orbs = new ArrayList<>();
    private int tick;
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
        this.done = false;
        this.orbs.clear();
        CsBossAttackLib.applyEffectToAll(level, session, MobEffects.DARKNESS, DARKNESS_TICKS, 0);
        // One orb per player, all spawned at the very start.
        for (ServerPlayer p : session.aliveParticipants(level)) {
            AttackGiratinaOrbEntity orb = AttackGiratinaOrbEntity.spawn(level,
                    boss.getX(), boss.getY() + 1.0, boss.getZ(), session.getId());
            session.trackAttackEntity(orb);
            orbs.add(new Orb(orb, p.getUUID()));
        }
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        Iterator<Orb> it = orbs.iterator();
        while (it.hasNext()) {
            Orb o = it.next();
            if (!o.entity.isAlive()) {
                it.remove();
                continue;
            }
            if (level.getPlayerByUUID(o.target) instanceof ServerPlayer p && p.isAlive()) {
                CsBossAttackLib.chase(o.entity, p.getX(), p.getY() + FLOAT_HEIGHT, p.getZ(),
                        CsBossAttackLib.homingStep(o.entity, p.getX(), p.getZ(), WALK_SPEED));
                o.entity.facePlayer(p.getX(), p.getZ()); // always look at the player it follows
                // Collision with the followed player: heavy hit, then the orb vanishes.
                double dx = o.entity.getX() - p.getX();
                double dz = o.entity.getZ() - p.getZ();
                if (dx * dx + dz * dz <= HIT_RADIUS * HIT_RADIUS) {
                    p.hurt(CsBossDamage.bullet(level), HIT_DAMAGE);
                    o.entity.discard();
                    it.remove();
                }
            }
        }

        // Orbs despawn after their lifetime (darkness outlasts them by 1 s).
        if (tick >= ORB_LIFETIME) {
            for (Orb o : orbs) {
                if (o.entity.isAlive()) {
                    o.entity.discard();
                }
            }
            orbs.clear();
            done = true;
        }
        tick++;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
