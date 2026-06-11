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
 * {@code base_electric_3} (plan 109): around <b>each player</b>, a ring (radius 3) of
 * {@code csboss_electricity} fields. A new wave 7 s later (1 s after blocks disappear).
 * 3–5 waves total. Blocks manage their own cycle (charge/active/disappear, plan 108).
 */
public class ElectricRingAttack implements CsBossAttack {

    private static final int RING_RADIUS = 3;
    private static final int WAVE_INTERVAL = 120; // 6 s (rings at 0 s and 6 s)
    private static final int WAVES = 2;           // deterministic (2*120 = 240)
    private static final List<BlockPos> RING = CsBossGridShapes.filledCircle(RING_RADIUS);

    private final String id;
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
        this.waves = WAVES;
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
