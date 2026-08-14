package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Small Hyperspace flag: a thin, dyeable adapter over the shared {@link HyperspaceDirectionalBlock}
 * (wall-mounted). The selection box covers the whole silhouette (wall arm + neon panel) while the
 * collision box is limited to the arm, so the hanging neon stays walk-through. All dye behaviour is
 * delegated to {@link HyperspaceFlagDye}; this class only wires the properties and the interaction
 * so the shared base stays un-dyeable for the barrier / railing / shutters that reuse it.
 */
public class HyperspaceFlagSmallBlock extends HyperspaceDirectionalBlock {

    private final VoxelShape shape;
    private final VoxelShape armShape;

    public HyperspaceFlagSmallBlock(Properties properties, VoxelShape shape, VoxelShape armShape) {
        super(properties, shape, true, true, armShape);
        this.shape = shape;
        this.armShape = armShape;
        registerDefaultState(HyperspaceFlagDye.undyed(defaultBlockState()));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new HyperspaceFlagSmallBlock(props, this.shape, this.armShape));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        HyperspaceFlagDye.appendProperties(builder);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return HyperspaceFlagDye.interact(stack, level, player, List.of(pos));
    }
}
