package maxigregrze.cobblesafari.client.renderer;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The spin and bob of a floating ball, shared by the Auspicious Pokeball and the floating Lost Item
 * (action plan 193, section 7.4) so both always move at the same pace.
 */
public final class FloatingBallAnimation {

    public static final float ROTATION_DEGREES_PER_TICK = 2.0f;
    /** Slow period of the vertical bob. */
    public static final float BOB_PHASE_PER_TICK = 0.04f;

    private FloatingBallAnimation() {
    }

    public static double time(BlockEntity blockEntity, float partialTick) {
        return blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() + (double) partialTick : partialTick;
    }

    public static float yawDegrees(double t) {
        return (float) ((t * ROTATION_DEGREES_PER_TICK) % 360.0);
    }

    /** Unit wave in [-1, 1]; each caller scales it by its own amplitude in blocks. */
    public static float bobWave(double t) {
        return (float) Math.sin(t * BOB_PHASE_PER_TICK);
    }
}
