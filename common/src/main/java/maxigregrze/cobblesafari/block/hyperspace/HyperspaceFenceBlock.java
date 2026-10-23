package maxigregrze.cobblesafari.block.hyperspace;

import maxigregrze.cobblesafari.block.base.BlockShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Kalosian Fence Wall (plan 194 - 5): connects exactly like a vanilla wall and keeps the wall's
 * 24px collision; only the selection box follows the model - a slim post plus one top rail per
 * connected side.
 *
 * <p>{@code up} and {@code low}/{@code tall} stay in the state, as WallBlock computes them, but the
 * multipart blockstate ignores them: the post is always drawn and both heights draw the same rail.
 * Being in {@code #minecraft:walls} is what makes vanilla walls connect back to it.</p>
 */
public class HyperspaceFenceBlock extends WallBlock {

    private static final VoxelShape POST = Shapes.or(
            Block.box(6, 0, 6, 10, 1, 10), Block.box(6.5, 1, 6.5, 9.5, 3, 9.5),
            Block.box(6, 3, 6, 10, 4, 10), Block.box(7, 4, 7, 9, 12, 9),
            Block.box(6.5, 12, 6.5, 9.5, 13, 9.5), Block.box(6, 13, 6, 10, 14, 10),
            Block.box(5.5, 14, 5.5, 10.5, 16, 10.5));
    private static final Map<Direction, VoxelShape> ARMS =
            BlockShapeUtils.precompute(Block.box(5.5, 14, 0, 10.5, 16, 5.5), Direction.NORTH);

    /** Selection shape per connection set, indexed north=1, east=2, south=4, west=8. */
    private static final VoxelShape[] OUTLINES = buildOutlines();

    public HyperspaceFenceBlock(Properties properties) {
        super(properties);
    }

    private static VoxelShape[] buildOutlines() {
        Direction[] order = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        VoxelShape[] out = new VoxelShape[16];
        for (int i = 0; i < out.length; i++) {
            VoxelShape shape = POST;
            for (int bit = 0; bit < order.length; bit++) {
                if ((i & (1 << bit)) != 0) {
                    shape = Shapes.or(shape, ARMS.get(order[bit]));
                }
            }
            out[i] = shape.optimize();
        }
        return out;
    }

    private static int index(BlockState state) {
        int index = 0;
        if (state.getValue(NORTH_WALL) != WallSide.NONE) {
            index |= 1;
        }
        if (state.getValue(EAST_WALL) != WallSide.NONE) {
            index |= 2;
        }
        if (state.getValue(SOUTH_WALL) != WallSide.NONE) {
            index |= 4;
        }
        if (state.getValue(WEST_WALL) != WallSide.NONE) {
            index |= 8;
        }
        return index;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINES[index(state)];
    }
}
