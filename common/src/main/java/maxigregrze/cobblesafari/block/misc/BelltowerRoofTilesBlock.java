package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import org.jetbrains.annotations.Nullable;

/**
 * Belltower roof tiles. Straight run, inner corner and outer corner, joining their own kind: that
 * is stair semantics, so this is a {@link StairBlock} with custom models rather than a bespoke
 * connected block. The corner derivation, the waterlogging and the shapes all come from vanilla -
 * and, crucially, a connected socle could not do the job at all, since it sees only whether a
 * neighbour is present, never which way it faces.
 *
 * <p>The only departure is that placement is forced to the bottom half: a roof is never upside
 * down. The {@code HALF} property still exists, so {@code /setblock} and structures can still write
 * {@code half=top} and the blockstate must keep covering it.</p>
 */
public class BelltowerRoofTilesBlock extends StairBlock {

    private final BlockState tilesBaseState;

    public BelltowerRoofTilesBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
        this.tilesBaseState = baseState;
    }

    @Override
    public MapCodec<? extends StairBlock> codec() {
        return simpleCodec(props -> new BelltowerRoofTilesBlock(this.tilesBaseState, props));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(HALF, Half.BOTTOM);
    }
}
