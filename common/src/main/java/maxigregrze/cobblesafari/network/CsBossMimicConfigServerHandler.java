package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlock;
import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server validation for the mimic GUI: creative + proximity, never trust the
 * client. Validates the mimic block id and stores the {@code reverse} option on the blockstate.
 */
public final class CsBossMimicConfigServerHandler {

    private static final double MAX_DISTANCE = 8.0D;

    private CsBossMimicConfigServerHandler() {}

    public static void handleSave(ServerPlayer player, SaveCsBossMimicConfigPayload payload) {
        if (!player.isCreative()) {
            return;
        }
        if (!player.blockPosition().closerThan(payload.pos(), MAX_DISTANCE)) {
            return;
        }
        if (!(player.level().getBlockEntity(payload.pos()) instanceof CsBossMimicBlockEntity be)) {
            return;
        }

        String id = payload.mimicBlockId().trim();
        if (id.isEmpty()) {
            be.setMimicBlockId("");
        } else {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc != null && BuiltInRegistries.BLOCK.containsKey(loc)) {
                be.setMimicBlockId(id);
            } else {
                be.setMimicBlockId("");
                player.sendSystemMessage(Component.translatable("cobblesafari.csboss.mimic.bad_block"));
            }
        }

        BlockState state = player.level().getBlockState(payload.pos());
        if (state.getBlock() instanceof CsBossMimicBlock
                && state.getValue(CsBossMimicBlock.REVERSE) != payload.reverse()) {
            player.level().setBlock(payload.pos(),
                    state.setValue(CsBossMimicBlock.REVERSE, payload.reverse()), Block.UPDATE_ALL);
        }

        be.syncToClients();
    }
}
