package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.BlockShapeUtils;
import maxigregrze.cobblesafari.block.base.HorizontalModelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Kalosian Posters (plan 194 - 11 quinquies): paper stuck on a wall, one of four layouts.
 *
 * <p>The layout is a state property rather than a blockstate random pick, so the selection box can
 * follow the sheets actually drawn. It is chosen at placement from the position's seed, so client
 * and server agree on it without a round trip. The models are drawn against a wall to their north,
 * i.e. as they stand at FACING=south, and the tilted sheets are approximated by four slices each.</p>
 */
public class HyperspacePosterBlock extends HorizontalModelBlock {

    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 1, 4);

    private static final List<Map<Direction, VoxelShape>> SHAPES = List.of(
            shape(Block.box(5, 3.5, 0, 14, 7.5, 1),
                    Block.box(3.5, 7, 0, 14, 10.5, 1),
                    Block.box(2, 10.5, 0, 12.5, 14, 1),
                    Block.box(2, 13.5, 0, 11, 17.5, 1)),
            shape(Block.box(1, 0.5, 0, 9, 4, 1),
                    Block.box(0, 3.5, 0, 9, 6.5, 1),
                    Block.box(-1.5, 6, 0, 8, 9.5, 1),
                    Block.box(-1.5, 9, 0, 7, 12, 1),
                    Block.box(8, 7, 0, 16, 10.5, 1),
                    Block.box(8, 10, 0, 17, 13, 1),
                    Block.box(9, 12.5, 0, 18.5, 16, 1),
                    Block.box(10.5, 15.5, 0, 18.5, 18.5, 1)),
            shape(Block.box(4, -1.5, 0, 13, 2, 1),
                    Block.box(4, 1.5, 0, 14.5, 5.5, 1),
                    Block.box(5, 5, 0, 15.5, 8.5, 1),
                    Block.box(6.5, 8, 0, 15.5, 11.5, 1)),
            shape(Block.box(3, 5, 0, 10, 17, 1)));

    public HyperspacePosterBlock(Properties properties, Settings settings) {
        super(properties, settings);
        this.registerDefaultState(this.defaultBlockState().setValue(VARIANT, 1));
    }

    private static Map<Direction, VoxelShape> shape(VoxelShape... boxes) {
        VoxelShape out = Shapes.empty();
        for (VoxelShape box : boxes) {
            out = Shapes.or(out, box);
        }
        return BlockShapeUtils.precompute(out.optimize(), Direction.SOUTH);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new HyperspacePosterBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        int variant = 1 + (int) Math.floorMod(Mth.getSeed(context.getClickedPos()), 4L);
        return state.setValue(VARIANT, variant);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(VARIANT) - 1).get(state.getValue(FACING));
    }
}
