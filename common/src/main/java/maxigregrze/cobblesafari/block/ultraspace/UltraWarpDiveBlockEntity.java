package maxigregrze.cobblesafari.block.ultraspace;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Data-less block entity: it only hosts the Ultra Warp Dive renderer (action plan 189, section 4.5). */
public class UltraWarpDiveBlockEntity extends BlockEntity {

    public UltraWarpDiveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ULTRAWARPDIVE, pos, state);
    }
}
