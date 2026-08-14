package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.BlockShapeUtils;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Orientable 3-cell vertical multiblock (large neon = wall-mounted; lamp post = ground).
 * The <em>placed</em> cell is the BOTTOM; CENTER goes at y+1 and TOP at y+2.
 * Lighting is configured per part through {@code Properties.lightLevel} at registration.
 *
 * <p>Shapes are given per cell through {@link TriShapes}, authored for a single facing and rotated
 * to the other three like every other orientable block ({@link BlockShapeUtils#precompute}). The
 * collision set is separate from the outline set so a column can be selected as a whole while only
 * part of it is solid (see the large flag: neon in the outline, arms only in the collision).</p>
 */
public class HyperspaceTriBlock extends HorizontalDirectionalBlock {

    public static final EnumProperty<HyperspaceTriPart> PART = EnumProperty.create("part", HyperspaceTriPart.class);

    /** One {@link VoxelShape} per cell of the column, all authored for the same facing. */
    public record TriShapes(VoxelShape bottom, VoxelShape center, VoxelShape top) {

        /** Same shape for the three cells. */
        public static TriShapes uniform(VoxelShape shape) {
            return new TriShapes(shape, shape, shape);
        }

        public VoxelShape get(HyperspaceTriPart part) {
            return switch (part) {
                case BOTTOM -> bottom;
                case CENTER -> center;
                case TOP -> top;
            };
        }
    }

    private final boolean wallMounted;
    private final Direction authoredFacing;
    private final TriShapes outline;
    @Nullable
    private final TriShapes collision;
    private final Map<HyperspaceTriPart, Map<Direction, VoxelShape>> outlineByFacing;
    @Nullable
    private final Map<HyperspaceTriPart, Map<Direction, VoxelShape>> collisionByFacing;

    private static boolean breaking = false;

    public HyperspaceTriBlock(Properties properties, boolean wallMounted, Direction authoredFacing,
                              TriShapes outline, @Nullable TriShapes collision) {
        super(properties);
        this.wallMounted = wallMounted;
        this.authoredFacing = authoredFacing;
        this.outline = outline;
        this.collision = collision;
        this.outlineByFacing = precompute(outline, authoredFacing);
        this.collisionByFacing = collision != null ? precompute(collision, authoredFacing) : null;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, HyperspaceTriPart.BOTTOM));
    }

    private static Map<HyperspaceTriPart, Map<Direction, VoxelShape>> precompute(TriShapes shapes, Direction authored) {
        Map<HyperspaceTriPart, Map<Direction, VoxelShape>> map = new EnumMap<>(HyperspaceTriPart.class);
        for (HyperspaceTriPart part : HyperspaceTriPart.values()) {
            map.put(part, BlockShapeUtils.precompute(shapes.get(part), authored));
        }
        return map;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new HyperspaceTriBlock(props, this.wallMounted, this.authoredFacing,
                this.outline, this.collision));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos(); // BOTTOM cell
        Level level = context.getLevel();
        Direction facing;
        if (wallMounted) {
            Direction clicked = context.getClickedFace();
            facing = clicked.getAxis().isHorizontal() ? clicked : context.getHorizontalDirection().getOpposite();
        } else {
            facing = context.getHorizontalDirection().getOpposite();
        }
        if (!level.getBlockState(pos.above()).canBeReplaced(context)
                || !level.getBlockState(pos.above(2)).canBeReplaced(context)) {
            return null;
        }
        if (!supportOk(level, pos, facing)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(PART, HyperspaceTriPart.BOTTOM);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || state.getValue(PART) != HyperspaceTriPart.BOTTOM) {
            return;
        }
        Direction facing = state.getValue(FACING);
        level.setBlock(pos.above(),
                this.defaultBlockState().setValue(FACING, facing).setValue(PART, HyperspaceTriPart.CENTER), Block.UPDATE_ALL);
        level.setBlock(pos.above(2),
                this.defaultBlockState().setValue(FACING, facing).setValue(PART, HyperspaceTriPart.TOP), Block.UPDATE_ALL);
    }

    /** Support of the whole column, evaluated from its BOTTOM cell. */
    private boolean supportOk(LevelReader level, BlockPos bottomPos, Direction facing) {
        if (!wallMounted) {
            BlockPos below = bottomPos.below();
            return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        }
        Direction back = facing.getOpposite();
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos cell = bottomPos.above(dy);
            BlockPos support = cell.relative(back);
            if (!level.getBlockState(support).isFaceSturdy(level, support, facing)) {
                return false;
            }
        }
        return true;
    }

    private BlockPos bottomOf(BlockPos pos, BlockState state) {
        return switch (state.getValue(PART)) {
            case BOTTOM -> pos;
            case CENTER -> pos.below();
            case TOP -> pos.below(2);
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return switch (state.getValue(PART)) {
            // Anchor: depends only on its own support (so it survives the moment it is placed).
            case BOTTOM -> supportOk(level, pos, state.getValue(FACING));
            case CENTER -> isPart(level, pos.below(), HyperspaceTriPart.BOTTOM);
            case TOP -> isPart(level, pos.below(), HyperspaceTriPart.CENTER);
        };
    }

    private boolean isPart(LevelReader level, BlockPos pos, HyperspaceTriPart part) {
        BlockState s = level.getBlockState(pos);
        return s.is(this) && s.getValue(PART) == part;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        breakStructure(level, pos, state);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            breakStructure(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void breakStructure(Level level, BlockPos pos, BlockState state) {
        if (breaking || level.isClientSide()) {
            return;
        }
        breaking = true;
        BlockPos bottom = bottomOf(pos, state);
        for (BlockPos p : new BlockPos[]{bottom, bottom.above(), bottom.above(2)}) {
            if (level.getBlockState(p).is(this)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        breaking = false;
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
        return shapeFor(outlineByFacing, outline, state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        TriShapes authored = this.collision;
        if (collisionByFacing == null || authored == null) {
            return Shapes.empty();
        }
        return shapeFor(collisionByFacing, authored, state);
    }

    private static VoxelShape shapeFor(Map<HyperspaceTriPart, Map<Direction, VoxelShape>> byFacing,
                                       TriShapes authored, BlockState state) {
        HyperspaceTriPart part = state.getValue(PART);
        return byFacing.get(part).getOrDefault(state.getValue(FACING), authored.get(part));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
