package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hyperspace bush: a foliage-tinted plant that, like flowers and saplings, must sit on dirt or
 * grass — it cannot float and breaks if its support is removed. Custom hitbox via {@code shape};
 * the shears-only drop is handled by the loot table.
 */
public class HyperspaceBushBlock extends BushBlock {

    private final VoxelShape shape;

    public HyperspaceBushBlock(Properties properties, VoxelShape shape) {
        super(properties);
        this.shape = shape;
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return simpleCodec(props -> new HyperspaceBushBlock(props, this.shape));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
