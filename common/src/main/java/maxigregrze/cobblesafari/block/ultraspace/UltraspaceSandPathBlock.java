package maxigregrze.cobblesafari.block.ultraspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.HoeConversion;
import maxigregrze.cobblesafari.block.base.ShapedBlock;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Strange Sand Path: a pixel shorter than a full cube like the vanilla dirt path, but with full
 * collision so nothing sinks into it. No gravity - a falling block one pixel short would land as a
 * full cube. Second step of the hoe cascade; drops Strange Sand, not itself.
 */
public class UltraspaceSandPathBlock extends ShapedBlock {

    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    public UltraspaceSandPathBlock(Properties properties) {
        super(properties, SHAPE, Shapes.block());
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(UltraspaceSandPathBlock::new);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return HoeConversion.tryConvert(stack, level, pos, player, hand, ModBlocks.ULTRASPACE_SAND_PATTERN);
    }
}
