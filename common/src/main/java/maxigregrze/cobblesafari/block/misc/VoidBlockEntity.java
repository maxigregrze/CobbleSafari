package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class VoidBlockEntity extends BlockEntity {

    public VoidBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOID_BLOCK, pos, state);
    }
}
