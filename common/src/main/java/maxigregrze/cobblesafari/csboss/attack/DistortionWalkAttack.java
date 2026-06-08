package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDistortionFlowerEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@code distortion_2} (plan 113, Type C / SPREAD): every couple of seconds the boss sends out a
 * shadow in a <b>random direction</b>. The shadow travels straight forward (with a small one-time
 * deviation a few degrees left or right around mid-course) while leaving a distortion-flower trail;
 * each flower later grows a vertical stem wall. 4–6 shadows over the attack.
 */
public class DistortionWalkAttack implements CsBossAttack {

    private static final int SEND_INTERVAL = 40;   // a new shadow every 2 s
    private static final int SHADOW_LIFE = 100;    // 5 s of forward travel
    private static final double WALK_SPEED = 0.2;  // shadow traces a path
    private static final double DEVIATION_DEG = 12.0; // max mid-course turn (left or right)
    private static final int END_DELAY = 40;

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private final List<Walk> walks = new ArrayList<>();
    private int count;
    private int tick;
    private int sent;
    private boolean done;

    private static final class Walk {
        final AttackShadowEntity shadow;
        final int birthTick;
        double dirX;
        double dirZ;
        BlockPos lastBlock;
        boolean deviated;

        Walk(AttackShadowEntity shadow, int birthTick, double dirX, double dirZ) {
            this.shadow = shadow;
            this.birthTick = birthTick;
            this.dirX = dirX;
            this.dirZ = dirZ;
        }
    }

    public DistortionWalkAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.SPREAD;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.sent = 0;
        this.done = false;
        this.walks.clear();
        this.count = 4 + rng.nextInt(3); // 4‑6
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (sent < count && tick == sent * SEND_INTERVAL) {
            sendShadow(level, session, boss);
            sent++;
        }

        Iterator<Walk> it = walks.iterator();
        while (it.hasNext()) {
            Walk w = it.next();
            if (!w.shadow.isAlive()) {
                it.remove();
                continue;
            }
            int age = tick - w.birthTick;
            if (age >= SHADOW_LIFE) {
                w.shadow.discard();
                it.remove();
                continue;
            }
            driveWalk(level, session, w, age);
        }

        if (sent >= count && walks.isEmpty()
                && tick >= (count - 1) * SEND_INTERVAL + SHADOW_LIFE + END_DELAY) {
            done = true;
        }
        tick++;
    }

    private void sendShadow(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        double theta = rng.nextDouble() * Math.PI * 2.0;
        AttackShadowEntity shadow = AttackShadowEntity.spawn(level, boss.getX(),
                session.getTriggerPos().getY(), boss.getZ(), session.getId());
        session.trackAttackEntity(shadow);
        walks.add(new Walk(shadow, tick, Math.cos(theta), Math.sin(theta)));
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.shadowball.actor", net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 0.8F);
    }

    private void driveWalk(ServerLevel level, BossBattleSession session, Walk w, int age) {
        // One-time slight course deviation around mid-life.
        if (!w.deviated && age >= SHADOW_LIFE / 2) {
            w.deviated = true;
            double delta = Math.toRadians((rng.nextDouble() * 2.0 - 1.0) * DEVIATION_DEG);
            double cos = Math.cos(delta);
            double sin = Math.sin(delta);
            double nx = w.dirX * cos - w.dirZ * sin;
            double nz = w.dirX * sin + w.dirZ * cos;
            w.dirX = nx;
            w.dirZ = nz;
        }
        w.shadow.setPos(w.shadow.getX() + w.dirX * WALK_SPEED, w.shadow.getY(),
                w.shadow.getZ() + w.dirZ * WALK_SPEED);

        BlockPos here = BlockPos.containing(w.shadow.getX(), w.shadow.getY(), w.shadow.getZ());
        if (!here.equals(w.lastBlock)) {
            w.lastBlock = here;
            AttackDistortionFlowerEntity flower = AttackDistortionFlowerEntity.spawn(level,
                    here.getX() + 0.5, w.shadow.getY(), here.getZ() + 0.5, session.getId());
            session.trackAttackEntity(flower);
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
