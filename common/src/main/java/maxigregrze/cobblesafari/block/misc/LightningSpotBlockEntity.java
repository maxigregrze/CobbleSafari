package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Deliberately empty: no persisted state, no ticking. Its only purpose is to open the
 * {@code BlockEntityRenderer} path, because the marker icon has to appear or not depending on what
 * the viewing player holds - something a blockstate cannot express.
 */
public class LightningSpotBlockEntity extends BlockEntity {

    public LightningSpotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIGHTNINGSPOT, pos, state);
    }
}
