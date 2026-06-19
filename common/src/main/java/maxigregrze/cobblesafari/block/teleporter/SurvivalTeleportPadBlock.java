package maxigregrze.cobblesafari.block.teleporter;

import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.teleporter.TeleportPadManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Craftable teleport pad variant. Configured with a Tinkhammer (shift-right-click opens GUI,
 * right-click cycles mode and re-pairs). Empty-hand interaction is disabled.
 */
public class SurvivalTeleportPadBlock extends TeleportPadBlock {

    public SurvivalTeleportPadBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return tinkhammerInteract(stack, state, level, pos, player, hit);
    }

    /**
     * Shared Tinkhammer logic — invoked from the block and from {@link maxigregrze.cobblesafari.item.TinkhammerItem}
     * so sneaking with a tool still reaches the handler when vanilla skips block activation.
     */
    public static ItemInteractionResult tinkhammerInteract(ItemStack stack, BlockState state, Level level,
                                                           BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!stack.is(ModItems.TINKHAMMER)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof TeleportPadBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isShiftKeyDown()) {
            Services.PLATFORM.sendPayloadToPlayer(sp, be.createOpenPayload());
        } else {
            TeleportPadMode next = state.getValue(MODE).next();
            level.setBlock(pos, state.setValue(MODE, next), Block.UPDATE_ALL);
            TeleportPadManager.breakLink(level, pos);
            if (level instanceof ServerLevel serverLevel) {
                TeleportPadManager.tryAutoPair(serverLevel, pos);
            }
            player.displayClientMessage(
                    Component.translatable("cobblesafari.teleport_pad.mode_changed",
                            Component.translatable("gui.cobblesafari.teleport_pad.mode." + next.getSerializedName())),
                    true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
