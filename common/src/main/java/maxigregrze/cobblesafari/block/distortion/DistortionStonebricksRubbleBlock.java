package maxigregrze.cobblesafari.block.distortion;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.FaceAttachedModelBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;

/**
 * Stonebricks rubble: same orthonormal shapes as {@link DistortionWeedBlock}, but
 * floor/ceiling placement faces away from the player ({@link FloorFacing#OPPOSITE}).
 */
public class DistortionStonebricksRubbleBlock extends FaceAttachedModelBlock {

    public static final MapCodec<DistortionStonebricksRubbleBlock> CODEC = simpleCodec(DistortionStonebricksRubbleBlock::new);

    public DistortionStonebricksRubbleBlock(Properties properties) {
        super(properties, DistortionWeedBlock.settings(true, FloorFacing.OPPOSITE));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
