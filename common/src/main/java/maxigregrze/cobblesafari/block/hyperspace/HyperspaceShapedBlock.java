package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Non-orientable block with a fixed custom {@link VoxelShape} used for both selection and collision.
 * Reused by road, cone, scaffolding platform/scaffolding (full cube), floating platform, slab.
 */
public class HyperspaceShapedBlock extends Block {

    private final VoxelShape shape;

    public HyperspaceShapedBlock(Properties properties, VoxelShape shape) {
        super(properties);
        this.shape = shape;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new HyperspaceShapedBlock(props, this.shape));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
