package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A covering: a carpet cut from another material, a quarter of a pixel thick (plan 194 - 10).
 * Keeps the carpet rule - survives as long as the block below is not air.
 */
public class CoveringBlock extends CarpetBlock {

    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 0.25, 16);

    public CoveringBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CarpetBlock> codec() {
        return simpleCodec(CoveringBlock::new);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
