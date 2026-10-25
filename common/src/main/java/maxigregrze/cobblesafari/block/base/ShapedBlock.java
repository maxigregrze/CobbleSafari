package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

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
    @Nullable
    private final VoxelShape supportShape;

    public ShapedBlock(Properties properties, VoxelShape shape) {
        this(properties, shape, shape);
    }

    public ShapedBlock(Properties properties, VoxelShape shape, VoxelShape collisionShape) {
        this(properties, shape, collisionShape, null);
    }

    /**
     * {@code supportShape} replaces the collision box in "is this face solid" tests (walls, bars,
     * torches, lanterns...). A piece of furniture with a full hitbox but no solid sides passes
     * {@link BlockShapeUtils#CAPPED_SUPPORT}: things still rest on it, walls and panes no longer join it.
     */
    public ShapedBlock(Properties properties, VoxelShape shape, VoxelShape collisionShape,
                       @Nullable VoxelShape supportShape) {
        super(properties);
        this.shape = shape;
        this.collisionShape = collisionShape;
        this.supportShape = supportShape;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new ShapedBlock(props, this.shape, this.collisionShape, this.supportShape));
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return supportShape != null ? supportShape : super.getBlockSupportShape(state, level, pos);
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
