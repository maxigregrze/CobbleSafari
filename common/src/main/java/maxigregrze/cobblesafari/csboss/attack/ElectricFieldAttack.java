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
 * {@code base_electric_2} (plan 108 § 4.2) : deux vagues (à 6 s d'intervalle) de champs
 * {@code csboss_electricity} posés sur ~50 % de la surface de l'arène via bruit de Perlin. Chaque
 * bloc gère son cycle (charge 3 s → actif 3 s → disparition, foudre au contact). L'attaque se
 * termine 8 s après la seconde vague.
 */
public class ElectricFieldAttack implements CsBossAttack {

    private static final int WAVE2_AT = 120;       // 2e vague à 6 s
    private static final int DONE = WAVE2_AT + 160; // 8 s après la 2e vague
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
