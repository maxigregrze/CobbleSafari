package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Slab for the Hyperspace and Kalos roads. Their models sit 1px lower than vanilla so the surface
 * lines up with the road block (top at y=15); the selection and collision boxes follow that surface.
 *
 * <p>This also keeps the double slab off a full-cube collision, which would give it a 0.2 shade
 * brightness and darken every road face around it under ambient occlusion. The support shape stays
 * vanilla, so whatever could be placed on a road slab still can. Like the vanilla dirt path, the
 * double slab still suffocates and blocks the view despite its one-pixel-short collision.</p>
 */
public class RoadSlabBlock extends SlabBlock {

    public static final MapCodec<RoadSlabBlock> CODEC = simpleCodec(RoadSlabBlock::new);

    private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 7, 16);
    private static final VoxelShape TOP = Block.box(0, 7, 0, 16, 15, 16);
    private static final VoxelShape DOUBLE = Block.box(0, 0, 0, 16, 15, 16);

    public RoadSlabBlock(Properties properties) {
        super(properties.isSuffocating(RoadSlabBlock::isDouble).isViewBlocking(RoadSlabBlock::isDouble));
    }

    private static boolean isDouble(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(TYPE) == SlabType.DOUBLE;
    }

    @Override
    public MapCodec<? extends SlabBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(TYPE)) {
            case BOTTOM -> BOTTOM;
            case TOP -> TOP;
            case DOUBLE -> DOUBLE;
        };
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return super.getShape(state, level, pos, CollisionContext.empty());
    }
}
