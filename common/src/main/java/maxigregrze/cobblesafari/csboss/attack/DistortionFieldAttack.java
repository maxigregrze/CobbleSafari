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
 * {@code distortion_3} (plan 113, Type B) : sélectionne ~10 % de la surface de l'arène par bruit de
 * Perlin (même méthode que {@code base_electric_2}) et y fait apparaître des fleurs de distorsion,
 * qui poussent en murs de tiges (mécanique de {@code distortion_2}).
 */
public class DistortionFieldAttack implements CsBossAttack {

    private static final int DURATION = 120;
    private static final double PERLIN_SCALE = 0.2;
    private static final double COVERAGE_THRESHOLD = 0.9; // ~10 %

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
    public void tick(ServerLevel level, BossBattleSession session, CsBossEntity boss) {
        if (done) {
            return;
        }
        if (++tick >= DURATION) {
            done = true;
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
