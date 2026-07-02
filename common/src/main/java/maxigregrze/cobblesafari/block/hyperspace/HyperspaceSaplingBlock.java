package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hyperspace sapling. Behaves like a vanilla {@link SaplingBlock} (planted on dirt, custom model)
 * except it <em>never</em> grows on its own — natural random‑tick growth is disabled — and only
 * grows <em>rarely</em> when bonemealed. Both the plain and flowering saplings grow the same
 * {@code cobblesafari:hyperspace_tree} via their shared {@link TreeGrower}.
 */
public class HyperspaceSaplingBlock extends SaplingBlock {

    /** Chance per successful bonemeal interaction to advance the sapling (kept low: rare growth). */
    private static final float BONEMEAL_SUCCESS_CHANCE = 0.10F;

    /** Bushy outline matching the vanilla azalea: full upper half plus a thin central stem below. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(6.0, 0.0, 6.0, 10.0, 8.0, 10.0));

    private final TreeGrower grower;

    public HyperspaceSaplingBlock(TreeGrower grower, Properties properties) {
        super(grower, properties);
        this.grower = grower;
    }

    @Override
    public MapCodec<? extends SaplingBlock> codec() {
        return simpleCodec(props -> new HyperspaceSaplingBlock(this.grower, props));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // No natural growth: Hyperspace saplings only advance when bonemealed.
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return level.random.nextFloat() < BONEMEAL_SUCCESS_CHANCE;
    }
}
