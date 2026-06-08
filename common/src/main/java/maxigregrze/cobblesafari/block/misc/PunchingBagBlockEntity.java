package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the upper half of the punching bag.
 * Stores the time of the last hit (right-click) to drive damped oscillation
 * on the client. No persistent data: animation is purely visual and
 * triggered by {@code Level#blockEvent} (like the vanilla bell).
 */
public class PunchingBagBlockEntity extends BlockEntity {

    private long lastSwingGameTime = Long.MIN_VALUE;
    private boolean swingPositive = true;

    public PunchingBagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUNCHINGBAG, pos, state);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == PunchingBagBlock.EVENT_SWING) {
            if (this.level != null) {
                this.lastSwingGameTime = this.level.getGameTime();
            }
            this.swingPositive = param != 0;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    public long getLastSwingGameTime() {
        return this.lastSwingGameTime;
    }

    public boolean isSwingPositive() {
        return this.swingPositive;
    }
}
