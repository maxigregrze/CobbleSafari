package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hollow "tube" scaffolding. The outline / selection shape is a <em>full cube</em> (so the
 * block is easy to target and place against), while the collision shape is a hollow tube —
 * a full cube minus a 14x16x14 vertical hole — letting the player drop straight through from
 * top to bottom.
 */
public class HyperspaceScaffoldTubeBlock extends Block {

    private static final VoxelShape COLLISION = Shapes.join(
            Shapes.block(), Block.box(1, 0, 1, 15, 16, 15), BooleanOp.ONLY_FIRST);

    public HyperspaceScaffoldTubeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(HyperspaceScaffoldTubeBlock::new);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }
}
