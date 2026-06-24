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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
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
 * override only the relevant hook — see {@code PileBlock} overriding {@code entityInside}.
 * Never subclass merely to set a shape or facing; pass {@link Settings} instead.</p>
 */
public class HorizontalModelBlock extends HorizontalDirectionalBlock {

    /** What the block must be attached to in order to survive. */
    public enum Support { NONE, GROUND, WALL }

    /** Collision behaviour. */
    public enum Collision { SHAPE, NONE, FULL }

    /** Immutable configuration for a {@link HorizontalModelBlock}. Build via {@link #builder()}. */
    public record Settings(
            @Nullable VoxelShape shape,
            Direction authoredFacing,
            boolean rotateShape,
            Direction defaultFacing,
            Support support,
            Collision collision,
            boolean emptyOcclusion,
            @Nullable String descriptionId) {

        public static Builder builder() {
            return new Builder();
        }

        /** Plain oriented full cube (no custom shape). */
        public static Settings cube() {
            return builder().build();
        }

        public static final class Builder {
            private VoxelShape shape = null;
            private Direction authoredFacing = Direction.NORTH;
            private boolean rotateShape = false;
            private Direction defaultFacing = Direction.NORTH;
            private Support support = Support.NONE;
            private Collision collision = Collision.SHAPE;
            private boolean emptyOcclusion = false;
            private String descriptionId = null;

            public Builder shape(VoxelShape shape) { this.shape = shape; return this; }
            public Builder authoredFacing(Direction facing) { this.authoredFacing = facing; return this; }
            public Builder rotateShape() { this.rotateShape = true; return this; }
            public Builder defaultFacing(Direction facing) { this.defaultFacing = facing; return this; }
            public Builder support(Support support) { this.support = support; return this; }
            public Builder collision(Collision collision) { this.collision = collision; return this; }
            public Builder emptyOcclusion() { this.emptyOcclusion = true; return this; }
            public Builder descriptionId(String id) { this.descriptionId = id; return this; }

            public Settings build() {
                return new Settings(shape, authoredFacing, rotateShape, defaultFacing,
                        support, collision, emptyOcclusion, descriptionId);
            }
        }
    }

    private final Settings settings;
    private final VoxelShape baseShape;
    @Nullable
    private final Map<Direction, VoxelShape> rotatedShapes;

    public HorizontalModelBlock(Properties properties, Settings settings) {
        super(properties);
        this.settings = settings;
        this.baseShape = settings.shape() != null ? settings.shape() : Shapes.block();
        this.rotatedShapes = (settings.rotateShape() && settings.shape() != null)
                ? BlockShapeUtils.precompute(settings.shape(), settings.authoredFacing())
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
            case SHAPE -> getShape(state, level, pos, context);
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
