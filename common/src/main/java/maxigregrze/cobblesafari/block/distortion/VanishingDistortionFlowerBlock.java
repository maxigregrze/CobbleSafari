package maxigregrze.cobblesafari.block.distortion;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;

public class VanishingDistortionFlowerBlock extends AbstractReactiveDistortionFlowerBlock {

    public static final MapCodec<VanishingDistortionFlowerBlock> CODEC = simpleCodec(VanishingDistortionFlowerBlock::new);

    public VanishingDistortionFlowerBlock(Properties properties) {
        super(properties, true, false, false, false);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}

