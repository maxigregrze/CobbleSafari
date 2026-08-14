package maxigregrze.cobblesafari.block.pokestatue;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Regular (two-block) statue configuration. One block entity type is created for <b>both</b>
 * halves: the lower one carries the data, the upper one is a render anchor whose renderer reads
 * {@code pos.below()}.
 *
 * <p>{@code glazed} is deliberately <em>not</em> stored here - it lives on the block state so the
 * light level and the WTHIT/Jade tooltip can read it without a block entity lookup.</p>
 */
public class PokeStatueBlockEntity extends AbstractPokeStatueBlockEntity {

    public static final float MAX_SCALE = 3.0f;

    public PokeStatueBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POKEMON_STATUE, pos, state);
    }

    @Override
    public PokeStatueVariant variant() {
        return PokeStatueVariant.REGULAR;
    }

    public PokeStatueMaterial getMaterial() {
        return PokeStatueMaterial.byId(getMode());
    }

    @Override
    public PokeStatueRenderStyle renderStyle() {
        return getMaterial().styleFor(getColor());
    }

    /** Both halves render part of the statue, so both need the refresh. */
    @Override
    protected BlockPos[] syncPositions() {
        return new BlockPos[]{worldPosition, worldPosition.above()};
    }

    /**
     * Mirrors the hologram finish and the "has a colour" flag into the block state of both halves.
     * {@code HOLOGRAM} drives the light level (a function of BlockState, hence not stored here);
     * {@code TINTED} selects the pedestal model that bakes the overlay layer.
     */
    @Override
    protected void applyDerivedState() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean hologram = getMaterial().isHologram();
        boolean tinted = hasColor();
        for (BlockPos p : syncPositions()) {
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof PokeStatueBlock)) {
                continue;
            }
            if (s.getValue(PokeStatueBlock.HOLOGRAM) != hologram || s.getValue(PokeStatueBlock.TINTED) != tinted) {
                // UPDATE_ALL so the light engine re-runs when the hologram flag switches 10 <-> 0.
                level.setBlock(p, s.setValue(PokeStatueBlock.HOLOGRAM, hologram)
                        .setValue(PokeStatueBlock.TINTED, tinted), Block.UPDATE_ALL);
            }
        }
    }
}
