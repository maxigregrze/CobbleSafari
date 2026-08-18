package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A {@link ConnectedModelBlock} that scans the four horizontal faces and exposes one boolean per
 * cardinal direction - exactly 16 states, one per real neighbourhood configuration.
 *
 * <p>The reduction of those 16 states down to six drawn models plus a {@code y} rotation is left
 * entirely to the blockstate JSON (see {@code scripts/generate_connected_blockstates.py}). Encoding
 * the intended silhouette in Java instead would freeze a rendering decision into save data.</p>
 */
public class SideConnectedModelBlock extends ConnectedModelBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    /** Bit per direction, matching {@link #shapeIndex}. Internal to the shape table only. */
    private static final int BIT_NORTH = 1;
    private static final int BIT_EAST = 2;
    private static final int BIT_SOUTH = 4;
    private static final int BIT_WEST = 8;

    /** One composed shape per connection state, or null when the block uses a single fixed shape. */
    @Nullable
    private final VoxelShape[] shapesByIndex;

    /** One collision box per connection state, or null when collision follows the selection shape. */
    @Nullable
    private final VoxelShape[] collisionsByIndex;

    public SideConnectedModelBlock(Properties properties, Settings settings) {
        super(properties, settings);
        this.shapesByIndex = (settings.coreShape() != null && settings.armShape() != null)
                ? buildShapes(settings.coreShape(), settings.armShape())
                : null;
        this.collisionsByIndex = settings.silhouetteCollisions().isEmpty()
                ? null
                : buildSilhouetteCollisions(settings.silhouetteCollisions());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false));
    }

    /**
     * Precomputes the 16 composed shapes once: the core, plus one copy of the arm per live
     * connection. The arm is authored pointing NORTH and rotated by
     * {@link BlockShapeUtils#precompute}, which follows the same rotation convention as the
     * blockstate {@code y} values the generator emits - so hitbox and model stay in step.
     */
    private static VoxelShape[] buildShapes(VoxelShape core, VoxelShape armNorth) {
        Map<Direction, VoxelShape> arms = BlockShapeUtils.precompute(armNorth, Direction.NORTH);
        VoxelShape[] out = new VoxelShape[16];
        for (int i = 0; i < out.length; i++) {
            VoxelShape composed = core;
            if ((i & BIT_NORTH) != 0) {
                composed = Shapes.or(composed, arms.get(Direction.NORTH));
            }
            if ((i & BIT_EAST) != 0) {
                composed = Shapes.or(composed, arms.get(Direction.EAST));
            }
            if ((i & BIT_SOUTH) != 0) {
                composed = Shapes.or(composed, arms.get(Direction.SOUTH));
            }
            if ((i & BIT_WEST) != 0) {
                composed = Shapes.or(composed, arms.get(Direction.WEST));
            }
            out[i] = composed.optimize();
        }
        return out;
    }

    /**
     * Index into the shape table. Deliberately ignores {@code layer}: a different model set per
     * floor is a detail difference, not a different silhouette, so 16 entries and not 64.
     */
    protected static int shapeIndex(BlockState state) {
        int index = 0;
        if (Boolean.TRUE.equals(state.getValue(NORTH))) {
            index |= BIT_NORTH;
        }
        if (Boolean.TRUE.equals(state.getValue(EAST))) {
            index |= BIT_EAST;
        }
        if (Boolean.TRUE.equals(state.getValue(SOUTH))) {
            index |= BIT_SOUTH;
        }
        if (Boolean.TRUE.equals(state.getValue(WEST))) {
            index |= BIT_WEST;
        }
        return index;
    }

    /**
     * Precomputes the 16 collision boxes from the per-silhouette declarations: each state is
     * classified into a silhouette plus a rotation, and the shape drawn for that silhouette is
     * turned by the same angle the blockstate turns the model. A silhouette left undeclared
     * collides with nothing.
     */
    protected static VoxelShape[] buildSilhouetteCollisions(Map<ConnectedShape, VoxelShape> declared) {
        VoxelShape[] out = new VoxelShape[16];
        for (int i = 0; i < out.length; i++) {
            ConnectedShape.Oriented oriented = ConnectedShape.classify(
                    (i & BIT_NORTH) != 0, (i & BIT_EAST) != 0, (i & BIT_SOUTH) != 0, (i & BIT_WEST) != 0);
            VoxelShape authored = declared.get(oriented.shape());
            if (authored == null) {
                out[i] = Shapes.empty();
                continue;
            }
            Direction facing = switch (oriented.yDegrees()) {
                case 90 -> Direction.EAST;
                case 180 -> Direction.SOUTH;
                case 270 -> Direction.WEST;
                default -> Direction.NORTH;
            };
            out[i] = BlockShapeUtils.rotateHorizontal(authored, Direction.NORTH, facing).optimize();
        }
        return out;
    }

    @Override
    protected VoxelShape shapeFor(BlockState state) {
        return shapesByIndex != null ? shapesByIndex[shapeIndex(state)] : super.shapeFor(state);
    }

    @Override
    protected VoxelShape collisionShapeFor(BlockState state) {
        return collisionsByIndex != null ? collisionsByIndex[shapeIndex(state)] : super.collisionShapeFor(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new SideConnectedModelBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected BlockState computeState(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState out = state;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BooleanProperty property = PipeBlock.PROPERTY_BY_DIRECTION.get(direction);
            BlockPos side = pos.relative(direction);
            // Always test against the original state so no already-rewritten axis feeds the next.
            out = out.setValue(property, connectsTo(state, ApparentState.at(level, side), direction));
        }
        return out;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH)).setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> state.setValue(NORTH, state.getValue(EAST)).setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST)).setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> state.setValue(NORTH, state.getValue(WEST)).setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST)).setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
            default -> super.mirror(state, mirror);
        };
    }
}
