package maxigregrze.cobblesafari.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared waterlogging boilerplate. Same parti pris as {@link DaylightLit}: the property and the
 * bodies of the four overrides live here, and each block writes only the delegation.
 *
 * <p>A base class cannot declare {@code WATERLOGGED} optionally - {@code createBlockStateDefinition}
 * runs before any subclass field is assigned, so a settings-driven property would read {@code null}
 * (see the header of {@link ConnectedModelBlock}). And Java has no multiple inheritance, so a
 * mix-in superclass would have to be duplicated per hierarchy. A static utility is the only shape
 * that works at once under {@code Block}, {@code HorizontalDirectionalBlock}, {@code StairBlock}
 * and {@code ConnectedModelBlock}.</p>
 *
 * <p>The block still has to {@code implements SimpleWaterloggedBlock}, add {@link #WATERLOGGED} in
 * {@code createBlockStateDefinition} and set it in {@code registerDefaultState}: those three touch
 * the block's own type and state definition and cannot be delegated.</p>
 */
public final class Waterlogged {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private Waterlogged() {}

    /** True when the clicked cell holds a water source. Body of {@code getStateForPlacement}. */
    public static boolean atPlacement(BlockPlaceContext context) {
        return context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
    }

    /**
     * Body of {@code getFluidState}. {@code fallback} is the caller's {@code super.getFluidState},
     * which this class cannot reach.
     */
    public static FluidState fluidState(BlockState state, FluidState fallback) {
        return Boolean.TRUE.equals(state.getValue(WATERLOGGED)) ? Fluids.WATER.getSource(false) : fallback;
    }

    /**
     * Body of {@code updateShape}: schedules the fluid tick so the water spreads. Call it
     * <em>before</em> delegating, since the delegate may return a different state - or air.
     */
    public static void scheduleTick(BlockState state, LevelAccessor level, BlockPos pos) {
        if (Boolean.TRUE.equals(state.getValue(WATERLOGGED))) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }
}
