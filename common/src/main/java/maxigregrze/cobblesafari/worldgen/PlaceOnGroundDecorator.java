package maxigregrze.cobblesafari.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import maxigregrze.cobblesafari.init.ModFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/**
 * Scatters blocks on the ground around a tree's trunk - leaf litter, in this mod's case.
 *
 * <p>A backport in spirit of the {@code minecraft:place_on_ground} decorator Mojang added
 * <em>after</em> 1.21.1 for the pale oak's moss carpet. Nothing in 1.21.1 can do this:
 * {@code alter_ground} replaces the ground block in place, which would dig a hole, and
 * {@code attached_to_leaves} hangs blocks in mid-air. The codec below is our own - it does not
 * attempt to mirror a signature that cannot be read from this version - so on a future
 * Minecraft update this type becomes a candidate for removal in favour of the vanilla one.</p>
 *
 * <p>Placement works by sampling rather than sweeping the disc: repeated draws with possible
 * duplicates give the irregular scatter comparable vanilla decorators produce, where an exhaustive
 * sweep would lay down a suspiciously even ring.</p>
 */
public class PlaceOnGroundDecorator extends TreeDecorator {

    public static final MapCodec<PlaceOnGroundDecorator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.intRange(1, 256).fieldOf("tries").forGetter(d -> d.tries),
                    Codec.intRange(0, 16).fieldOf("radius").forGetter(d -> d.radius),
                    Codec.intRange(1, 16).fieldOf("height").forGetter(d -> d.height),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(d -> d.chance),
                    BlockStateProvider.CODEC.fieldOf("block_provider").forGetter(d -> d.blockProvider)
            ).apply(instance, PlaceOnGroundDecorator::new));

    /** How far above the trunk base the column scan starts, so uneven ground still resolves. */
    private static final int SCAN_ABOVE = 2;

    private final int tries;
    private final int radius;
    private final int height;
    private final float chance;
    private final BlockStateProvider blockProvider;

    public PlaceOnGroundDecorator(int tries, int radius, int height, float chance, BlockStateProvider blockProvider) {
        this.tries = tries;
        this.radius = radius;
        this.height = height;
        this.chance = chance;
        this.blockProvider = blockProvider;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModFeatures.PLACE_ON_GROUND;
    }

    @Override
    public void place(Context context) {
        if (context.logs().isEmpty()) {
            return;
        }
        // logs() is sorted by Y, so the first entry is the lowest trunk block - usually the tree
        // origin. "Usually", because CherryTrunkPlacer routes setDirtAt through the SAME setter as
        // the trunk: on ground that is not already in #minecraft:dirt (farmland, for instance) the
        // replacement dirt block lands in this set too and drags the base one block underground.
        // That is absorbed by placeOnGround starting its scan SCAN_ABOVE blocks higher, rather than
        // by filtering here - the scan has to tolerate uneven ground anyway.
        BlockPos base = context.logs().get(0);
        RandomSource random = context.random();
        for (int i = 0; i < tries; i++) {
            if (random.nextFloat() >= chance) {
                continue;
            }
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz > radius * radius) {
                continue;                       // keep the scatter inside a disc, not a square
            }
            placeOnGround(context, base.offset(dx, 0, dz));
        }
    }

    /**
     * Looks for the first free cell resting on solid ground in the column through {@code start},
     * and drops one block there.
     *
     * <p>The scan starts {@value #SCAN_ABOVE} blocks above the trunk base and walks down past
     * {@code height}, and it <em>steps over</em> anything solid instead of giving up on the column -
     * the pattern vanilla's {@code AlterGroundDecorator} uses. An earlier version started level with
     * the base and returned on the first non-replaceable cell, which aborted the whole column at the
     * slightest obstruction.</p>
     *
     * <p>Only {@link Context#level()} is available here, a {@link LevelSimulatedReader}, so every
     * test goes through {@code isStateAtPosition}: neither {@code getBlockState} nor
     * {@code isFaceSturdy} can be reached during worldgen decoration.</p>
     */
    private void placeOnGround(Context context, BlockPos start) {
        LevelSimulatedReader level = context.level();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = SCAN_ABOVE; dy >= -height; dy--) {
            cursor.setWithOffset(start, 0, dy, 0);
            if (!level.isStateAtPosition(cursor, BlockStateBase::canBeReplaced)) {
                continue;                       // trunk, canopy, a flower - keep walking down
            }
            BlockPos below = cursor.below();
            if (level.isStateAtPosition(below, BlockStateBase::isSolid)) {
                context.setBlock(cursor.immutable(), blockProvider.getState(context.random(), cursor));
                return;
            }
        }
    }
}
