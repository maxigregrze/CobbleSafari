package maxigregrze.cobblesafari.block.distortion;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.DistortionFlowerPart;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DistortionCarpetFlowerBlock extends Block {

    public static final MapCodec<DistortionCarpetFlowerBlock> CODEC = simpleCodec(DistortionCarpetFlowerBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public DistortionCarpetFlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(ModBlocks.DISTORTION_FLOWER)) {
            return false;
        }
        if (below.getValue(DistortionFlowerBlock.PART) != DistortionFlowerPart.STEM) {
            return false;
        }
        return below.getValue(DistortionFlowerBlock.FACE) == AttachFace.WALL;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
