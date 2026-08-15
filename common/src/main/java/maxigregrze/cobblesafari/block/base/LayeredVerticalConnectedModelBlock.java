package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A {@link VerticalConnectedModelBlock} that additionally cycles through {@code layerCount} model
 * sets according to its height inside its vertical run - the vertical mirror of
 * {@link LayeredSideConnectedModelBlock}, and the crossing of the two existing classes.
 *
 * <p>Neither parent alone is enough for a stack whose <em>top</em> is drawn differently.
 * {@link #LAYER} counts from the bottom and can never say "I am the last one"; {@code SEGMENT}
 * says exactly that but knows nothing of parity. Reusing
 * {@link LayeredSideConnectedModelBlock} instead would also drag in the four horizontal connection
 * booleans of {@link SideConnectedModelBlock}, so two such blocks placed side by side would read
 * as connected and 16 useless states would have to be folded back onto the same models.</p>
 *
 * <p>{@link #LAYER} has the same fixed {@code 0..MAX_LAYERS-1} range as its side counterpart, and
 * for the same reason (see {@link ConnectedModelBlock}): {@code layerCount} only drives the
 * modulo, and the generated blockstate aliases the unreachable values onto set 0.</p>
 */
public class LayeredVerticalConnectedModelBlock extends VerticalConnectedModelBlock {

    /** Hard ceiling on authored model sets. Bump this and regenerate blockstates to extend. */
    public static final int MAX_LAYERS = 4;

    public static final IntegerProperty LAYER = IntegerProperty.create("layer", 0, MAX_LAYERS - 1);

    private final int layerCount;

    public LayeredVerticalConnectedModelBlock(Properties properties, Settings settings, int layerCount) {
        super(properties, settings);
        this.layerCount = Mth.clamp(layerCount, 1, MAX_LAYERS);
        this.registerDefaultState(this.defaultBlockState().setValue(LAYER, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new LayeredVerticalConnectedModelBlock(props, this.settings, this.layerCount));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LAYER);
    }

    @Override
    protected BlockState computeState(BlockState state, BlockGetter level, BlockPos pos) {
        return super.computeState(state, level, pos)
                .setValue(LAYER, computeLayer(state, level, pos, LAYER, layerCount));
    }
}
