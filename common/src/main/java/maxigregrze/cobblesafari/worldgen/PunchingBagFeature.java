package maxigregrze.cobblesafari.worldgen;

import com.mojang.serialization.Codec;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Worldgen feature placing the punching bag (2-block multiblock), because
 * {@code minecraft:simple_block} does not call {@code setPlacedBy} and would
 * therefore place only the lower half.
 */
public class PunchingBagFeature extends Feature<NoneFeatureConfiguration> {

    public PunchingBagFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        BlockPos above = pos.above();

        boolean supported = level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        boolean spaceFree = level.getBlockState(pos).canBeReplaced() && level.getBlockState(above).canBeReplaced();
        if (!supported || !spaceFree) {
            return false;
        }

        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        BlockState lower = ModBlocks.PUNCHINGBAG.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);

        level.setBlock(pos, lower, Block.UPDATE_CLIENTS);
        level.setBlock(above, upper, Block.UPDATE_CLIENTS);
        return true;
    }
}
