package maxigregrze.cobblesafari.block.dungeon;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Ultra Wormhole device: a 2 × 2 multiblock (two cells wide, two tall) the structure author
 * places by hand in a parent dungeon's wormhole room. Right-clicking any cell opens the Ultra Warp
 * Ride (action plans 183 § 4.1 and 201 § 13).
 *
 * <p>The authored models ({@code ultra_entranceportal_down} / {@code _up}, a plane on the
 * {@code z = 8} slice spanning {@code x = -16} to {@code 32}) are centred on one column. To centre
 * the device on a two-wide structure they are drawn half a block towards the right cell by
 * {@code UltraEntrancePortalBlockEntityRenderer}, from a block entity on the main cell: a model
 * JSON cannot reach that far. Every cell is therefore {@link RenderShape#INVISIBLE}.</p>
 *
 * <p>"Right" is {@code FACING.getClockWise()}, the axis the authored plane runs along. The main
 * cell is {@code LOWER} + {@code LEFT}: it is the position {@link SubDungeonManager#openDevice}
 * resolves and the key of the once-per-device entry.</p>
 *
 * <p><b>The block entity carries no state.</b> There is no confirm payload: the device opens on a
 * vanilla interaction, so reach is server-enforced and the position never comes from the client.
 * The once-per-run entry lives on the parent instance record.</p>
 */
public class UltraEntrancePortalBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<UltraEntrancePortalBlock> CODEC = simpleCodec(UltraEntrancePortalBlock::new);

    public enum Part implements StringRepresentable {
        LEFT, RIGHT;

        @Override
        public String getSerializedName() {
            return this == LEFT ? "left" : "right";
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    /** Shift of the drawn model and of the post, in model units, towards the right cell. */
    private static final double SHIFT = 8.0;

    /**
     * The post, taken from the three <b>solid</b> elements of the authored models (the two others
     * are the flat swirling plane). Authored north-facing, in model units, before the shift.
     */
    private static final double[][] POST_LOWER = {
            {7.0, 2.0, 7.5, 9.0, 12.0, 8.5},
            {6.0, 12.0, 7.5, 10.0, 15.0, 8.5},
            {5.0, 15.0, 7.5, 11.0, 16.0, 8.5}
    };
    private static final double[][] POST_UPPER = {
            {7.0, 4.0, 7.5, 9.0, 14.0, 8.5},
            {6.0, 1.0, 7.5, 10.0, 4.0, 8.5},
            {5.0, 0.0, 7.5, 11.0, 1.0, 8.5}
    };

    private static final Map<Direction, VoxelShape> SHAPES_LOWER_LEFT = buildShapes(POST_LOWER, 0.0);
    private static final Map<Direction, VoxelShape> SHAPES_LOWER_RIGHT = buildShapes(POST_LOWER, -16.0);
    private static final Map<Direction, VoxelShape> SHAPES_UPPER_LEFT = buildShapes(POST_UPPER, 0.0);
    private static final Map<Direction, VoxelShape> SHAPES_UPPER_RIGHT = buildShapes(POST_UPPER, -16.0);

    /**
     * Shifts the post by {@link #SHIFT}, keeps the slice that falls in this cell ({@code cellOffset}
     * 0 for the left cell, -16 for the right one), then turns it to each horizontal facing.
     */
    private static Map<Direction, VoxelShape> buildShapes(double[][] boxes, double cellOffset) {
        Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                double x0 = Math.max(0.0, b[0] + SHIFT + cellOffset);
                double x1 = Math.min(16.0, b[3] + SHIFT + cellOffset);
                if (x1 > x0) {
                    shape = Shapes.or(shape, rotate(new double[]{x0, b[1], b[2], x1, b[4], b[5]}, facing));
                }
            }
            byFacing.put(facing, shape);
        }
        return byFacing;
    }

    /** A quarter turn about the block's vertical centre line, matching the blockstate's y rotation. */
    private static VoxelShape rotate(double[] b, Direction facing) {
        double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        return switch (facing) {
            case EAST -> Block.box(16.0 - z1, y0, x0, 16.0 - z0, y1, x1);
            case SOUTH -> Block.box(16.0 - x1, y0, 16.0 - z1, 16.0 - x0, y1, 16.0 - z0);
            case WEST -> Block.box(z0, y0, 16.0 - x1, z1, y1, 16.0 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    public UltraEntrancePortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                .setValue(PART, Part.LEFT));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.DOUBLE_BLOCK_HALF, PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    public static boolean isMain(BlockState state) {
        return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                && state.getValue(PART) == Part.LEFT;
    }

    /** The direction from the left column to the right one. */
    public static Direction rightOf(Direction facing) {
        return facing.getClockWise();
    }

    /** The main cell of the device {@code state} at {@code pos} belongs to. */
    public static BlockPos mainPos(BlockPos pos, BlockState state) {
        BlockPos p = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        return state.getValue(PART) == Part.RIGHT ? p.relative(rightOf(state.getValue(FACING)).getOpposite()) : p;
    }

    /**
     * Outline and collision both follow the post (shifted to the seam of the two columns). The
     * portal plane it holds up stays empty air.
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean upper = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
        boolean right = state.getValue(PART) == Part.RIGHT;
        Map<Direction, VoxelShape> shapes = upper
                ? (right ? SHAPES_UPPER_RIGHT : SHAPES_UPPER_LEFT)
                : (right ? SHAPES_LOWER_RIGHT : SHAPES_LOWER_LEFT);
        return shapes.getOrDefault(state.getValue(FACING), Shapes.block());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos rightPos = pos.relative(rightOf(facing));
        if (pos.getY() >= level.getMaxBuildHeight() - 1
                || !level.getBlockState(pos.above()).canBeReplaced(context)
                || !level.getBlockState(rightPos).canBeReplaced(context)
                || !level.getBlockState(rightPos.above()).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                .setValue(PART, Part.LEFT);
    }

    /**
     * Places the three other cells. {@code UPDATE_KNOWN_SHAPE} keeps the cells already placed from
     * running their partner check against a cell that does not exist yet.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!isMain(state)) {
            return;
        }
        int flags = Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE;
        BlockPos rightPos = pos.relative(rightOf(state.getValue(FACING)));
        BlockState upper = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        level.setBlock(pos.above(), upper, flags);
        level.setBlock(rightPos, state.setValue(PART, Part.RIGHT), flags);
        level.setBlock(rightPos.above(), upper.setValue(PART, Part.RIGHT), flags);
    }

    /**
     * Each cell checks the partners it touches: its vertical twin, and for the lower cells the
     * other lower cell. Losing any cell therefore removes the whole device, whatever removed it.
     * Unlike {@code HyperspaceDoubleBlock} the lower cells need no floor: the device is baked into
     * a structure and may well hang over a void.
     */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!partnerOk(state, direction, neighborState)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private boolean partnerOk(BlockState state, Direction direction, BlockState neighbor) {
        DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        Part part = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        Direction towardsOther = part == Part.LEFT ? rightOf(facing) : rightOf(facing).getOpposite();
        if (direction == (half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN)) {
            return neighbor.is(this)
                    && neighbor.getValue(PART) == part
                    && neighbor.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != half;
        }
        if (half == DoubleBlockHalf.LOWER && direction == towardsOther) {
            return neighbor.is(this)
                    && neighbor.getValue(PART) != part
                    && neighbor.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
        }
        return true;
    }

    /** Every cell opens the ride on the main cell, the only position {@code openDevice} resolves. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        SubDungeonManager.openDevice(serverPlayer, mainPos(pos, state));
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isMain(state) ? new UltraEntrancePortalBlockEntity(pos, state) : null;
    }
}
