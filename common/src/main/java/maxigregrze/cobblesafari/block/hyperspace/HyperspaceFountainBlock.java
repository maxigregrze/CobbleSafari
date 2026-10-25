package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.ShapedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Kalosian Fountain (plan 194 - 11 quater): a stone basin around a column whose bowl spills water.
 * Purely decorative; the water is drawn with the vanilla still / flowing water sprites, tinted by the
 * biome's water colour, and murmurs like running water.
 *
 * <p>The basin is wider than the cell (36px across); its rim and floor still collide, which vanilla
 * supports for shapes reaching up to a block outside their cell.</p>
 */
public class HyperspaceFountainBlock extends ShapedBlock {

    /** The solid cuboids of the model: basin rim and floor, column, bowl. Water planes are left out. */
    public static final VoxelShape SHAPE = Shapes.or(
            Block.box(-10, 0, -8, -6, 4, 24), Block.box(-8, 0, 22, 24, 4, 26),
            Block.box(22, 0, -8, 26, 4, 24), Block.box(-8, 0, -10, 24, 4, -6),
            Block.box(-8, 0, -8, 24, 0.5, 24),
            Block.box(5, 0, 5, 11, 1, 11), Block.box(6, 1, 6, 10, 8, 10),
            Block.box(6.5, 8, 6.5, 9.5, 10, 9.5), Block.box(6, 10, 6, 10, 12, 10),
            Block.box(1, 12, 1, 15, 14, 15), Block.box(6, 14, 6, 10, 16, 10),
            Block.box(0, 12, 0, 16, 16, 1), Block.box(0, 12, 15, 16, 16, 16),
            Block.box(0, 12, 1, 1, 16, 15), Block.box(15, 12, 1, 16, 16, 15)).optimize();

    /** One murmur every 24 ticks on average: a fountain never stops, unlike a stray flowing block. */
    private static final int SOUND_CHANCE = 24;

    public HyperspaceFountainBlock(Properties properties) {
        super(properties, SHAPE);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(HyperspaceFountainBlock::new);
    }

    /** The running-water ambience of vanilla flowing water ({@code WaterFluid.animateTick}). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(SOUND_CHANCE) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.WATER_AMBIENT,
                    SoundSource.BLOCKS, random.nextFloat() * 0.25F + 0.75F, random.nextFloat() + 0.5F, false);
        }
    }
}
