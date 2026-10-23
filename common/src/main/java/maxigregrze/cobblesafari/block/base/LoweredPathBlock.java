package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A path: a pixel shorter than a full cube like the vanilla dirt path, but with full collision so
 * nothing sinks into it. Shared by the Strange Sand Path and the Megapolis road.
 */
public class LoweredPathBlock extends ShapedBlock {

    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    public LoweredPathBlock(Properties properties) {
        super(properties, SHAPE, Shapes.block());
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(LoweredPathBlock::new);
    }

    /**
     * The full collision would otherwise give a 0.2 shade brightness, which ambient occlusion reads
     * both for this block's own lowered top face and for the edges of its neighbours.
     */
    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
