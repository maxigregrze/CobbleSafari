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
 * client. Validates both block ids and stores the mode on the blockstate.
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

        be.setMimicBlockId(validId(player, payload.mimicBlockId()));
        be.setBattleMimicBlockId(validId(player, payload.battleMimicBlockId()));

        BlockState state = player.level().getBlockState(payload.pos());
        if (state.getBlock() instanceof CsBossMimicBlock) {
            BlockState next = CsBossMimicBlock.Mode.byIndex(payload.mode()).apply(state);
            if (next != state) {
                state = next;
                player.level().setBlock(payload.pos(), state, Block.UPDATE_ALL);
            }
        }

        // Forced: the target ids and the mode change what the neighbourhood sees us as, so the
        // disguise has to be re-broadcast even when the resolved state lands on the same value.
        CsBossMimicBlock.refresh(player.level(), payload.pos(), state, true);
    }

    /** The trimmed id if it names a registered block, {@code ""} otherwise (with a message if non-blank). */
    private static String validId(ServerPlayer player, String raw) {
        String id = raw.trim();
        if (id.isEmpty()) {
            return "";
        }
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc != null && BuiltInRegistries.BLOCK.containsKey(loc)) {
            return id;
        }
        player.sendSystemMessage(Component.translatable("cobblesafari.csboss.mimic.bad_block"));
        return "";
    }
}
