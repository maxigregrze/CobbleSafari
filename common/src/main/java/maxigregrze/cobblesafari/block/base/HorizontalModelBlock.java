package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Shared base for cardinal (N/E/S/W) blocks that are just a custom model + orientation,
 * with no bespoke behaviour. Everything is driven by {@link Settings}: shape, the facing
 * the shape is authored for, whether the shape rotates with FACING, the default facing,
 * the support requirement, the collision mode, occlusion, and an optional shared
 * description id.
 *
 * <p>Blocks that need a real mechanic (e.g. an effect on entities) may extend this and
 * override only the relevant hook - see {@code PileBlock} overriding {@code entityInside}.
 * Never subclass merely to set a shape or facing; pass {@link Settings} instead.</p>
 */
public class HorizontalModelBlock extends HorizontalDirectionalBlock {

    /** What the block must be attached to in order to survive. */
    public enum Support { NONE, GROUND, WALL, CEILING }

    /** Collision behaviour. */
    public enum Collision { SHAPE, NONE, FULL }

    /** Immutable configuration for a {@link HorizontalModelBlock}. Build via {@link #builder()}. */
    public record Settings(
            @Nullable VoxelShape shape,
            @Nullable VoxelShape collisionShape,
            Direction authoredFacing,
            boolean rotateShape,
            Direction defaultFacing,
            Support support,
            Collision collision,
            boolean emptyOcclusion,
            @Nullable String descriptionId,
            @Nullable Double seatHeight) {

        public static Builder builder() {
            return new Builder();
        }

        /** Plain oriented full cube (no custom shape). */
        public static Settings cube() {
            return builder().build();
        }

        public static final class Builder {
            private VoxelShape shape = null;
            private VoxelShape collisionShape = null;
            private Direction authoredFacing = Direction.NORTH;
            private boolean rotateShape = false;
            private Direction defaultFacing = Direction.NORTH;
            private Support support = Support.NONE;
            private Collision collision = Collision.SHAPE;
            private boolean emptyOcclusion = false;
            private String descriptionId = null;
            private Double seatHeight = null;

            public Builder shape(VoxelShape shape) { this.shape = shape; return this; }
            /** Collision box, when it must be narrower than the selection box (defaults to {@link #shape}). */
            public Builder collisionShape(VoxelShape shape) { this.collisionShape = shape; return this; }
            public Builder authoredFacing(Direction facing) { this.authoredFacing = facing; return this; }
            public Builder rotateShape() { this.rotateShape = true; return this; }
            public Builder defaultFacing(Direction facing) { this.defaultFacing = facing; return this; }
            public Builder support(Support support) { this.support = support; return this; }
            public Builder collision(Collision collision) { this.collision = collision; return this; }
            public Builder emptyOcclusion() { this.emptyOcclusion = true; return this; }
            public Builder descriptionId(String id) { this.descriptionId = id; return this; }
            /** Makes the block a seat: an empty-handed right-click sits the player {@code heightPx} above its floor. */
            public Builder seat(double heightPx) { this.seatHeight = heightPx; return this; }

            public Settings build() {
                return new Settings(shape, collisionShape, authoredFacing, rotateShape, defaultFacing,
                        support, collision, emptyOcclusion, descriptionId, seatHeight);
            }
        }
    }

    protected final Settings settings;
    private final VoxelShape baseShape;
    @Nullable
    private final Map<Direction, VoxelShape> rotatedShapes;
    @Nullable
    private final VoxelShape baseCollisionShape;
    @Nullable
    private final Map<Direction, VoxelShape> rotatedCollisionShapes;

    public HorizontalModelBlock(Properties properties, Settings settings) {
        super(properties);
        this.settings = settings;
        this.baseShape = settings.shape() != null ? settings.shape() : Shapes.block();
        this.rotatedShapes = (settings.rotateShape() && settings.shape() != null)
                ? BlockShapeUtils.precompute(settings.shape(), settings.authoredFacing())
                : null;
        this.baseCollisionShape = settings.collisionShape();
        this.rotatedCollisionShapes = (settings.rotateShape() && settings.collisionShape() != null)
                ? BlockShapeUtils.precompute(settings.collisionShape(), settings.authoredFacing())
                : null;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, settings.defaultFacing()));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new HorizontalModelBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing;
        if (settings.support() == Support.WALL) {
            Direction clicked = context.getClickedFace();
            facing = clicked.getAxis().isHorizontal() ? clicked : context.getHorizontalDirection().getOpposite();
        } else {
            facing = context.getHorizontalDirection().getOpposite();
        }
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return switch (settings.support()) {
            case NONE -> true;
            case GROUND -> {
                BlockPos below = pos.below();
                yield level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
            }
            case WALL -> {
                Direction back = state.getValue(FACING).getOpposite();
                BlockPos supportPos = pos.relative(back);
                yield level.getBlockState(supportPos).isFaceSturdy(level, supportPos, back.getOpposite());
            }
            // Literally vanilla LanternBlock.canSurvive in its hanging form: a chain, a fence or a
            // full block carries the block, a bottom slab or a pressure plate does not.
            case CEILING -> Block.canSupportCenter(level, pos.above(), Direction.DOWN);
        };
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (settings.support() != Support.NONE && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * The way a sitter looks. Seat models are drawn with their backrest on the north side, so they
     * face south as authored; that front turns with FACING exactly like the model does.
     */
    public Direction seatFront(BlockState state) {
        Direction front = Direction.SOUTH;
        Direction turned = settings.authoredFacing();
        Direction facing = state.getValue(FACING);
        while (turned != facing) {
            turned = turned.getClockWise();
            front = front.getClockWise();
        }
        return front;
    }

    /** True when the block was built with {@link Settings.Builder#seat}. */
    public boolean isSeat() {
        return settings.seatHeight() != null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (settings.seatHeight() != null) {
            return SeatEntity.trySit(level, pos, state, player, settings.seatHeight());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
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
        if (rotatedShapes != null) {
            return rotatedShapes.getOrDefault(state.getValue(FACING), baseShape);
        }
        return baseShape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (settings.collision()) {
            case SHAPE -> {
                if (baseCollisionShape == null) {
                    yield getShape(state, level, pos, context);
                }
                yield rotatedCollisionShapes != null
                        ? rotatedCollisionShapes.getOrDefault(state.getValue(FACING), baseCollisionShape)
                        : baseCollisionShape;
            }
            case NONE -> Shapes.empty();
            case FULL -> Shapes.block();
        };
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return settings.emptyOcclusion() ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public String getDescriptionId() {
        return settings.descriptionId() != null ? settings.descriptionId() : super.getDescriptionId();
    }
}
