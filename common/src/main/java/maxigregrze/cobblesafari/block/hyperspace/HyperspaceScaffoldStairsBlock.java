package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Orientable "scaffolding stairs": like vanilla stairs but with 4 steps. Collision is a 4-step
 * staircase (ascending toward {@code FACING}); the visual is the supplied custom model.
 */
public class HyperspaceScaffoldStairsBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<HyperspaceScaffoldStairsBlock> CODEC = simpleCodec(HyperspaceScaffoldStairsBlock::new);

    // Authored for NORTH: tallest at the front (z=0), descending toward the back (z=16).
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(0, 0, 12, 16, 4, 16),
            Block.box(0, 0, 8, 16, 8, 12),
            Block.box(0, 0, 4, 16, 12, 8),
            Block.box(0, 0, 0, 16, 16, 4));

    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public HyperspaceScaffoldStairsBlock(Properties properties) {
        super(properties);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            // Rotate so the staircase lines up with the rendered model on every facing.
            shapes.put(d, HyperspaceShapes.rotateForFacing(NORTH_SHAPE, d));
        }
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.getOrDefault(state.getValue(FACING), NORTH_SHAPE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
