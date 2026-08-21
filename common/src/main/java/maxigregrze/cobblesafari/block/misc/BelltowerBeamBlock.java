package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.BlockShapeUtils;
import maxigregrze.cobblesafari.block.base.Grid3x3MultiblockBlock;
import maxigregrze.cobblesafari.block.base.Waterlogged;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Belltower beam: a 3x3 horizontal multiblock on a single Y layer, placed by its centre cell
 * ({@link BeamPart#P5}), orientable in the four cardinal directions, drawing one of two model sets
 * according to the parity of its Y.
 *
 * <p>{@code PART} is the cell in the structure's own frame and {@code FACING} the structure's
 * orientation, so rotating only ever touches FACING. {@code ALT} is not a player choice: it is a
 * pure function of the Y coordinate, kept in the blockstate because the blockstate JSON cannot
 * read a coordinate.</p>
 */
public class BelltowerBeamBlock extends Grid3x3MultiblockBlock implements SimpleWaterloggedBlock {

    public static final EnumProperty<BeamPart> PART = EnumProperty.create("part", BeamPart.class);
    public static final BooleanProperty ALT = BooleanProperty.create("alt");

    /**
     * The vanilla horizontal facing property, borrowed rather than re-created: this block extends
     * {@link Grid3x3MultiblockBlock} and so cannot inherit it from
     * {@code HorizontalDirectionalBlock}, but sharing the same property instance keeps the
     * blockstate key {@code facing} and the rotation semantics identical to every other block.
     */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /**
     * Selection box per cell, authored for {@code facing=north}: the largest cuboid of the matching
     * model. The nine {@code _alt} models each hold a single cuboid identical to the one below, so
     * ALT does not enter the lookup - one table, not two.
     */
    private static final Map<BeamPart, VoxelShape> NORTH_SHAPES = northShapes();

    /** The four rotations of each, through the mod's shared convention. */
    private static final Map<BeamPart, Map<Direction, VoxelShape>> SHAPES = rotateAll();

    public BelltowerBeamBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BeamPart.P5)
                .setValue(ALT, Boolean.FALSE)
                .setValue(Waterlogged.WATERLOGGED, Boolean.FALSE));
    }

    private static Map<BeamPart, VoxelShape> northShapes() {
        Map<BeamPart, VoxelShape> map = new EnumMap<>(BeamPart.class);
        map.put(BeamPart.P1, Block.box(10, 0, 10, 16, 16, 16));
        map.put(BeamPart.P2, Block.box(0, 0, 10, 16, 16, 16));
        map.put(BeamPart.P3, Block.box(0, 0, 10, 6, 16, 16));
        map.put(BeamPart.P4, Block.box(10, 0, 0, 16, 16, 16));
        map.put(BeamPart.P5, Block.box(0, 0, 0, 16, 16, 16));
        map.put(BeamPart.P6, Block.box(0, 0, 0, 6, 16, 16));
        map.put(BeamPart.P7, Block.box(10, 0, 0, 16, 16, 6));
        map.put(BeamPart.P8, Block.box(0, 0, 0, 16, 16, 6));
        map.put(BeamPart.P9, Block.box(0, 0, 0, 6, 16, 6));
        return Map.copyOf(map);
    }

    private static Map<BeamPart, Map<Direction, VoxelShape>> rotateAll() {
        Map<BeamPart, Map<Direction, VoxelShape>> map = new EnumMap<>(BeamPart.class);
        for (Map.Entry<BeamPart, VoxelShape> entry : NORTH_SHAPES.entrySet()) {
            map.put(entry.getKey(), BlockShapeUtils.precompute(entry.getValue(), Direction.NORTH));
        }
        return Map.copyOf(map);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(BelltowerBeamBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, ALT, Waterlogged.WATERLOGGED);
    }

    // ------------------------------------------------------------------ placement and Y parity

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        return state
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(PART, BeamPart.P5)
                .setValue(ALT, isAlt(context.getClickedPos()))
                .setValue(Waterlogged.WATERLOGGED, Waterlogged.atPlacement(context));
    }

    /**
     * Re-anchors ALT on raw writes. /setblock, /fill, /clone, worldgen and structures never go
     * through getStateForPlacement, so a beam saved at an even Y and replaced at an odd one would
     * otherwise keep the wrong model set. Same mechanism, and same guard against looping, as
     * {@code ConnectedModelBlock.refreshOnPlace}.
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean alt = isAlt(pos);
        if (Boolean.TRUE.equals(state.getValue(ALT)) != alt) {
            level.setBlock(pos, state.setValue(ALT, alt), Block.UPDATE_ALL);
        }
    }

    /** Default model set on even Y, {@code _alt} on odd Y. */
    private static boolean isAlt(BlockPos pos) {
        return (pos.getY() & 1) != 0;
    }

    // ------------------------------------------------------------------ multiblock hooks

    @Override
    protected boolean isCenter(BlockState state) {
        return state.getValue(PART) == BeamPart.P5;
    }

    @Override
    protected boolean isPartOfSame(BlockState state) {
        return state.is(this);
    }

    @Override
    protected BlockPos centerOf(BlockState state, BlockPos pos) {
        BeamPart part = state.getValue(PART);
        int[] world = rotate(part.dx(), part.dz(), state.getValue(FACING));
        return pos.offset(-world[0], 0, -world[1]);
    }

    @Override
    protected BlockState partStateFor(BlockState centerState, Level level, BlockPos partPos, int dx, int dz) {
        int[] local = unrotate(dx, dz, centerState.getValue(FACING));
        // FACING and ALT ride along, so the nine cells cannot disagree. WATERLOGGED does not: the
        // structure is one Y layer wide and each cell answers to the water that was standing in it.
        return centerState
                .setValue(PART, BeamPart.at(local[0], local[1]))
                .setValue(Waterlogged.WATERLOGGED, Waterlogged.atPosition(level, partPos));
    }

    /** Local offset to world offset. north is the identity; east turns (dx, dz) into (-dz, dx). */
    private static int[] rotate(int dx, int dz, Direction facing) {
        return switch (facing) {
            case EAST -> new int[]{-dz, dx};
            case SOUTH -> new int[]{-dx, -dz};
            case WEST -> new int[]{dz, -dx};
            default -> new int[]{dx, dz};
        };
    }

    /** World offset back to local offset. */
    private static int[] unrotate(int dx, int dz, Direction facing) {
        return switch (facing) {
            case EAST -> new int[]{dz, -dx};
            case SOUTH -> new int[]{-dx, -dz};
            case WEST -> new int[]{-dz, dx};
            default -> new int[]{dx, dz};
        };
    }

    // ------------------------------------------------------------------ waterlogging

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Waterlogged.fluidState(state, super.getFluidState(state));
    }

    /**
     * The base deliberately has no {@code updateShape} - cohesion is {@code onRemove}'s job - so
     * this one exists only to let the water spread.
     */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Waterlogged.scheduleTick(state, level, pos);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // ------------------------------------------------------------------ orientation and shape

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
        Map<Direction, VoxelShape> byFacing = SHAPES.get(state.getValue(PART));
        return byFacing.getOrDefault(state.getValue(FACING), NORTH_SHAPES.get(state.getValue(PART)));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // A beam is structural timber: you walk on it.
        return getShape(state, level, pos, context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
