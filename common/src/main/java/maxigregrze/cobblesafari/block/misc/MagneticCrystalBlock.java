package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.HangingDoubleModelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** Two‑cell magnetic crystal (slim column). All behaviour lives in {@link HangingDoubleModelBlock}. */
public class MagneticCrystalBlock extends HangingDoubleModelBlock {

    public static final MapCodec<MagneticCrystalBlock> CODEC = simpleCodec(MagneticCrystalBlock::new);

    public MagneticCrystalBlock(Properties properties) {
        super(properties,
                Block.box(3, 0, 3, 13, 16, 13),   // floor lower
                Block.box(3, 0, 3, 13, 14, 13),   // floor upper
                Block.box(3, 2, 3, 13, 16, 13),   // ceiling lower
                Block.box(3, -14, 3, 13, 16, 13)); // ceiling upper
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
