package maxigregrze.cobblesafari.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Shared break rule for two-tall blocks whose loot table only drops from {@code half=lower}.
 *
 * <p>Vanilla removes the mined cell itself <em>after</em> {@code playerWillDestroy} and only runs
 * its loot if that removal succeeds, so the mined cell must never be cleared here. When the upper
 * half is mined in survival, the lower half is destroyed <b>with</b> its drop (the pattern of
 * {@code PokeStatueBlock}); otherwise the other half is cleared without drops.</p>
 */
public final class MultiCellBreak {

    private MultiCellBreak() {}

    public static void breakOtherHalf(Level level, BlockPos pos, BlockState state, Player player, Block self) {
        if (level.isClientSide()) {
            return;
        }
        DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        BlockPos other = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        if (!level.getBlockState(other).is(self)) {
            return;
        }
        if (half == DoubleBlockHalf.UPPER && !player.isCreative()) {
            level.destroyBlock(other, true, player);
        } else {
            level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
    }
}
