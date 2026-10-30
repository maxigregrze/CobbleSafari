package maxigregrze.cobblesafari.block.csboss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sol brûlant ({@code base_fire_2/3}, traînée d'Entei) : alerte = texture {@code attack_scorched}
 * animée + « grésillement de lave » (fumée + extinction) ; actif = texture magma + flammes / lave.
 * Jamais de vrai feu : le toit du Bell Tower est en bois.
 */
public class CsBossScorchedTileBlock extends CsBossTileBlock {

    public static final int WARN_TICKS = 30;
    public static final int ACTIVE_TICKS = 30;

    public CsBossScorchedTileBlock(Properties properties) {
        super(properties, WARN_TICKS, ACTIVE_TICKS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + 0.15;
        double z = pos.getZ() + random.nextDouble();
        if (!state.getValue(ACTIVE)) {
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.03, 0.0);
            }
            if (random.nextInt(40) == 0) {
                level.playLocalSound(x, y, z, SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE,
                        0.25F, 2.6F + random.nextFloat() * 0.8F, false);
            }
        } else {
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.04, 0.0);
            }
            if (random.nextInt(14) == 0) {
                level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
            }
        }
    }
}
