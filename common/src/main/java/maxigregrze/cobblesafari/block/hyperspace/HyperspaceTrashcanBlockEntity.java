package maxigregrze.cobblesafari.block.hyperspace;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores the game time of the last "open" trigger, used by the renderer to drive the lid animation.
 * Animation is purely visual and triggered by {@code Level#blockEvent} (like the punching bag).
 */
public class HyperspaceTrashcanBlockEntity extends BlockEntity {

    private long lastOpenGameTime = Long.MIN_VALUE;

    public HyperspaceTrashcanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYPERSPACE_TRASHCAN, pos, state);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == HyperspaceTrashcanBlock.EVENT_OPEN) {
            if (this.level != null) {
                this.lastOpenGameTime = this.level.getGameTime();
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    public long getLastOpenGameTime() {
        return this.lastOpenGameTime;
    }
}
