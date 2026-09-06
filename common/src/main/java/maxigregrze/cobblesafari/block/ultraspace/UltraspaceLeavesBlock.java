package maxigregrze.cobblesafari.block.ultraspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Strange Leaves: a carpet of foliage that refuses to pile up.
 *
 * <p>Vanilla {@link CarpetBlock} survives on anything that is not air, which lets carpets stack
 * into a column. A drift of leaves is meant to lie <em>on</em> something, one layer deep, so the
 * one thing this subclass adds is that a leaves block underneath is not valid ground. Everything
 * else - the one-pixel shape, the support requirement, the break when that support goes, the
 * shears-only drop - is inherited unchanged.</p>
 */
public class UltraspaceLeavesBlock extends CarpetBlock {

    public static final MapCodec<UltraspaceLeavesBlock> CODEC = simpleCodec(UltraspaceLeavesBlock::new);

    public UltraspaceLeavesBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CarpetBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && !level.getBlockState(pos.below()).is(ModBlocks.ULTRASPACE_LEAVES);
    }
}
