package maxigregrze.cobblesafari.block.csboss;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Support de rendu du courant ascendant (retours plan 199) : permet au renderer de re-texturer les
 * rochers du geyser avec le bloc sur lequel la tuile est posée ; garde aussi la décoration remplacée.
 */
public class CsBossUpdraftTileBlockEntity extends CsBossTileBlockEntity {

    public CsBossUpdraftTileBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CSBOSS_UPDRAFT, pos, state);
    }
}
