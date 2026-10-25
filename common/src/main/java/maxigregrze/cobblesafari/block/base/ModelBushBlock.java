package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A foliage plant with a custom hitbox that, like flowers and saplings, cannot float and breaks
 * when its support is removed. The shears-only drop is handled by the loot table.
 *
 * <p>{@code groundTag} says what it may be planted on. Left {@code null}, the vanilla
 * {@link BushBlock} rule applies ({@code #minecraft:dirt} plus farmland) - that is the Hyperspace
 * bush. Given a tag, only blocks in that tag are accepted, which is what lets the Ultra Space bush
 * grow on strange sand instead of dirt.</p>
 */
public class ModelBushBlock extends BushBlock {

    private final VoxelShape shape;
    @Nullable
    private final TagKey<Block> groundTag;

    public ModelBushBlock(Properties properties, VoxelShape shape) {
        this(properties, shape, null);
    }

    public ModelBushBlock(Properties properties, VoxelShape shape, @Nullable TagKey<Block> groundTag) {
        super(properties);
        this.shape = shape;
        this.groundTag = groundTag;
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return simpleCodec(props -> new ModelBushBlock(props, this.shape, this.groundTag));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return groundTag == null ? super.mayPlaceOn(state, level, pos) : state.is(groundTag);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    /**
     * A bush holds nothing up and offers no solid face, even when its hitbox fills the cell (the cube
     * bush): walls, bars and panes skip it the way vanilla skips leaves.
     */
    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}
