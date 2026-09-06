package maxigregrze.cobblesafari.block.ultraspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.FaceAttachedModelBlock;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Strange Bonsai sapling stage. Placement, orientation and survival are entirely the
 * {@link FaceAttachedModelBlock} base's job - configured floor-and-wall only, on a full block.
 * The one thing this class adds is growth, and it follows {@code HyperspaceSaplingBlock}: the stem
 * <em>never</em> grows on its own (it is not registered {@code randomTicks()}), only rarely when
 * bonemealed.
 */
public class UltraspaceBonsaiStemBlock extends FaceAttachedModelBlock implements BonemealableBlock {

    /** Chance per bonemeal use that the stem becomes a bonsai. */
    private static final float BONEMEAL_SUCCESS_CHANCE = 0.25F;

    private final Settings settings;

    public UltraspaceBonsaiStemBlock(Properties properties, Settings settings) {
        super(properties, settings);
        this.settings = settings;
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new UltraspaceBonsaiStemBlock(props, this.settings));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < BONEMEAL_SUCCESS_CHANCE;
    }

    /**
     * The bonsai inherits the stem's face and facing, so a stem on a wall grows a wall bonsai
     * pointing the same way. Which of the two drawn variants it gets is decided by the blockstate's
     * model array, not here - the mod keeps visual randomness out of Java.
     */
    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockState grown = ModBlocks.ULTRASPACE_BONSAI.defaultBlockState()
                .setValue(FACE, state.getValue(FACE))
                .setValue(FACING, state.getValue(FACING));
        if (grown.canSurvive(level, pos)) {
            level.setBlock(pos, grown, Block.UPDATE_ALL);
        }
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.GROWER;
    }
}
