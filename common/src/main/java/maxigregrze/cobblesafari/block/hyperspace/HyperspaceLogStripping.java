package maxigregrze.cobblesafari.block.hyperspace;

import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Axe-stripping for the Hyperspace log. The log is a six-way {@link DirectionalBlock} (it carries
 * {@code FACING}, not the vanilla {@code AXIS}), so it cannot be registered through the loaders'
 * standard strippable APIs — both expect an {@code axis} pillar. This helper reproduces the vanilla
 * strip behaviour (swap block, preserve orientation, damage the axe, play the sound) and is invoked
 * from each loader's right-click/use-block hook.
 */
public final class HyperspaceLogStripping {

    private HyperspaceLogStripping() {
    }

    /**
     * Attempts to strip a Hyperspace log at {@code pos}. Returns {@link InteractionResult#PASS} when
     * the interaction does not apply (wrong item or block) so the caller leaves other handling intact.
     */
    public static InteractionResult tryStrip(Player player, Level level, InteractionHand hand, BlockPos pos) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (!(heldStack.getItem() instanceof AxeItem)) {
            return InteractionResult.PASS;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.HYPERSPACE_LOG)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            BlockState stripped = ModBlocks.HYPERSPACE_LOG_STRIPPED.defaultBlockState()
                    .setValue(DirectionalBlock.FACING, state.getValue(DirectionalBlock.FACING));
            level.setBlock(pos, stripped, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, stripped));
            level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
            heldStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
