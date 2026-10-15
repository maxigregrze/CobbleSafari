package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.block.pokestatue.AbstractPokeStatueBlockEntity;
import maxigregrze.cobblesafari.block.pokestatue.CreativeStatueBlock;
import maxigregrze.cobblesafari.block.pokestatue.PokeStatueBlock;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.manager.BannedItemsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Server validation + handling for both statue config GUIs.
 *
 * <p>The variant announced by the client is <b>ignored</b>: everything is derived from the block
 * actually present at the target position, so a modified client cannot borrow the creative
 * statue's looser rules for a survival-placed one.</p>
 *
 * <p>The species and animation strings are deliberately <b>not</b> validated. The server has no
 * client model, so it cannot know which species or animations resolve - and a resource pack can
 * change the answer per client anyway. Unknown values fall back at render time.</p>
 */
public final class PokeStatueConfigServerHandler {

    private static final double MAX_DISTANCE = 8.0D;

    private PokeStatueConfigServerHandler() {}

    public static void handleSave(ServerPlayer player, SavePokeStatueConfigPayload payload) {
        BlockPos pos = payload.pos();
        if (!player.blockPosition().closerThan(pos, MAX_DISTANCE)) {
            return;
        }
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!allowed(player, level, state)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof AbstractPokeStatueBlockEntity be)) {
            return;
        }
        // configure() re-clamps mode, scale, progress and speed against this block's own variant;
        // the playback options only land on a Creative Statue, the regular one ignores them.
        be.configure(payload.species(), payload.mode(), payload.scale(),
                payload.color(), payload.animation(), payload.progress(),
                payload.fixed(), payload.speed(), payload.showDummy());
    }

    /** Per-block gate: the two statues have deliberately different access rules. */
    private static boolean allowed(ServerPlayer player, Level level, BlockState state) {
        if (state.getBlock() instanceof CreativeStatueBlock) {
            // Creative-only block: no dimensional ban check, it could never fire for a creative player.
            return player.isCreative();
        }
        if (state.getBlock() instanceof PokeStatueBlock) {
            // The client only ever targets the lower half; refuse anything else rather than translating.
            if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER) {
                return false;
            }
            if (state.getValue(PokeStatueBlock.GLAZED)) {
                return false;
            }
            // Creative players may configure without the Tinkhammer (plan 188, C4).
            if (!player.isCreative()
                    && !player.getMainHandItem().is(ModItems.TINKHAMMER)
                    && !player.getOffhandItem().is(ModItems.TINKHAMMER)) {
                return false;
            }
            return player.isCreative() || BannedItemsManager.isBlockBreakingAllowed(level.dimension());
        }
        return false;
    }
}
