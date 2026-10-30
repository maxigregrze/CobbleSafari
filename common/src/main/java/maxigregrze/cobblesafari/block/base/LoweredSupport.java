package maxigregrze.cobblesafari.block.base;

import maxigregrze.cobblesafari.init.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Road-like floors one pixel short of a full block ({@link ModBlockTags#LOWERED_SUPPORTS}): a small block
 * standing on one is drawn 1 px lower, so it sits on the road instead of floating above it
 * (action plan 196, sections 7.9 and 8.8).
 */
public final class LoweredSupport {

    private static final double PX = 1.0 / 16.0;
    /** Top of a road-like block, with some slack for rounding. */
    private static final double TOP_MIN = 14.5 * PX;
    private static final double TOP_MAX = 15.5 * PX;

    private LoweredSupport() {}

    /** Only states whose shape actually tops out at 15 px count, so a bottom road slab does not. */
    public static boolean test(BlockState state, BlockGetter level, BlockPos pos) {
        if (!state.is(ModBlockTags.LOWERED_SUPPORTS)) {
            return false;
        }
        double top = state.getShape(level, pos).max(Direction.Axis.Y);
        return top >= TOP_MIN && top <= TOP_MAX;
    }
}
