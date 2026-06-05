package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDistortionFlowerEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * {@code distortion_2} (plan 113, Type A) : pour chaque joueur, une ombre part du boss et le suit 5 s.
 * Sur chaque bloc qu'elle traverse, une fleur de distorsion apparaît ; 2 s plus tard la fleur pousse
 * un mur de tiges verticales (6 blocs, actif 10 s). Répété 1‑3 fois.
 */
public class DistortionWalkAttack implements CsBossAttack {

    private static final int FOLLOW_TICKS = 100;  // 5 s de poursuite
    private static final double WALK_SPEED = 0.2; // l'ombre trace un chemin
    private static final int END_DELAY = 40;

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private final List<Walk> walks = new ArrayList<>();
    private int waves;
    private int tick;
    private int wavesSpawned;
    private boolean done;

    private static final class Walk {
        final AttackShadowEntity shadow;
        final UUID target;
        final int birthTick;
        BlockPos lastBlock;

        Walk(AttackShadowEntity shadow, UUID target, int birthTick) {
            this.shadow = shadow;
            this.target = target;
            this.birthTick = birthTick;
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
        return AttackCategory.TARGETED;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.wavesSpawned = 0;
        this.done = false;
        this.walks.clear();
        this.waves = 1 + rng.nextInt(3); // 1‑3
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (wavesSpawned < waves && tick == wavesSpawned * FOLLOW_TICKS) {
            for (ServerPlayer p : session.aliveParticipants(level)) {
                AttackShadowEntity shadow = AttackShadowEntity.spawn(level, boss.getX(),
                        session.getTriggerPos().getY(), boss.getZ(), session.getId());
                session.trackAttackEntity(shadow);
                walks.add(new Walk(shadow, p.getUUID(), tick));
            }
            wavesSpawned++;
        }

        Iterator<Walk> it = walks.iterator();
        while (it.hasNext()) {
            Walk w = it.next();
            if (!w.shadow.isAlive()) {
                it.remove();
                continue;
            }
            int age = tick - w.birthTick;
            if (age >= FOLLOW_TICKS) {
                w.shadow.discard();
                it.remove();
                continue;
            }
            driveWalk(level, session, w);
        }

        if (wavesSpawned >= waves && walks.isEmpty()
                && tick >= (waves - 1) * FOLLOW_TICKS + FOLLOW_TICKS + END_DELAY) {
            done = true;
        }
        tick++;
    }

    private void driveWalk(ServerLevel level, BossBattleSession session, Walk w) {
        if (level.getPlayerByUUID(w.target) instanceof ServerPlayer p && p.isAlive()) {
            CsBossAttackLib.chase(w.shadow, p.getX(), p.getY(), p.getZ(), WALK_SPEED);
        }
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
