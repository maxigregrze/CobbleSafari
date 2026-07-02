package maxigregrze.cobblesafari.worldgen;

import com.mojang.serialization.Codec;
import maxigregrze.cobblesafari.block.hyperspace.HyperspaceQuadBlock;
import maxigregrze.cobblesafari.block.hyperspace.HyperspaceQuadPart;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places one of a few Hyperspace "prop" decorations on the surface: the full lamp post
 * (the 4-cell {@link HyperspaceQuadBlock} stacked) or a flowerpot topped with a Hyperspace
 * sapling (plain or flowering). Multiblocks cannot be placed by {@code minecraft:simple_block}
 * (it never calls {@code setPlacedBy}), so every cell is written directly with
 * {@code UPDATE_CLIENTS} — same approach as {@link PunchingBagFeature}.
 */
public class HyperspaceDecorationFeature extends Feature<NoneFeatureConfiguration> {

    public HyperspaceDecorationFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource random = context.random();

        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                || !level.getBlockState(pos).canBeReplaced()) {
            return false;
        }

        int choice = random.nextInt(3);
        if (choice == 0) {
            return placeLamppost(level, pos, random);
        }
        Block sapling = choice == 1 ? ModBlocks.HYPERSPACE_SAPLING : ModBlocks.HYPERSPACE_SAPLING_FLOWERED;
        return placePottedSapling(level, pos, sapling);
    }

    private boolean placeLamppost(WorldGenLevel level, BlockPos pos, RandomSource random) {
        for (int dy = 0; dy <= 3; dy++) {
            if (!level.getBlockState(pos.above(dy)).canBeReplaced()) {
                return false;
            }
        }
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockState base = ModBlocks.HYPERSPACE_LAMPPOST.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
        level.setBlock(pos, base.setValue(HyperspaceQuadBlock.PART, HyperspaceQuadPart.BOTTOM), Block.UPDATE_CLIENTS);
        level.setBlock(pos.above(), base.setValue(HyperspaceQuadBlock.PART, HyperspaceQuadPart.CENTER), Block.UPDATE_CLIENTS);
        level.setBlock(pos.above(2), base.setValue(HyperspaceQuadBlock.PART, HyperspaceQuadPart.CENTERTOP), Block.UPDATE_CLIENTS);
        level.setBlock(pos.above(3), base.setValue(HyperspaceQuadBlock.PART, HyperspaceQuadPart.TOP), Block.UPDATE_CLIENTS);
        return true;
    }

    private boolean placePottedSapling(WorldGenLevel level, BlockPos pos, Block sapling) {
        if (!level.getBlockState(pos.above()).canBeReplaced()) {
            return false;
        }
        level.setBlock(pos, ModBlocks.HYPERSPACE_FLOWERPOT.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pos.above(), sapling.defaultBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }
}
