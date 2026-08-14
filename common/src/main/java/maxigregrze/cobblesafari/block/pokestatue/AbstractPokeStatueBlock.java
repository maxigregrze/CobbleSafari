package maxigregrze.cobblesafari.block.pokestatue;

import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared base for the statue blocks: orientation, resolution of the part that stores the
 * configuration, and opening the shared config screen.
 *
 * <p>Everything that genuinely differs stays in the subclasses - the regular statue's two-block
 * structure, waxing and light level, the creative statue's full cube and indestructibility.</p>
 */
public abstract class AbstractPokeStatueBlock extends HorizontalDirectionalBlock implements EntityBlock {

    protected AbstractPokeStatueBlock(Properties properties) {
        super(properties);
    }

    /**
     * Position of the block entity holding the configuration for the part at {@code pos}. Identity
     * for a single-block statue; the lower half for the two-block one.
     */
    public abstract BlockPos dataPos(BlockState state, BlockPos pos);

    /** Vertical origin of the Pokémon model within its own block, in blocks. */
    public abstract double modelOriginY();

    /**
     * Sends the config screen to a player already validated by the caller. Server side only;
     * returns false when there is no block entity to configure.
     */
    protected boolean openConfig(Level level, BlockPos pos, BlockState state, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        AbstractPokeStatueBlockEntity be = AbstractPokeStatueBlockEntity.dataAt(level, pos, state);
        if (be == null) {
            return false;
        }
        Services.PLATFORM.sendPayloadToPlayer(serverPlayer, be.createOpenPayload());
        return true;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
