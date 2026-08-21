package maxigregrze.cobblesafari.block.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Untinted maple foliage. Behaves like vanilla leaves - it decays by distance from
 * {@code #minecraft:logs}, which the mangrove trunk already belongs to - and adds two things: it
 * drifts coloured leaves on the client, and it slowly litters the ground beneath it.
 *
 * <p>The block carries no colour handler, which is precisely what keeps it untinted: vanilla leaves
 * are only green because a {@code BlockColor} is registered for them.</p>
 */
public class MapleLeavesBlock extends LeavesBlock {

    /** How far down to look for the ground when littering. */
    private static final int LITTER_DROP = 4;
    /** Naturally shed litter never grows past this; only players and bonemeal go higher. */
    private static final int NATURAL_LAYER_CAP = 3;

    private final Supplier<SimpleParticleType> particle;
    private final Supplier<Block> pile;

    public MapleLeavesBlock(Properties properties, Supplier<SimpleParticleType> particle, Supplier<Block> pile) {
        super(properties);
        this.particle = particle;
        this.pile = pile;
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return simpleCodec(props -> new MapleLeavesBlock(props, this.particle, this.pile));
    }

    /**
     * Every maple leaf ticks, not just the dying ones.
     *
     * <p>{@link LeavesBlock#isRandomlyTicking} returns {@code distance == 7 && !persistent} - it
     * only wakes leaves that are <em>about to decay</em>, because vanilla has nothing else for a
     * leaf to do. Leaves on a living tree sit at distance 1..6 and would therefore never receive a
     * random tick at all, so {@link #randomTick} - and with it the whole littering behaviour -
     * would never run on a healthy tree. Decay itself is unaffected: {@code super.randomTick}
     * still re-checks {@code decaying(state)} before removing anything.</p>
     */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);   // vanilla decay must keep running
        if (random.nextFloat() >= MapleLeafPileBlock.LITTER_CHANCE) {
            return;
        }
        litter(level, pos);
    }

    /**
     * Sheds one layer of litter on the ground below, if this block is on the underside of the
     * canopy. The eligibility test is one lookup and comes first: without it every leaf block in a
     * tree - a hundred and more - would litter daily and bury the forest floor in a day.
     */
    private void litter(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).canBeReplaced()) {
            return;                                     // not the underside of the canopy
        }
        if (!(pile.get() instanceof MapleLeafPileBlock pileBlock)) {
            return;
        }
        BlockPos.MutableBlockPos cursor = pos.mutable().move(0, -1, 0);
        for (int i = 0; i < LITTER_DROP; i++) {
            BlockPos below = cursor.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
                pileBlock.accumulateAt(level, cursor.immutable(), NATURAL_LAYER_CAP);
                return;
            }
            if (!level.getBlockState(cursor).canBeReplaced() && !level.getBlockState(cursor).is(pileBlock)) {
                return;                                 // something solid in the way
            }
            cursor.move(0, -1, 0);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(12) != 0) {
            return;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
            level.addParticle(particle.get(),
                    pos.getX() + random.nextDouble(),
                    pos.getY() - 0.05,
                    pos.getZ() + random.nextDouble(),
                    0.0, 0.0, 0.0);
        }
    }
}
