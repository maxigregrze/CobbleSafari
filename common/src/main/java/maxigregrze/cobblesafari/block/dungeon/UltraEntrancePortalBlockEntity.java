package maxigregrze.cobblesafari.block.dungeon;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Render anchor of the Ultra Wormhole device, on its main cell only. Carries no state: it exists so
 * {@code UltraEntrancePortalBlockEntityRenderer} can draw the models half a block to the side
 * (action plan 201 § 13).
 */
public class UltraEntrancePortalBlockEntity extends BlockEntity {

    public UltraEntrancePortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ULTRA_ENTRANCE_PORTAL, pos, state);
    }
}
