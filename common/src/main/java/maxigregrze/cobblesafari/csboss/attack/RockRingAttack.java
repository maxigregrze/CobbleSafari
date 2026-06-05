package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackMeteoriteEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@code base_rock_2} (plan 109) : autour de <b>chaque joueur</b>, un anneau (rayon 3) d'ombres
 * statiques ; 1 s plus tard, une météorite tombe sur chacune (comme {@code base_rock_1}) et pose un
 * bloc météorite. 1 s après l'impact, on recommence. 3‑5 vagues au total. La météorite blesse (18)
 * un joueur traversé.
 */
public class RockRingAttack implements CsBossAttack {

    private static final int RING_RADIUS = 3;
    private static final int FALL_TICKS = 20;    // chute 2× plus lente (10→20), visible plus longtemps
    private static final int IMPACT_AT = 30;     // impact inchangé
    private static final int METEOR_AT = IMPACT_AT - FALL_TICKS; // 10 : météorite apparaît plus tôt
    private static final int WAVE_INTERVAL = IMPACT_AT + 20;     // 1 s après l'impact ⇒ 50
    private static final int NOMINAL_WAVES = 4;  // ±25 % ⇒ 3‑5
    private static final int RING_STAGGER = 4;   // ticks de décalage de chute par anneau (centre → extérieur)
    private static final double FALL_HEIGHT = 20.0;
    private static final float METEOR_DAMAGE = 18.0F;
    private static final List<BlockPos> RING = CsBossGridShapes.filledCircle(RING_RADIUS);

    private final String id;
    private final RandomSource rng = RandomSource.create();
    private final List<Rock> rocks = new ArrayList<>();
    private int waves;
    private int tick;
    private int wavesSpawned;
    private boolean done;

    private static final class Rock {
        final AttackShadowEntity shadow;
        final int birthTick;
        AttackMeteoriteEntity meteor;
        boolean damaged;

        Rock(AttackShadowEntity shadow, int birthTick) {
            this.shadow = shadow;
            this.birthTick = birthTick;
        }
    }

    public RockRingAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.wavesSpawned = 0;
        this.done = false;
        this.rocks.clear();
        this.waves = CsBossAttackLib.varyOccurrences(NOMINAL_WAVES, rng);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (wavesSpawned < waves && tick == wavesSpawned * WAVE_INTERVAL) {
            spawnWave(level, session);
            wavesSpawned++;
        }

        Iterator<Rock> it = rocks.iterator();
        while (it.hasNext()) {
            Rock r = it.next();
            if (!r.shadow.isAlive()) {
                it.remove();
                continue;
            }
            int age = tick - r.birthTick;
            driveRock(level, session, r, age);
            if (age >= IMPACT_AT) {
                it.remove();
            }
        }

        if (wavesSpawned >= waves && rocks.isEmpty() && tick >= (waves - 1) * WAVE_INTERVAL + WAVE_INTERVAL) {
            done = true;
        }
        tick++;
    }

    private void spawnWave(ServerLevel level, BossBattleSession session) {
        int triggerY = session.getTriggerPos().getY();
        for (ServerPlayer p : session.aliveParticipants(level)) {
            BlockPos pc = p.blockPosition();
            for (BlockPos off : RING) {
                int x = pc.getX() + off.getX();
                int z = pc.getZ() + off.getZ();
                BlockPos surface = CsBossSurfaceScanner.findSurfaceColumn(level, x, z, triggerY,
                        CsBossSurfaceScanner.DEFAULT_Y_TOLERANCE);
                double y = surface != null ? surface.getY() + 1.0 : p.getY();
                AttackShadowEntity shadow = AttackShadowEntity.spawn(level, x + 0.5, y, z + 0.5, session.getId());
                session.trackAttackEntity(shadow);
                // Décalage par anneau : les cases centrales tombent d'abord, puis cercle par cercle.
                int ringDist = (int) Math.round(Math.sqrt((double) off.getX() * off.getX() + (double) off.getZ() * off.getZ()));
                rocks.add(new Rock(shadow, tick + ringDist * RING_STAGGER));
            }
        }
        CsBossAttackLib.sound(level, session.getArenaCenter().x, session.getArenaCenter().y,
                session.getArenaCenter().z, "cobblemon:move.rockthrow.target",
                net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 1.0F);
    }

    private void driveRock(ServerLevel level, BossBattleSession session, Rock r, int age) {
        if (age < METEOR_AT) {
            // Poussière colorée tombant du ciel : télégraphe au‑dessus de l'ombre.
            CsBossAttackLib.meteorTelegraph(level, r.shadow.getX(), r.shadow.getY(), r.shadow.getZ(),
                    CsBossAttackLib.METEOR_DUST);
        }
        if (age == METEOR_AT) {
            AttackMeteoriteEntity meteor = AttackMeteoriteEntity.spawn(level,
                    r.shadow.getX(), r.shadow.getY() + FALL_HEIGHT, r.shadow.getZ(), session.getId(), false);
            session.trackAttackEntity(meteor);
            r.meteor = meteor;
            CsBossAttackLib.sound(level, r.shadow.getX(), r.shadow.getY(), r.shadow.getZ(),
                    "cobblemon:move.rockthrow.actor", net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 1.0F);
        } else if (age > METEOR_AT && age <= IMPACT_AT) {
            if (r.meteor != null && r.meteor.isAlive()) {
                double prevProgress = Math.min(1.0, (age - 1 - METEOR_AT) / (double) FALL_TICKS);
                double progress = Math.min(1.0, (age - METEOR_AT) / (double) FALL_TICKS);
                double prevY = r.shadow.getY() + FALL_HEIGHT * (1.0 - prevProgress);
                double newY = r.shadow.getY() + FALL_HEIGHT * (1.0 - progress);
                r.meteor.setPos(r.shadow.getX(), newY, r.shadow.getZ());
                if (!r.damaged && CsBossAttackLib.meteorSweepHit(level, session, r.meteor, prevY, newY, METEOR_DAMAGE)) {
                    r.damaged = true;
                }
            }
            if (age >= IMPACT_AT) {
                impact(level, r);
            }
        }
    }

    private void impact(ServerLevel level, Rock r) {
        BlockPos pos = BlockPos.containing(r.shadow.getX(), r.shadow.getY(), r.shadow.getZ());
        if (r.meteor != null && r.meteor.isAlive()) {
            r.meteor.discard();
        }
        r.shadow.discard();
        if (level.getBlockState(pos).canBeReplaced()) {
            level.setBlockAndUpdate(pos, ModBlocks.METEORITE.defaultBlockState());
        }
        CsBossAttackLib.sound(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                "cobblemon:impact.rock", net.minecraft.sounds.SoundSource.HOSTILE, 1.3F, 1.0F);
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
