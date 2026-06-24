package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.block.base.HorizontalModelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Orientable ground "pile" block (mud pile / sludge pile). Selection box = 8×8 cube at the
 * bottom of the cell; no collision (entities pass through) and slows them like a web.
 * Placeable only on solid ground.
 *
 * <p>Pure orientation/shape/support is delegated to {@link HorizontalModelBlock}; the only
 * bespoke behaviour kept here is {@link #entityInside} (the slowdown). Subclasses may
 * extend it (e.g. {@code SludgePileBlock} adds poison).</p>
 */
public abstract class PileBlock extends HorizontalModelBlock {

    // 8×8 cube centered on X/Z at the bottom of the cell (symmetric under Y rotation).
    protected static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 8, 12);
    private static final Vec3 STUCK_MULTIPLIER = new Vec3(0.25, 0.05F, 0.25);

    protected PileBlock(Properties properties) {
        super(properties, Settings.builder()
                .shape(SHAPE)
                .support(Support.GROUND)
                .collision(Collision.NONE)
                .emptyOcclusion()
                .build());
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.makeStuckInBlock(state, STUCK_MULTIPLIER);
    }
}
