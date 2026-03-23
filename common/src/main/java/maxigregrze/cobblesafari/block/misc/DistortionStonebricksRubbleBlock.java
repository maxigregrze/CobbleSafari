package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class DistortionStonebricksRubbleBlock extends DistortionWeedBlock {
    public static final MapCodec<DistortionStonebricksRubbleBlock> CODEC = simpleCodec(DistortionStonebricksRubbleBlock::new);

    public DistortionStonebricksRubbleBlock(Properties properties) {
        super(properties, true);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
