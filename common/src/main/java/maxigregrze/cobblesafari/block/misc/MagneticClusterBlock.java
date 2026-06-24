package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.HangingDoubleModelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** Two‑cell magnetic cluster (full‑width). All behaviour lives in {@link HangingDoubleModelBlock}. */
public class MagneticClusterBlock extends HangingDoubleModelBlock {

    public static final MapCodec<MagneticClusterBlock> CODEC = simpleCodec(MagneticClusterBlock::new);

    public MagneticClusterBlock(Properties properties) {
        super(properties,
                Block.box(0, 0, 0, 16, 16, 16),   // floor lower
                Block.box(0, 0, 0, 16, 8, 16),    // floor upper
                Block.box(0, 8, 0, 16, 16, 16),   // ceiling lower
                Block.box(0, -8, 0, 16, 16, 16)); // ceiling upper
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
