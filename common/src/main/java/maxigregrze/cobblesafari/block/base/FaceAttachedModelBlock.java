package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Shared base for orthonormal blocks: a custom model that attaches to the floor, the
 * ceiling, or one of the four walls (12 orientations: {@code FACE} × {@code FACING}), with
 * no bespoke behaviour. Built on vanilla {@link FaceAttachedHorizontalDirectionalBlock}.
 *
 * <p>Everything is driven by {@link Settings}: the six face shapes, whether wall placement
 * is allowed, the floor/ceiling placement facing convention (the "weed" case), and the
 * collision mode. The wall placement facing and the survival rules are common to the whole
 * family and handled here.</p>
 */
public class FaceAttachedModelBlock extends FaceAttachedHorizontalDirectionalBlock {

    /** Collision behaviour. */
    public enum Collision { SHAPE, FULL }

    /** How a floor/ceiling‑placed block orients horizontally. */
    public enum FloorFacing {
        /** Faces the player (= {@code getHorizontalDirection()}). */
        PLAYER,
        /** Faces away from the player (= {@code getHorizontalDirection().getOpposite()}, vanilla feel). */
        OPPOSITE
    }

    /** Immutable configuration for a {@link FaceAttachedModelBlock}. Build via {@link #builder()}. */
    public record Settings(
            VoxelShape floor,
            VoxelShape ceiling,
            VoxelShape wallNorth,
            VoxelShape wallSouth,
            VoxelShape wallEast,
            VoxelShape wallWest,
            boolean allowWallPlacement,
            boolean allowCeilingPlacement,
            boolean rotateFloorShape,
            boolean requireSupport,
            boolean requireFullBlockSupport,
            FloorFacing floorCeilingFacing,
            Collision collision) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private VoxelShape floor = Shapes.block();
            private VoxelShape ceiling = Shapes.block();
            private VoxelShape wallNorth = Shapes.block();
            private VoxelShape wallSouth = Shapes.block();
            private VoxelShape wallEast = Shapes.block();
            private VoxelShape wallWest = Shapes.block();
            private boolean allowWallPlacement = true;
            private boolean allowCeilingPlacement = true;
            private boolean rotateFloorShape = false;
            private boolean requireSupport = true;
            private boolean requireFullBlockSupport = false;
            private FloorFacing floorCeilingFacing = FloorFacing.OPPOSITE;
            private Collision collision = Collision.SHAPE;

            public Builder shapes(VoxelShape floor, VoxelShape ceiling,
                                  VoxelShape wallNorth, VoxelShape wallSouth,
                                  VoxelShape wallEast, VoxelShape wallWest) {
                this.floor = floor;
                this.ceiling = ceiling;
                this.wallNorth = wallNorth;
                this.wallSouth = wallSouth;
                this.wallEast = wallEast;
                this.wallWest = wallWest;
                return this;
            }

            public Builder noWall() { this.allowWallPlacement = false; return this; }
            /**
             * Turn the floor and ceiling shapes with FACING, as the blockstate turns the model.
             * Only needed when that shape is not radially symmetric - a flat panel, say.
             */
            public Builder rotateFloorShape() { this.rotateFloorShape = true; return this; }
            /** Refuse placement under a block - the plant case, which only grows up or sideways. */
            public Builder noCeiling() { this.allowCeilingPlacement = false; return this; }
            /**
             * Drop the support requirement entirely: the block survives wherever it is put and never
             * pops off. For decorations that read as floating rather than fixed to a surface.
             */
            public Builder noSupport() { this.requireSupport = false; return this; }
            /** Demand a full cube behind the attachment, not merely a sturdy face (no slabs, stairs, fences). */
            public Builder requireFullBlockSupport() { this.requireFullBlockSupport = true; return this; }
            public Builder floorFacing(FloorFacing facing) { this.floorCeilingFacing = facing; return this; }
            public Builder collision(Collision collision) { this.collision = collision; return this; }

            public Settings build() {
                return new Settings(floor, ceiling, wallNorth, wallSouth, wallEast, wallWest,
                        allowWallPlacement, allowCeilingPlacement, rotateFloorShape, requireSupport, requireFullBlockSupport,
                        floorCeilingFacing, collision);
            }
        }
    }

    private final Settings settings;
    /** Floor / ceiling shapes turned to each facing, or null when the shape does not turn. */
    @Nullable
    private final Map<Direction, VoxelShape> rotatedFloor;
    @Nullable
    private final Map<Direction, VoxelShape> rotatedCeiling;

    public FaceAttachedModelBlock(Properties properties, Settings settings) {
        super(properties);
        this.settings = settings;
        this.rotatedFloor = settings.rotateFloorShape()
                ? BlockShapeUtils.precompute(settings.floor(), Direction.NORTH) : null;
        this.rotatedCeiling = settings.rotateFloorShape()
                ? BlockShapeUtils.precompute(settings.ceiling(), Direction.NORTH) : null;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new FaceAttachedModelBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        AttachFace face = switch (clicked) {
            case UP -> AttachFace.FLOOR;
            case DOWN -> AttachFace.CEILING;
            default -> AttachFace.WALL;
        };
        if (face == AttachFace.WALL && !settings.allowWallPlacement()) {
            return null;
        }
        if (face == AttachFace.CEILING && !settings.allowCeilingPlacement()) {
            return null;
        }
        Direction facing;
        if (face == AttachFace.WALL) {
            facing = clicked;
        } else {
            facing = settings.floorCeilingFacing() == FloorFacing.PLAYER
                    ? context.getHorizontalDirection()
                    : context.getHorizontalDirection().getOpposite();
        }
        BlockState state = this.defaultBlockState().setValue(FACE, face).setValue(FACING, facing);
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> rotatedFloor != null ? rotatedFloor.get(state.getValue(FACING)) : settings.floor();
            case CEILING -> rotatedCeiling != null ? rotatedCeiling.get(state.getValue(FACING)) : settings.ceiling();
            case WALL -> switch (state.getValue(FACING)) {
                case NORTH -> settings.wallNorth();
                case SOUTH -> settings.wallSouth();
                case EAST -> settings.wallEast();
                case WEST -> settings.wallWest();
                default -> settings.wallNorth();
            };
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return settings.collision() == Collision.FULL ? Shapes.block() : getShape(state, level, pos, context);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!settings.requireSupport()) {
            return true;
        }
        Direction attachDir = getConnectedDirection(state);
        BlockPos supportPos = pos.relative(attachDir.getOpposite());
        BlockState support = level.getBlockState(supportPos);
        if (!support.isFaceSturdy(level, supportPos, attachDir)) {
            return false;
        }
        return !settings.requireFullBlockSupport() || support.isCollisionShapeFullBlock(level, supportPos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == getConnectedDirection(state).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
