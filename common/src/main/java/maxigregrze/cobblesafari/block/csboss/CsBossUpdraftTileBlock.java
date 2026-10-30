package maxigregrze.cobblesafari.block.csboss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Courant ascendant ({@code base_flying_3}) : modèle de geyser au magma remplacé par du béton noir,
 * rochers re-texturés avec le bloc du sol (block entity + renderer).
 * Alerte = éclats du bloc sous la tuile qui sautillent (effet de séisme) ; actif = rafales de vent
 * violentes vers le haut. L'impulsion, la Nausée et les dégâts sont appliqués par l'attaque.
 */
public class CsBossUpdraftTileBlock extends CsBossTileBlock {

    public static final int WARN_TICKS = 40;
    public static final int ACTIVE_TICKS = 40;

    private static final SoundEvent WIND_BURST =
            SoundEvent.createVariableRangeEvent(ResourceLocation.parse("minecraft:entity.wind_charge.wind_burst"));

    public CsBossUpdraftTileBlock(Properties properties) {
        super(properties, WARN_TICKS, ACTIVE_TICKS);
    }

    /** Rendu par {@code CsBossUpdraftTileRenderer} : rochers re-texturés avec le bloc du dessous. */
    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CsBossUpdraftTileBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
        double y = pos.getY() + 0.1;
        double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
        if (!state.getValue(ACTIVE)) {
            BlockState below = maxigregrze.cobblesafari.block.base.ApparentState.at(level, pos.below());
            if (!below.isAir() && random.nextInt(2) == 0) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below), x, y, z,
                        0.0, 0.25 + random.nextDouble() * 0.15, 0.0);
            }
        } else {
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.6 + random.nextDouble() * 0.3, 0.0);
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SMALL_GUST, x, y + 0.5 + random.nextDouble(), z, 0.0, 0.0, 0.0);
            }
            if (random.nextInt(30) == 0) {
                level.playLocalSound(x, y, z, WIND_BURST, SoundSource.HOSTILE, 0.35F, 1.2F, false);
            }
        }
    }
}
