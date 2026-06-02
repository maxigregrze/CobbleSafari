package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity de la moitié supérieure du sac de frappe.
 * Garde l'instant du dernier coup (clic droit) pour piloter l'oscillation amortie
 * côté rendu. Aucune donnée persistante : l'animation est purement visuelle et
 * déclenchée par {@code Level#blockEvent} (comme la cloche vanilla).
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
