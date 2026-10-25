package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.BlockShapeUtils;
import maxigregrze.cobblesafari.block.base.SideConnectedModelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Kalosian Planter (plan 194 - 11 ter): a stone box of soil with a 4px rim that overhangs the cell by
 * 2px and opens towards the planters beside it. A side's rim drops when that side connects; a 2x2
 * corner post closes the gap between two rims and only stands when neither of its two sides
 * connects. It counts as dirt ({@code #minecraft:dirt}), so flowers, saplings and bushes take root.
 *
 * <p>The overhang is what makes diagonals irrelevant: a neighbouring planter's rim always covers the
 * corner, so the four sides alone decide the model. Selection and collision follow the rim drawn.</p>
 */
public class HyperspacePlanterBlock extends SideConnectedModelBlock {

    private static final VoxelShape BODY = Block.box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape SIDE_NORTH = Block.box(0, 16, -2, 16, 20, 2);
    private static final VoxelShape SIDE_EAST = Block.box(14, 16, 0, 18, 20, 16);
    private static final VoxelShape SIDE_SOUTH = Block.box(0, 16, 14, 16, 20, 18);
    private static final VoxelShape SIDE_WEST = Block.box(-2, 16, 0, 2, 20, 16);
    private static final VoxelShape POST_NE = Block.box(16, 16, -2, 18, 20, 0);
    private static final VoxelShape POST_SE = Block.box(16, 16, 16, 18, 20, 18);
    private static final VoxelShape POST_SW = Block.box(-2, 16, 16, 0, 20, 18);
    private static final VoxelShape POST_NW = Block.box(-2, 16, -2, 0, 20, 0);

    /** Bits: north, east, south, west. */
    private static final VoxelShape[] SHAPES = buildShapes();

    public HyperspacePlanterBlock(Properties properties, Settings settings) {
        super(properties, settings);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new HyperspacePlanterBlock(props, this.settings));
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape[] sides = {SIDE_NORTH, SIDE_EAST, SIDE_SOUTH, SIDE_WEST};
        // Post i sits between side i and side i+1 (clockwise): NE, SE, SW, NW.
        VoxelShape[] posts = {POST_NE, POST_SE, POST_SW, POST_NW};
        VoxelShape[] out = new VoxelShape[16];
        for (int i = 0; i < out.length; i++) {
            VoxelShape shape = BODY;
            for (int side = 0; side < 4; side++) {
                boolean open = (i & (1 << side)) == 0;
                boolean nextOpen = (i & (1 << ((side + 1) % 4))) == 0;
                if (open) {
                    shape = Shapes.or(shape, sides[side]);
                }
                if (open && nextOpen) {
                    shape = Shapes.or(shape, posts[side]);
                }
            }
            out[i] = shape.optimize();
        }
        return out;
    }

    @Override
    protected VoxelShape shapeFor(BlockState state) {
        return SHAPES[shapeIndex(state)];
    }

    /**
     * Its sides are a rim, not a wall: only the top and bottom count as solid, so torches and lanterns
     * still attach while walls, bars and panes stop connecting. Plants rely on {@code #dirt}, not on
     * this.
     */
    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return BlockShapeUtils.CAPPED_SUPPORT;
    }
}
