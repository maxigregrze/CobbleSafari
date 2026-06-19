package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * {@code base_ghost_3} (Type A / TARGETED): an Elder-Guardian-style jumpscare. For each player a
 * minion (boss JSON minion model) sweeps in front of the camera — from 90° below the view centre up
 * to 90° above it over 1.5 s — facing the player, while the curse sound plays and 10 s of Nausea is
 * applied.
 */
public class GhostJumpscareAttack implements CsBossAttack {

    private static final int DURATION = 30; // 1.5 s sweep
    private static final int NAUSEA_TICKS = 200; // 10 s
    private static final double FACE_DISTANCE = 3.0;
    private static final double MINION_HEIGHT = 3.5;
    private static final double START_ANGLE = -90.0; // below the camera centre
    private static final double END_ANGLE = 90.0; // above the camera centre

    private final String id;
    private final List<Jump> jumps = new ArrayList<>();
    private int tick;
    private boolean done;

    private static final class Jump {
        final CsBossMinionEntity minion;
        final UUID target;

        Jump(CsBossMinionEntity minion, UUID target) {
            this.minion = minion;
            this.target = target;
        }
    }

    public GhostJumpscareAttack(String id) {
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
        this.jumps.clear();
        CsBossAttackLib.applyEffectToAll(level, session, MobEffects.CONFUSION, NAUSEA_TICKS, 0);
        for (ServerPlayer p : session.aliveParticipants(level)) {
            Vec3 pos = sweepPos(p, 0.0);
            CsBossMinionEntity minion = session.spawnMinion(level, pos);
            minion.resizeToHeight(MINION_HEIGHT);
            minion.faceTargetInstant(p.getEyePosition());
            jumps.add(new Jump(minion, p.getUUID()));
            CsBossAttackLib.sound(level, p.getX(), p.getY(), p.getZ(),
                    "minecraft:entity.elder_guardian.curse", SoundSource.HOSTILE, 1.4F, 1.0F);
        }
        boss.triggerAttackAnimation();
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        Iterator<Jump> it = jumps.iterator();
        while (it.hasNext()) {
            Jump j = it.next();
            if (!j.minion.isAlive()) {
                it.remove();
                continue;
            }
            if (level.getPlayerByUUID(j.target) instanceof ServerPlayer p && p.isAlive()) {
                double progress = Math.min(1.0, tick / (double) DURATION);
                Vec3 pos = sweepPos(p, progress);
                j.minion.setPos(pos.x, pos.y, pos.z);
                j.minion.faceTarget(p.getEyePosition());
            }
        }

        if (++tick >= DURATION) {
            for (Jump j : jumps) {
                if (j.minion.isAlive()) {
                    j.minion.discard();
                }
            }
            jumps.clear();
            done = true;
        }
    }

    /**
     * Position in front of the camera at sweep {@code progress} (0 → 1): the look direction tilted
     * around the view's horizontal axis from {@link #START_ANGLE} (below) to {@link #END_ANGLE} (above).
     */
    private static Vec3 sweepPos(ServerPlayer p, double progress) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1, 0, 0); // looking straight up/down: pick an arbitrary horizontal axis
        }
        right = right.normalize();
        Vec3 viewUp = right.cross(look).normalize();
        double ang = Math.toRadians(START_ANGLE + (END_ANGLE - START_ANGLE) * progress);
        Vec3 dir = look.scale(Math.cos(ang)).add(viewUp.scale(Math.sin(ang)));
        Vec3 center = eye.add(dir.scale(FACE_DISTANCE));
        return new Vec3(center.x, center.y - MINION_HEIGHT * 0.5, center.z);
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
