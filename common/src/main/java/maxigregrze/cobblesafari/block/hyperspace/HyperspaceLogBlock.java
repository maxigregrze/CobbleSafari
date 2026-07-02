package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.DirectionalModelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Hyperspace log: a six‑way oriented pillar (model + hitbox follow {@link DirectionalBlock#FACING})
 * with a custom <em>placement</em> rule — it may only be placed against a sturdy face of a solid
 * block, or onto another Hyperspace log sharing the <em>same</em> orientation. The orientation is
 * the clicked face: clicking a block's top ⇒ upright (UP), a side ⇒ lying (that side), the bottom
 * face of a block above ⇒ upside down (DOWN).
 *
 * <p>The restriction is checked only at placement (no {@code canSurvive}/{@code updateShape}): a
 * log already placed stays even if its support is later removed.</p>
 */
public class HyperspaceLogBlock extends DirectionalModelBlock {

    private static final VoxelShape PILLAR = Block.box(4, 0, 4, 12, 16, 12);

    public HyperspaceLogBlock(Properties properties) {
        super(properties, Settings.builder()
                .shape(PILLAR)
                .rotateShape()
                .defaultFacing(Direction.UP)
                .collision(Collision.SHAPE)
                .build());
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(HyperspaceLogBlock::new);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockPos supportPos = context.getClickedPos().relative(facing.getOpposite());
        BlockState support = context.getLevel().getBlockState(supportPos);
        boolean solid = support.isFaceSturdy(context.getLevel(), supportPos, facing);
        boolean sameLog = support.is(this) && support.getValue(FACING) == facing;
        return (solid || sameLog) ? this.defaultBlockState().setValue(FACING, facing) : null;
    }
}
