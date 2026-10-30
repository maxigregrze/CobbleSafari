package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.carrier.CarrierEntity;
import maxigregrze.cobblesafari.entity.carrier.CarrierOptions;
import maxigregrze.cobblesafari.entity.carrier.OrbitCarrierMotion;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackTornadoEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code base_flying_4} — Gust / Whirlwind (plan 199 § 12.4, TARGETED). Une tornade par joueur le
 * poursuit lentement. Au contact : 3 dégâts, puis prise par un {@link CarrierEntity} (un tour autour
 * de la tornade en montant, 1,5 s) et projection vers l'extérieur ; immunité 2 s à cette tornade.
 */
public class WhirlwindChaseAttack implements CsBossAttack {

    /** 0,12 bloc/tick = 2,4 blocs/s : plus lent que la marche (4,3) et le sprint (5,6), on peut la semer. */
    private static final double CHASE_SPEED = 0.12;
    private static final int CHASE_END = 220;
    private static final int DURATION = 240;
    private static final double HIT_RADIUS = 1.2;
    private static final double HIT_HEIGHT = 3.0;
    private static final float DAMAGE = 3.0F; // base 3
    private static final int GRAB_TICKS = 30;
    private static final double ORBIT_RADIUS = 1.5;
    private static final double ORBIT_RISE = 2.5;
    private static final double FLING_OUT = 0.9;
    private static final double FLING_UP = 0.3;
    private static final int IMMUNITY_TICKS = 40;

    private static final class Twister {
        final AttackTornadoEntity entity;
        final UUID target;
        final Map<UUID, Integer> immuneUntil = new HashMap<>();

        Twister(AttackTornadoEntity entity, UUID target) {
            this.entity = entity;
            this.target = target;
        }
    }

    private final String id;
    private final List<Twister> twisters = new ArrayList<>();
    private int tick;
    private boolean done;

    public WhirlwindChaseAttack(String id) {
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
        this.twisters.clear();
        double y = session.getFloorY();
        for (ServerPlayer p : session.aliveParticipants(level)) {
            AttackTornadoEntity t = session.trackAttackEntity(
                    AttackTornadoEntity.spawn(level, boss.getX(), y, boss.getZ(), session.getId()));
            twisters.add(new Twister(t, p.getUUID()));
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.gust.actor", SoundSource.HOSTILE, 1.6F, 0.6F);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        for (Twister tw : twisters) {
            AttackTornadoEntity t = tw.entity;
            if (!t.isAlive()) {
                continue;
            }
            if (tick < CHASE_END) {
                if (level.getPlayerByUUID(tw.target) instanceof ServerPlayer p && p.isAlive()) {
                    CsBossAttackLib.chase(t, p.getX(), t.getY(), p.getZ(),
                            CsBossAttackLib.homingStep(t, p.getX(), p.getZ(), CHASE_SPEED));
                }
                grabOnContact(level, session, tw);
            } else {
                t.setAlpha(1.0F - (tick - CHASE_END) / (float) (DURATION - CHASE_END));
            }
        }
        if (++tick >= DURATION) {
            for (Twister tw : twisters) {
                if (tw.entity.isAlive()) {
                    tw.entity.discard();
                }
            }
            done = true;
        }
    }

    private void grabOnContact(ServerLevel level, BossBattleSession session, Twister tw) {
        AttackTornadoEntity t = tw.entity;
        for (ServerPlayer p : session.aliveParticipants(level)) {
            if (p.isPassenger() || tick < tw.immuneUntil.getOrDefault(p.getUUID(), 0)) {
                continue;
            }
            double dx = p.getX() - t.getX();
            double dz = p.getZ() - t.getZ();
            double dy = p.getY() - t.getY();
            if (dx * dx + dz * dz > HIT_RADIUS * HIT_RADIUS || dy < -0.5 || dy > HIT_HEIGHT) {
                continue;
            }
            CsBossDamage.hurt(p, session, DAMAGE);
            if (!CsBossDamage.stillInFight(p, level) || p.isPassenger()) {
                continue; // rescued out of the arena by the lethal touch: no cross-level grab
            }
            double angle = Math.atan2(dz, dx);
            Vec3 fling = new Vec3(Math.cos(angle) * FLING_OUT, FLING_UP, Math.sin(angle) * FLING_OUT);
            CarrierEntity carrier = CarrierEntity.grab(level, p,
                    new OrbitCarrierMotion(() -> t.isAlive() ? t.position() : null, ORBIT_RADIUS, angle, 1.0,
                            p.getY(), ORBIT_RISE, GRAB_TICKS),
                    new CarrierOptions(false, null, fling, false, null));
            session.trackAttackEntity(carrier);
            tw.immuneUntil.put(p.getUUID(), tick + GRAB_TICKS + IMMUNITY_TICKS);
            CsBossAttackLib.sound(level, p.getX(), p.getY(), p.getZ(),
                    "cobblemon:move.gust.target", SoundSource.HOSTILE, 1.2F, 1.0F);
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
