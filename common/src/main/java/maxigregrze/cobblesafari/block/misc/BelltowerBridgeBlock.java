package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.ApparentState;
import maxigregrze.cobblesafari.block.base.BlockShapeUtils;
import maxigregrze.cobblesafari.block.base.ConnectedModelBlock;
import maxigregrze.cobblesafari.block.base.Waterlogged;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Suspension bridge: a deck that hangs off the side of an anchor block and chains away from it in
 * a straight line, sagging in the middle. Its model comes from two derived properties - where it
 * sits in its chain ({@link BridgeSegment}) and which parallel lines flank it ({@link BridgeLine},
 * texture only) - while {@code FACING} is fixed at placement and never recomputed.
 *
 * <p>{@code FACING} points <em>away</em> from the anchor, i.e. the direction the chain runs. The
 * six supplied models are authored with that direction at NORTH, so the blockstate {@code y}
 * rotation and {@link BlockShapeUtils}'s convention line up with no offset.</p>
 *
 * <p>Losing the anchor kills the whole run: each block dies, which removes the anchor of the next
 * one, and so on - the wall-sign mechanism, with no bookkeeping of its own.</p>
 */
public class BelltowerBridgeBlock extends ConnectedModelBlock implements SimpleWaterloggedBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BridgeSegment> SEGMENT = EnumProperty.create("segment", BridgeSegment.class);
    public static final EnumProperty<BridgeLine> LINE = EnumProperty.create("line", BridgeLine.class);

    /**
     * Hard cap on every chain walk. Placement already refuses to go further, but {@code /setblock},
     * {@code /fill} and structures do not go through placement: without a cap, one neighbour update
     * on a pathological run would sweep an unbounded stretch of the world. Beyond the cap a block
     * simply reads as {@link BridgeSegment#MIDDLE} - ugly, but bounded.
     */
    public static final int MAX_LENGTH = 32;

    /**
     * Bottom Y, in pixels, of each of the four 4-pixel-deep slabs that make up every model, in
     * order z 0-4 / 4-8 / 8-12 / 12-16. Read off the supplied Blockbench files: all six silhouettes
     * share one skeleton and differ only by these altitudes, which is what draws the sag.
     */
    private static final Map<BridgeSegment, int[]> SLAB_BOTTOMS = slabBottoms();

    /** 6 silhouettes x 4 facings. LINE is not an index: swapping the texture moves no voxel. */
    private static final Map<BridgeSegment, Map<Direction, VoxelShape>> SHAPES = buildShapes();

    public BelltowerBridgeBlock(Properties properties) {
        super(properties, Settings.builder()
                .collision(Collision.SHAPE)
                .emptyOcclusion()
                .build());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SEGMENT, BridgeSegment.SINGLE)
                .setValue(LINE, BridgeLine.SOLO)
                .setValue(Waterlogged.WATERLOGGED, Boolean.FALSE));
    }

    private static Map<BridgeSegment, int[]> slabBottoms() {
        Map<BridgeSegment, int[]> map = new EnumMap<>(BridgeSegment.class);
        map.put(BridgeSegment.SINGLE, new int[]{12, 11, 11, 12});
        map.put(BridgeSegment.DOUBLE_START, new int[]{10, 10, 11, 12});
        map.put(BridgeSegment.DOUBLE_END, new int[]{12, 11, 10, 10});
        map.put(BridgeSegment.START, new int[]{9, 9, 10, 12});
        map.put(BridgeSegment.MIDDLE, new int[]{8, 8, 8, 8});
        map.put(BridgeSegment.END, new int[]{12, 10, 9, 9});
        return Map.copyOf(map);
    }

    private static Map<BridgeSegment, Map<Direction, VoxelShape>> buildShapes() {
        Map<BridgeSegment, Map<Direction, VoxelShape>> table = new EnumMap<>(BridgeSegment.class);
        for (BridgeSegment segment : BridgeSegment.values()) {
            table.put(segment, BlockShapeUtils.precompute(slabsOf(segment), Direction.NORTH));
        }
        return Map.copyOf(table);
    }

    /**
     * The four slabs of one silhouette. X spans the full cell: the 0.1px overhang the models carry
     * is anti z-fighting between adjacent lines and has no business in a hitbox.
     */
    private static VoxelShape slabsOf(BridgeSegment segment) {
        int[] y = SLAB_BOTTOMS.get(segment);
        return Shapes.or(
                Block.box(0, y[0], 0, 16, y[0] + 4.0, 4),
                Block.box(0, y[1], 4, 16, y[1] + 4.0, 8),
                Block.box(0, y[2], 8, 16, y[2] + 4.0, 12),
                Block.box(0, y[3], 12, 16, y[3] + 4.0, 16)).optimize();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(BelltowerBridgeBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SEGMENT, LINE, Waterlogged.WATERLOGGED);
    }

    @Override
    protected VoxelShape shapeFor(BlockState state) {
        return SHAPES.get(state.getValue(SEGMENT)).get(state.getValue(FACING));
    }

    // ------------------------------------------------------------------ derivation

    /** True when {@code state} is a bridge running in {@code facing}. Direction is strict. */
    public static boolean isBridgeFacing(BlockState state, Direction facing) {
        return state.getBlock() instanceof BelltowerBridgeBlock && state.getValue(FACING) == facing;
    }

    /**
     * How many same-facing bridges stand between {@code pos} and its anchor. 0 means {@code pos}
     * touches the anchor. Saturates at {@link #MAX_LENGTH}.
     */
    private static int indexInLine(BlockGetter level, BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < MAX_LENGTH; i++) {
            cursor.move(back);
            if (!isBridgeFacing(ApparentState.at(level, cursor), facing)) {
                return i;
            }
        }
        return MAX_LENGTH;
    }

    /** How many same-facing bridges continue past {@code pos}. Saturates at {@link #MAX_LENGTH}. */
    private static int forwardCount(BlockGetter level, BlockPos pos, Direction facing) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < MAX_LENGTH; i++) {
            cursor.move(facing);
            if (!isBridgeFacing(ApparentState.at(level, cursor), facing)) {
                return i;
            }
        }
        return MAX_LENGTH;
    }

    /**
     * Whether the parallel line through {@code side} covers exactly the same span as ours.
     * Comparing (index, length) is equivalent to comparing the two extents, since the lines run
     * parallel at the same height - which is what makes "only connect lines of equal length" exact.
     * The cheap test comes first so an unequal line costs one scan, not two.
     */
    private static boolean lineMatches(BlockGetter level, BlockPos side, Direction facing, int index, int length) {
        if (!isBridgeFacing(ApparentState.at(level, side), facing)) {
            return false;
        }
        int sideIndex = indexInLine(level, side, facing);
        if (sideIndex != index) {
            return false;
        }
        return sideIndex + 1 + forwardCount(level, side, facing) == length;
    }

    @Override
    protected BlockState computeState(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        int index = indexInLine(level, pos, facing);
        int length = index + 1 + forwardCount(level, pos, facing);
        boolean left = lineMatches(level, pos.relative(facing.getCounterClockWise()), facing, index, length);
        boolean right = lineMatches(level, pos.relative(facing.getClockWise()), facing, index, length);
        return state
                .setValue(SEGMENT, BridgeSegment.of(index, length))
                .setValue(LINE, BridgeLine.of(left, right));
    }

    // ------------------------------------------------------------------ survival

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos anchorPos = pos.relative(facing.getOpposite());
        BlockState anchor = level.getBlockState(anchorPos);
        // A same-facing bridge relays the anchoring; anything else must present a sturdy face.
        return isBridgeFacing(anchor, facing) || anchor.isFaceSturdy(level, anchorPos, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        Waterlogged.scheduleTick(state, level, pos);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Covers raw setBlock (worldgen, /fill, structures), which never consults canSurvive.
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        super.tick(state, level, pos, random);
    }

    // ------------------------------------------------------------------ placement

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        // Makes getClickedPos() resolve to the bridge itself, so BelltowerBridgeBlockItem can
        // recognise "clicked a bridge" and walk to the end of the run. Scaffolding does the same.
        return context.getItemInHand().is(this.asItem());
    }

    /**
     * The direction a placement at {@code context} would run in, or null when none can be deduced.
     * Clicking a bridge always yields that bridge's own direction - the item has already relocated
     * the placement to the end of its chain - which is what makes a lateral, direction-changing
     * placement structurally impossible rather than merely refused.
     */
    @Nullable
    private static Direction placementFacing(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        BlockPos against = context.getClickedPos().relative(clickedFace.getOpposite());
        BlockState againstState = context.getLevel().getBlockState(against);
        if (againstState.getBlock() instanceof BelltowerBridgeBlock) {
            return againstState.getValue(FACING);
        }
        return clickedFace.getAxis().isHorizontal() ? clickedFace : null;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = placementFacing(context);
        if (facing == null) {
            return null;
        }
        BlockState base = this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(Waterlogged.WATERLOGGED, Waterlogged.atPlacement(context));
        if (!canSurvive(base, context.getLevel(), context.getClickedPos())) {
            return null;
        }
        return computeState(base, context.getLevel(), context.getClickedPos());
    }

    // ------------------------------------------------------------------ boilerplate

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Waterlogged.fluidState(state, super.getFluidState(state));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
