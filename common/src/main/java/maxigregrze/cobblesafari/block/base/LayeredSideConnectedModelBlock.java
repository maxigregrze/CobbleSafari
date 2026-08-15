package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A {@link SideConnectedModelBlock} that additionally cycles through {@code layerCount} model sets
 * according to its height inside a vertical run of stack peers - so a tower of scaffolding can
 * alternate its look floor by floor.
 *
 * <p>{@link #LAYER} has a fixed {@code 0..MAX_LAYERS-1} range shared by every instance, because a
 * per-block range would have to be chosen before subclass fields exist (see
 * {@link ConnectedModelBlock}). {@code layerCount} therefore only drives the modulo: states above
 * it are simply never reached, and the generated blockstate aliases them onto set 0.</p>
 */
public class LayeredSideConnectedModelBlock extends SideConnectedModelBlock {

    /** Hard ceiling on authored model sets. Bump this and regenerate blockstates to extend. */
    public static final int MAX_LAYERS = 4;

    public static final IntegerProperty LAYER = IntegerProperty.create("layer", 0, MAX_LAYERS - 1);

    private final int layerCount;

    /**
     * One collision table per layer, or null when collision does not vary by layer. Alternating
     * model sets can put matter where the previous layer had none, so a silhouette alone is not
     * always enough to describe what a state collides with.
     */
    @Nullable
    private final VoxelShape[][] collisionsByLayer;

    public LayeredSideConnectedModelBlock(Properties properties, Settings settings, int layerCount) {
        super(properties, settings);
        this.layerCount = Mth.clamp(layerCount, 1, MAX_LAYERS);
        this.collisionsByLayer = settings.layerSilhouetteCollisions().isEmpty()
                ? null
                : buildLayeredCollisions(settings, this.layerCount);
        this.registerDefaultState(this.defaultBlockState().setValue(LAYER, 0));
    }

    /** Per-layer declarations win; any layer without one falls back to the layer-agnostic table. */
    private static VoxelShape[][] buildLayeredCollisions(Settings settings, int layerCount) {
        VoxelShape[][] out = new VoxelShape[layerCount][];
        for (int layer = 0; layer < layerCount; layer++) {
            Map<ConnectedShape, VoxelShape> declared =
                    settings.layerSilhouetteCollisions().getOrDefault(layer, settings.silhouetteCollisions());
            out[layer] = buildSilhouetteCollisions(declared);
        }
        return out;
    }

    @Override
    protected VoxelShape collisionShapeFor(BlockState state) {
        if (collisionsByLayer == null) {
            return super.collisionShapeFor(state);
        }
        int layer = Math.min(state.getValue(LAYER), collisionsByLayer.length - 1);
        return collisionsByLayer[layer][shapeIndex(state)];
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new LayeredSideConnectedModelBlock(props, this.settings, this.layerCount));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LAYER);
    }

    @Override
    protected BlockState computeState(BlockState state, BlockGetter level, BlockPos pos) {
        // Layer counting is axis-agnostic and shared with LayeredVerticalConnectedModelBlock.
        return super.computeState(state, level, pos)
                .setValue(LAYER, computeLayer(state, level, pos, LAYER, layerCount));
    }
}
