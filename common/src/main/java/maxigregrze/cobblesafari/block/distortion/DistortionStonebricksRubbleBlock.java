package maxigregrze.cobblesafari.block.distortion;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class DistortionStonebricksRubbleBlock extends DistortionWeedBlock {
    public static final MapCodec<DistortionStonebricksRubbleBlock> CODEC = simpleCodec(DistortionStonebricksRubbleBlock::new);

    public DistortionStonebricksRubbleBlock(Properties properties) {
        super(properties, true);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        AttachFace face = faceForDirection(clickedFace);
        Direction horizontalFacing = face == AttachFace.WALL
                ? clickedFace
                : context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        BlockState candidate = this.defaultBlockState()
                .setValue(FACING, horizontalFacing)
                .setValue(FACE, face);
        if (this.canSurvive(candidate, context.getLevel(), pos)) {
            return candidate;
        }
        return null;
    }
}
