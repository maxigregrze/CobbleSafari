package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDistortionFlowerEntity;
import maxigregrze.cobblesafari.underground.logic.PerlinNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * {@code distortion_3} (Type B): selects ~10% of the arena surface via Perlin noise
 * (same method as {@code base_electric_2}) and spawns distortion flowers there,
 * which grow into stem walls (mechanic of {@code distortion_2}).
 */
public class DistortionFieldAttack implements CsBossAttack {

    private static final int DURATION = 240; // ≈12 s
    private static final int WAVE2_AT = 120; // 2nd flower wave at 6 s
    private static final double PERLIN_SCALE = 0.2;
    // Perlin (normalized) peaks around ~0.85; lower threshold ⇒ more coverage (~30 %).
    private static final double COVERAGE_THRESHOLD = 0.6;

    private final String id;
    private final net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create();
    private int tick;
    private boolean done;

    public DistortionFieldAttack(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public AttackCategory category() {
        return AttackCategory.AREA;
    }

    @Override
    public void begin(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        this.tick = 0;
        this.done = false;
        placeFlowers(level, session, boss);
    }

    @Override
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (tick == WAVE2_AT) {
            placeFlowers(level, session, boss);
        }
        if (++tick >= DURATION) {
            done = true;
        }
    }

    private void placeFlowers(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        List<BlockPos> surfaces = CsBossSurfaceScanner.scanSurface(level, session);
        PerlinNoise noise = new PerlinNoise(level.getRandom().nextLong());
        for (BlockPos surface : surfaces) {
            double n = noise.noise2dNormalized(surface.getX() * PERLIN_SCALE, surface.getZ() * PERLIN_SCALE);
            if (n > COVERAGE_THRESHOLD) {
                AttackDistortionFlowerEntity flower = AttackDistortionFlowerEntity.spawn(level,
                        surface.getX() + 0.5, surface.getY() + 1.0, surface.getZ() + 0.5, session.getId());
                session.trackAttackEntity(flower);
            }
        }
        boss.triggerAttackAnimation();
        CsBossAttackLib.sound(level, boss.getX(), boss.getY(), boss.getZ(),
                "cobblemon:move.shadowball.actor", SoundSource.HOSTILE, 1.4F, 0.7F);
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
