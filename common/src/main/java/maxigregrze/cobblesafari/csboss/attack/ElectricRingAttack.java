package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * {@code base_electric_3} (plan 109) : autour de <b>chaque joueur</b>, un anneau (rayon 3) de champs
 * {@code csboss_electricity}. Une nouvelle vague 7 s plus tard (1 s après la disparition des blocs).
 * 3‑5 vagues au total. Les blocs gèrent eux‑mêmes leur cycle (charge/actif/disparition, plan 108).
 */
public class ElectricRingAttack implements CsBossAttack {

    private static final int RING_RADIUS = 3;
    private static final int WAVE_INTERVAL = 140; // 7 s
    private static final int NOMINAL_WAVES = 4;   // ±25 % ⇒ 3‑5
    private static final List<BlockPos> RING = CsBossGridShapes.filledCircle(RING_RADIUS);

    private final String id;
    private final net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create();
    private int waves;
    private int tick;
    private int wavesSpawned;
    private boolean done;

    public ElectricRingAttack(String id) {
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
        this.waves = CsBossAttackLib.varyOccurrences(NOMINAL_WAVES, rng);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (wavesSpawned < waves && tick == wavesSpawned * WAVE_INTERVAL) {
            placeRings(level, session, boss);
            wavesSpawned++;
        }
        if (tick >= (waves - 1) * WAVE_INTERVAL + WAVE_INTERVAL) {
            done = true;
        }
        tick++;
    }

    private void placeRings(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        int triggerY = session.getTriggerPos().getY();
        BlockState field = ModBlocks.CSBOSS_ELECTRICITY.defaultBlockState();
        for (ServerPlayer p : session.aliveParticipants(level)) {
            BlockPos pc = p.blockPosition();
            for (BlockPos off : RING) {
                BlockPos surface = CsBossSurfaceScanner.findSurfaceColumn(level,
                        pc.getX() + off.getX(), pc.getZ() + off.getZ(), triggerY, CsBossSurfaceScanner.DEFAULT_Y_TOLERANCE);
                if (surface != null) {
                    CsBossSurfaceScanner.placeOnSurface(level, surface, field);
                }
            }
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.thundershock.actor", SoundSource.HOSTILE, 1.4F, 0.9F);
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
