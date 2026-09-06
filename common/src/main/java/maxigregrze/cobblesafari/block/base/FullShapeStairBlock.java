package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A {@link StairBlock} that keeps stair <em>connection</em> semantics - the shape property, the way
 * it corners with its neighbours - but presents a full cube for selection and collision.
 *
 * <p>For a decorative run whose model already fills its cell, vanilla's stepped box only makes the
 * block fiddly to walk on and to click, and buys nothing: there is no step to climb.</p>
 */
public class FullShapeStairBlock extends StairBlock {

    private final BlockState baseState;

    public FullShapeStairBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
        this.baseState = baseState;
    }

    @Override
    public MapCodec<? extends StairBlock> codec() {
        return simpleCodec(props -> new FullShapeStairBlock(this.baseState, props));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }
}
