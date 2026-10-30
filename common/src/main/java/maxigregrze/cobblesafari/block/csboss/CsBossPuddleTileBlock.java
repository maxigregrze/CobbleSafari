package maxigregrze.cobblesafari.block.csboss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Flaque de Suicune ({@code jump_2}, retours plan 199) : film d'eau teintée (couleur d'eau du biome).
 * Alerte = remous et éclaboussures ; actif = geyser d'eau qui jaillit. L'impulsion et les dégâts
 * sont appliqués par l'attaque ({@code CsBossTileField}).
 */
public class CsBossPuddleTileBlock extends CsBossTileBlock {

    public static final int WARN_TICKS = 30;
    public static final int ACTIVE_TICKS = 20;

    public CsBossPuddleTileBlock(Properties properties) {
        super(properties, WARN_TICKS, ACTIVE_TICKS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.15 + random.nextDouble() * 0.7;
        double y = pos.getY() + 0.12;
        double z = pos.getZ() + 0.15 + random.nextDouble() * 0.7;
        if (!state.getValue(ACTIVE)) {
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SPLASH, x, y, z, 0.0, 0.05, 0.0);
            }
            if (random.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0.0, 0.02, 0.0);
            }
        } else {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.SPLASH, x, y + random.nextDouble() * 1.5, z,
                        (random.nextDouble() - 0.5) * 0.2, 0.6 + random.nextDouble() * 0.4,
                        (random.nextDouble() - 0.5) * 0.2);
            }
            level.addParticle(ParticleTypes.CLOUD, x, y + 0.3, z, 0.0, 0.25 + random.nextDouble() * 0.2, 0.0);
            if (random.nextInt(25) == 0) {
                level.playLocalSound(x, y, z, SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE,
                        0.4F, 0.9F + random.nextFloat() * 0.3F, false);
            }
        }
    }
}
