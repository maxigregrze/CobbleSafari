package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity of the belltower bell. Stores when the bell was last struck and from which side, to
 * drive the damped oscillation on the client.
 *
 * <p>Nothing is persisted and nothing is synchronised: the animation is purely visual and is
 * triggered by {@code Level#blockEvent}, like the vanilla bell and the punching bag. A bell that
 * was ringing when its chunk unloaded is simply still again when it comes back.</p>
 */
public class BelltowerBellBlockEntity extends BlockEntity {

    private long lastRingGameTime = Long.MIN_VALUE;
    private Direction ringFace = Direction.NORTH;

    public BelltowerBellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BELLTOWER_BELL, pos, state);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == BelltowerBellBlock.EVENT_RING) {
            if (this.level != null) {
                this.lastRingGameTime = this.level.getGameTime();
            }
            Direction face = Direction.from3DDataValue(param);
            // Guard against a malformed param: a vertical face would give a degenerate swing axis.
            this.ringFace = face.getAxis().isHorizontal() ? face : Direction.NORTH;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    public long getLastRingGameTime() {
        return this.lastRingGameTime;
    }

    public Direction getRingFace() {
        return this.ringFace;
    }
}
