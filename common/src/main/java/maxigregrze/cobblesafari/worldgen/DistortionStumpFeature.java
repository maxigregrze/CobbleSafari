package maxigregrze.cobblesafari.worldgen;

import com.mojang.serialization.Codec;
import maxigregrze.cobblesafari.block.DistortionFlowerPart;
import maxigregrze.cobblesafari.block.distortion.DistortionFlowerBlock;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Place une « souche de fleur distordue » verticale (base + tiges + fleur) de 4 à 7 blocs
 * de haut, posée au sol dans le biome Psy.
 */
public class DistortionStumpFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_HEIGHT = 4;
    private static final int MAX_HEIGHT = 7;

    public DistortionStumpFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos base = context.origin();

        if (!level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP)) {
            return false;
        }

        int height = MIN_HEIGHT + context.random().nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
        for (int i = 0; i < height; i++) {
            if (!level.getBlockState(base.above(i)).canBeReplaced()) {
                return false;
            }
        }

        Block block = ModBlocks.DISTORTION_FLOWER;
        BlockState common = block.defaultBlockState()
                .setValue(DistortionFlowerBlock.FACE, AttachFace.FLOOR)
                .setValue(DistortionFlowerBlock.FACING, Direction.NORTH);
        for (int i = 0; i < height; i++) {
            DistortionFlowerPart part;
            if (i == 0) {
                part = DistortionFlowerPart.BASE;
            } else if (i == height - 1) {
                part = DistortionFlowerPart.FLOWER;
            } else {
                part = DistortionFlowerPart.STEM;
            }
            level.setBlock(base.above(i), common.setValue(DistortionFlowerBlock.PART, part), Block.UPDATE_CLIENTS);
        }
        return true;
    }
}
