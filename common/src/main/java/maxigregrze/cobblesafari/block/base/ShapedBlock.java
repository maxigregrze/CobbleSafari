package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Non-orientable block with a fixed custom {@link VoxelShape}. Reused by the Hyperspace and Kalos
 * road, cone, slab and crate, and by the Ultra Space sand path.
 *
 * <p>The collision box defaults to the selection box, which is what a decorative block wants. The
 * second constructor separates them for the {@code dirt_path} case: a selection box one pixel
 * shorter than the cell, but a full cube of collision so nothing sinks into it.</p>
 */
public class ShapedBlock extends Block {

    private final VoxelShape shape;
    private final VoxelShape collisionShape;

    public ShapedBlock(Properties properties, VoxelShape shape) {
        this(properties, shape, shape);
    }

    public ShapedBlock(Properties properties, VoxelShape shape, VoxelShape collisionShape) {
        super(properties);
        this.shape = shape;
        this.collisionShape = collisionShape;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new ShapedBlock(props, this.shape, this.collisionShape));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisionShape;
    }
}
