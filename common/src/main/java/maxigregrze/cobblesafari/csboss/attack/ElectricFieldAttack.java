package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.underground.logic.PerlinNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * {@code base_electric_2}: two waves (6 s apart) of
 * {@code csboss_electricity} fields placed on ~50% of the arena surface via Perlin noise. Each
 * block manages its cycle (charge 3 s → active 3 s → disappear, lightning on contact). The attack
 * ends 8 s after the second wave.
 */
public class ElectricFieldAttack implements CsBossAttack {

    private static final int WAVE2_AT = 120; // 2nd wave at 6 s
    private static final int DONE = 240; // ≈12 s total (waves at 0 s and 6 s)
    private static final double PERLIN_SCALE = 0.2;
    private static final double COVERAGE_THRESHOLD = 0.5; // ~50 %

    private final String id;
    private int tick;
    private boolean done;

    public ElectricFieldAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.done = false;
        placeFields(level, session, boss);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (tick == WAVE2_AT) {
            placeFields(level, session, boss);
        }
        if (++tick >= DONE) {
            done = true;
        }
    }

    private void placeFields(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        List<BlockPos> surfaces = CsBossSurfaceScanner.scanSurface(level, session);
        PerlinNoise noise = new PerlinNoise(level.getRandom().nextLong());
        BlockState field = ModBlocks.CSBOSS_ELECTRICITY.defaultBlockState();
        for (BlockPos surface : surfaces) {
            double n = noise.noise2dNormalized(surface.getX() * PERLIN_SCALE, surface.getZ() * PERLIN_SCALE);
            if (n > COVERAGE_THRESHOLD) {
                CsBossSurfaceScanner.placeOnSurface(level, surface, field);
            }
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.thundershock.actor", SoundSource.HOSTILE, 1.4F, 0.9F);
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.AREA;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
