package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;

public class AppearingDistortionFlowerBlock extends AbstractReactiveDistortionFlowerBlock {

    public static final MapCodec<AppearingDistortionFlowerBlock> CODEC = simpleCodec(AppearingDistortionFlowerBlock::new);

    public AppearingDistortionFlowerBlock(Properties properties) {
        super(properties, false, true, true, true);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}

