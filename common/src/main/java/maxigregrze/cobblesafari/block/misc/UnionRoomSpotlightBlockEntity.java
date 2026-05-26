package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class UnionRoomSpotlightBlockEntity extends BlockEntity {

    public UnionRoomSpotlightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UNION_ROOM_SPOTLIGHT, pos, state);
    }
}
