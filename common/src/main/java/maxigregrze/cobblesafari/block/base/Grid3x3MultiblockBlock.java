package maxigregrze.cobblesafari.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared engine for multiblocks laid out as a <strong>3x3 square on a single Y layer</strong>,
 * anchored on their centre cell: the player places the centre, the eight peripheral cells are
 * written for them, and breaking any one of the nine takes the whole structure down.
 *
 * <p>The mod's other multiblocks are all vertical - the Hyperspace family
 * ({@code HyperspaceDoubleBlock}, {@code HyperspaceTriBlock}, {@code HyperspaceQuadBlock}) and the
 * {@code DOUBLE_BLOCK_HALF} pairs ({@code OnlineFeaturePcBlock}, {@code PunchingBagBlock},
 * {@code UnionRoomGlobeBlock}, {@code PokeStatueBlock}). The Giratina Core is a 3x3 horizontal one
 * carrying its own copy of this logic; it is <em>not</em> migrated onto this class (it would have
 * to leave {@code BaseEntityBlock}), but {@link #removalFlags()} and
 * {@link #breaksOnPlayerDestroy(Player)} exist so that it could be, later, without any change in
 * behaviour.</p>
 *
 * <p>Everything that depends on <em>how a cell is represented</em> is left to the subclass. The
 * beam encodes its cell in a blockstate property; the Giratina Core encodes it in the block's
 * identity. Neither is better, so this class knows about neither - and in particular provides
 * <strong>no {@code rotate} / {@code mirror}</strong>.</p>
 */
public abstract class Grid3x3MultiblockBlock extends Block {

    /** The eight peripheral cells, on the constant-Y plane. Order carries no meaning. */
    protected static final int[][] SIDE_OFFSETS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1,  0},          {1,  0},
            {-1,  1}, {0,  1}, {1,  1}
    };

    /**
     * Re-entrancy guard shared by the whole hierarchy: clearing a cell fires its own
     * {@code onRemove}, which would call {@link #breakStructure} again. Static and not
     * thread-safe, exactly like {@code HyperspaceQuadBlock.breaking} and
     * {@code GiratinaCoreBlock.removingMultiblock} - block logic runs on the server thread.
     */
    private static boolean breaking = false;

    protected Grid3x3MultiblockBlock(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------ subclass contract

    /** True when this state is the centre cell, the one the player actually places. */
    protected abstract boolean isCenter(BlockState state);

    /** True when this state belongs to the same multiblock family as this block. */
    protected abstract boolean isPartOfSame(BlockState state);

    /** The centre of the multiblock the cell at {@code pos} carrying {@code state} belongs to. */
    protected abstract BlockPos centerOf(BlockState state, BlockPos pos);

    /**
     * The state to write at world offset {@code (dx, dz)} from the centre, on placement.
     *
     * <p>{@code partPos} is the cell's world position, still holding whatever the placement is
     * about to replace - a waterlogging subclass reads its fluid here, since by the time the cell
     * is written the fluid is the block's own.</p>
     */
    protected abstract BlockState partStateFor(BlockState centerState, Level level, BlockPos partPos, int dx, int dz);

    /** Flags for the clearing {@code setBlock}. The Giratina Core would return {@code 35}. */
    protected int removalFlags() {
        return Block.UPDATE_ALL;
    }

    /** Should destroying a cell as this player take the structure down? */
    protected boolean breaksOnPlayerDestroy(Player player) {
        return true;
    }

    // ------------------------------------------------------------------ lifecycle

    /** Refuses the placement unless the eight peripheral cells are replaceable. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        BlockPos center = context.getClickedPos();
        for (int[] offset : SIDE_OFFSETS) {
            if (!context.getLevel().getBlockState(center.offset(offset[0], 0, offset[1])).canBeReplaced(context)) {
                return null;
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !isCenter(state)) {
            return;
        }
        for (int[] offset : SIDE_OFFSETS) {
            BlockPos partPos = pos.offset(offset[0], 0, offset[1]);
            level.setBlock(partPos, partStateFor(state, level, partPos, offset[0], offset[1]), Block.UPDATE_ALL);
        }
    }

    /**
     * A peripheral cell lives as long as its centre does; the centre answers to nobody, so it
     * survives the very moment it is placed. Deliberately <em>not</em> backed by an
     * {@code updateShape} that returns air: cohesion is {@link #onRemove}'s job, which already
     * covers every way the centre can vanish.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (isCenter(state)) {
            return true;
        }
        BlockState center = level.getBlockState(centerOf(state, pos));
        return center.getBlock() instanceof Grid3x3MultiblockBlock other
                && other.isCenter(center)
                && isPartOfSame(center);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && breaksOnPlayerDestroy(player)) {
            breakStructure(level, centerOf(state, pos), pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && !breaking) {
            breakStructure(level, centerOf(state, pos), null);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Clears the nine cells. The peers go through {@code setBlock}, which produces
     * <strong>no drop</strong>; only the cell the player actually mined runs its loot table, so
     * exactly one item is returned whichever cell was broken.
     */
    protected void breakStructure(Level level, BlockPos center, @Nullable BlockPos keep) {
        if (breaking) {
            return;
        }
        breaking = true;
        try {
            clearIfPart(level, center, keep);
            for (int[] offset : SIDE_OFFSETS) {
                clearIfPart(level, center.offset(offset[0], 0, offset[1]), keep);
            }
        } finally {
            breaking = false;
        }
    }

    /**
     * A cell becomes its own fluid rather than plain air, so a waterlogged structure leaves water
     * behind exactly as {@code Level.destroyBlock} does for the cell the player mined. With no
     * fluid, {@code createLegacyBlock} is air, which is what every non-waterlogged multiblock got
     * before.
     */
    private void clearIfPart(Level level, BlockPos pos, @Nullable BlockPos keep) {
        if (pos.equals(keep)) {
            return; // the mined cell: vanilla removes it and runs its loot table
        }
        BlockState state = level.getBlockState(pos);
        if (isPartOfSame(state)) {
            level.setBlock(pos, state.getFluidState().createLegacyBlock(), removalFlags());
        }
    }
}
