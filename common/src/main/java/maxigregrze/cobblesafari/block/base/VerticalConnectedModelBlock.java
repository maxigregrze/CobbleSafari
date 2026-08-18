package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A {@link ConnectedModelBlock} that scans the Y axis only: it derives {@link #SEGMENT} from the
 * peers directly above and below, so a lone block renders {@code single} and stacking a second one
 * turns the pair into {@code bottom} + {@code top}.
 *
 * <p>Unlike {@link HangingDoubleModelBlock}, this is <em>not</em> a multiblock: nothing is
 * auto-placed, nothing breaks jointly, and no support is required. Each block is independent and
 * simply re-reads its neighbours, so breaking the bottom of a pair leaves the top standing as
 * {@code single} instead of destroying it. Stack height is unbounded - three blocks give
 * {@code bottom} + {@code middle} + {@code top} with no extra code.</p>
 */
public class VerticalConnectedModelBlock extends ConnectedModelBlock {

    public static final EnumProperty<VerticalSegment> SEGMENT =
            EnumProperty.create("segment", VerticalSegment.class);

    public VerticalConnectedModelBlock(Properties properties, Settings settings) {
        super(properties, settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(SEGMENT, VerticalSegment.SINGLE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new VerticalConnectedModelBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEGMENT);
    }

    @Override
    protected BlockState computeState(BlockState state, BlockGetter level, BlockPos pos) {
        boolean below = connectsTo(state, ApparentState.at(level, pos.below()), Direction.DOWN);
        boolean above = connectsTo(state, ApparentState.at(level, pos.above()), Direction.UP);
        return state.setValue(SEGMENT, VerticalSegment.of(below, above));
    }

    /** Per-segment shape when declared (e.g. a sill on the bottom pane only); fixed shape otherwise. */
    @Override
    protected VoxelShape shapeFor(BlockState state) {
        VoxelShape segmentShape = settings.segmentShapes().get(state.getValue(SEGMENT));
        return segmentShape != null ? segmentShape : super.shapeFor(state);
    }
}
