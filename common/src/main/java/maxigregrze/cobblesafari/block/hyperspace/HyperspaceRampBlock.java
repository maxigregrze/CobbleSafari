package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Climbable ramp that spans two cells (climb two blocks at once). The 32 px model is rendered by the
 * LOWER half; the UPPER half is an invisible climbable filler. Climbing comes from {@code #minecraft:climbable}.
 */
public class HyperspaceRampBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<HyperspaceRampBlock> CODEC = simpleCodec(HyperspaceRampBlock::new);

    // Per-cell staircase hitbox, authored from the supplied Blockbench hitbox models and then
    // flipped 180° around Y (N<->S, E<->W) — i.e. the boxes below are the provided shapes mirrored
    // on both horizontal axes. The model is flipped to match via the blockstate (+180° rotation).
    private static final VoxelShape LOWER_NORTH_SHAPE = Shapes.or(
            Block.box(0, 0, 3, 16, 3, 16),
            Block.box(0, 3, 4, 16, 6, 16),
            Block.box(0, 6, 5, 16, 9, 16),
            Block.box(0, 9, 6, 16, 12, 16),
            Block.box(0, 12, 7, 16, 15, 16),
            Block.box(0, 15, 8, 16, 16, 16));
    private static final VoxelShape UPPER_NORTH_SHAPE = Shapes.or(
            Block.box(0, 0, 8, 16, 1, 16),
            Block.box(0, 1, 9, 16, 3, 16),
            Block.box(0, 3, 10, 16, 6, 16),
            Block.box(0, 6, 11, 16, 8, 16),
            Block.box(0, 8, 12, 16, 11, 16),
            Block.box(0, 11, 13, 16, 14, 16));

    private final Map<Direction, VoxelShape> lowerShapes = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> upperShapes = new EnumMap<>(Direction.class);

    public HyperspaceRampBlock(Properties properties) {
        super(properties);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            lowerShapes.put(d, HyperspaceShapes.rotateForFacing(LOWER_NORTH_SHAPE, d));
            upperShapes.put(d, HyperspaceShapes.rotateForFacing(UPPER_NORTH_SHAPE, d));
        }
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.DOUBLE_BLOCK_HALF);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (pos.getY() < level.getMaxBuildHeight() - 1
                && level.getBlockState(pos.above()).canBeReplaced(context)) {
            BlockState s = this.defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            if (canSurvive(s, level, pos)) {
                return s;
            }
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(pos.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            // Wall behind, solid ground below, or another ramp below (bottom anchor).
            BlockPos back = pos.relative(facing.getOpposite());
            if (level.getBlockState(back).isFaceSturdy(level, back, facing)) {
                return true;
            }
            if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                return true;
            }
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
        }
        BlockState below = level.getBlockState(pos.below());
        return below.is(this)
                && below.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                && below.getValue(FACING) == facing;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        BlockPos other = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        if (level.getBlockState(other).is(this)) {
            level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
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
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Staircase on both halves: the player walks up the steps.
        return shapeFor(state);
    }

    private VoxelShape shapeFor(BlockState state) {
        boolean lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
        Map<Direction, VoxelShape> map = lower ? lowerShapes : upperShapes;
        return map.getOrDefault(state.getValue(FACING), lower ? LOWER_NORTH_SHAPE : UPPER_NORTH_SHAPE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Only the LOWER half renders the 32 px model; the UPPER half is an invisible climbable filler.
        return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }
}
