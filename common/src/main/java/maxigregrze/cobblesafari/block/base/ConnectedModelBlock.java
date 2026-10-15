package maxigregrze.cobblesafari.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared engine for blocks whose blockstate properties are <em>derived from their neighbours</em>
 * rather than from the player: recomputed on placement and on every neighbour update, with the
 * choice of model pushed entirely into the blockstate JSON.
 *
 * <p>This class holds everything that does not depend on which axes a block scans: the
 * {@link Settings} record, the matching rules, the placement / update / self-heal hooks, and the
 * shape, occlusion and rendering behaviour. A concrete subclass only declares its
 * {@code static final} properties, adds them in {@code createBlockStateDefinition}, and
 * implements {@link #computeState}.</p>
 *
 * <p>Properties are declared per subclass, not per {@link Settings}, on purpose:
 * {@code createBlockStateDefinition} runs from inside the {@link Block} constructor, before any
 * subclass field is assigned, so a settings-driven property set would read {@code null}. Vanilla
 * splits {@code CrossCollisionBlock} / {@code FenceBlock} for the same reason. Everything that is
 * <em>not</em> a blockstate property still goes through {@link Settings}.</p>
 *
 * @see VerticalConnectedModelBlock  vertical runs (single / bottom / middle / top)
 * @see SideConnectedModelBlock      the four horizontal connections
 * @see LayeredSideConnectedModelBlock  the above, plus a per-floor model set
 */
public abstract class ConnectedModelBlock extends Block implements NeighborDerivedBlock {

    /** Collision behaviour, matching {@link HorizontalModelBlock.Collision}. */
    public enum Collision { SHAPE, NONE, FULL }

    /**
     * Immutable configuration for a {@link ConnectedModelBlock}. Build via {@link #builder()}.
     *
     * <p>{@code shape} is the single fixed shape used when nothing finer is declared.
     * {@code coreShape} / {@code armShape} are read only by {@link SideConnectedModelBlock}, and
     * {@code segmentShapes} only by {@link VerticalConnectedModelBlock}; each is ignored by the
     * blocks it does not concern.</p>
     */
    public record Settings(
            @Nullable TagKey<Block> connectTag,
            @Nullable TagKey<Block> stackTag,
            @Nullable TagKey<Block> groundTag,
            @Nullable VoxelShape shape,
            @Nullable VoxelShape coreShape,
            @Nullable VoxelShape armShape,
            Map<VerticalSegment, VoxelShape> segmentShapes,
            Map<ConnectedShape, VoxelShape> silhouetteCollisions,
            Map<Integer, Map<ConnectedShape, VoxelShape>> layerSilhouetteCollisions,
            Collision collision,
            boolean emptyOcclusion,
            boolean noAoShadow,
            @Nullable Direction.Axis skipRenderingAxis,
            boolean refreshOnPlace,
            @Nullable String descriptionId) {

        public static Builder builder() {
            return new Builder();
        }

        /** Plain full cube that only connects to itself. */
        public static Settings cube() {
            return builder().build();
        }

        public static final class Builder {
            private TagKey<Block> connectTag = null;
            private TagKey<Block> stackTag = null;
            private TagKey<Block> groundTag = null;
            private VoxelShape shape = null;
            private VoxelShape coreShape = null;
            private VoxelShape armShape = null;
            private final Map<VerticalSegment, VoxelShape> segmentShapes = new EnumMap<>(VerticalSegment.class);
            private final Map<ConnectedShape, VoxelShape> silhouetteCollisions = new EnumMap<>(ConnectedShape.class);
            private final Map<Integer, Map<ConnectedShape, VoxelShape>> layerSilhouetteCollisions = new HashMap<>();
            private Collision collision = Collision.SHAPE;
            private boolean emptyOcclusion = false;
            private boolean noAoShadow = false;
            private Direction.Axis skipRenderingAxis = null;
            private boolean refreshOnPlace = true;
            private String descriptionId = null;

            /** Blocks of this tag count as a connection, on top of the block itself. */
            public Builder connectTag(TagKey<Block> tag) { this.connectTag = tag; return this; }

            /** Blocks of this tag belong to the same vertical stack. Defaults to {@link #connectTag}. */
            public Builder stackTag(TagKey<Block> tag) { this.stackTag = tag; return this; }

            /**
             * Blocks of this tag count as ground for a {@link GroundedSideConnectedModelBlock},
             * on top of anything already sturdy. Needed because a block can read as solid to the
             * eye while its collision says otherwise - the scaffolding tube is a full cube to
             * target but hollow to walk through, so {@code isFaceSturdy} alone rejects it.
             */
            public Builder groundTag(TagKey<Block> tag) { this.groundTag = tag; return this; }

            public Builder shape(VoxelShape shape) { this.shape = shape; return this; }

            /**
             * Makes a {@link SideConnectedModelBlock} compose its shape per state: {@code core} is
             * always present, and one copy of {@code armNorth} - authored pointing NORTH, i.e.
             * reaching {@code z = 0} - is OR'd in per live connection, rotated to each facing.
             * Omit to keep the single fixed {@link #shape}.
             */
            public Builder connectedShape(VoxelShape core, VoxelShape armNorth) {
                this.coreShape = core;
                this.armShape = armNorth;
                return this;
            }

            /** Per-segment shape for a {@link VerticalConnectedModelBlock}. Unset segments fall back to {@link #shape}. */
            public Builder segmentShape(VerticalSegment segment, VoxelShape shape) {
                this.segmentShapes.put(segment, shape);
                return this;
            }

            /**
             * Collision box for one silhouette of a {@link SideConnectedModelBlock}, drawn in the
             * canonical orientation (see {@link ConnectedShape}) and rotated per state by the
             * engine. Declaring any silhouette switches the block to per-state collision, and every
             * silhouette left undeclared then collides with <em>nothing</em> - which is how an
             * empty model gets no collision without a special case. The selection box is untouched.
             */
            public Builder silhouetteCollision(ConnectedShape shape, VoxelShape collision) {
                this.silhouetteCollisions.put(shape, collision);
                return this;
            }

            /**
             * Collision for one silhouette on one specific layer, overriding the layer-agnostic
             * declaration above. Only meaningful on a {@link LayeredSideConnectedModelBlock}, whose
             * alternating model sets can put matter where the previous layer had none. A layer with
             * no override falls back to {@link #silhouetteCollision(ConnectedShape, VoxelShape)}.
             */
            public Builder silhouetteCollision(int layer, ConnectedShape shape, VoxelShape collision) {
                this.layerSilhouetteCollisions
                        .computeIfAbsent(layer, k -> new EnumMap<>(ConnectedShape.class))
                        .put(shape, collision);
                return this;
            }

            public Builder collision(Collision collision) { this.collision = collision; return this; }
            public Builder emptyOcclusion() { this.emptyOcclusion = true; return this; }

            /**
             * Reports full shade brightness, so ambient occlusion treats the block like glass. Needed
             * when the collision is a full cube but the model is not: vanilla would then read a 0.2
             * shade, darkening the block's own inset faces and the faces of everything around it.
             */
            public Builder noAoShadow() { this.noAoShadow = true; return this; }

            /** Two peers stop drawing the face they share along this axis. Null disables it. */
            public Builder skipRenderingAxis(Direction.Axis axis) { this.skipRenderingAxis = axis; return this; }

            public Builder noRefreshOnPlace() { this.refreshOnPlace = false; return this; }
            public Builder descriptionId(String id) { this.descriptionId = id; return this; }

            public Settings build() {
                Map<Integer, Map<ConnectedShape, VoxelShape>> byLayer = new HashMap<>();
                layerSilhouetteCollisions.forEach((layer, map) -> byLayer.put(layer, Map.copyOf(map)));
                return new Settings(connectTag, stackTag, groundTag, shape, coreShape, armShape,
                        Map.copyOf(segmentShapes), Map.copyOf(silhouetteCollisions),
                        Map.copyOf(byLayer), collision,
                        emptyOcclusion, noAoShadow, skipRenderingAxis, refreshOnPlace, descriptionId);
            }
        }
    }

    protected final Settings settings;
    private final VoxelShape shape;

    protected ConnectedModelBlock(Properties properties, Settings settings) {
        super(properties);
        this.settings = settings;
        this.shape = settings.shape() != null ? settings.shape() : Shapes.block();
    }

    // ------------------------------------------------------------------ matching

    /**
     * True when {@code neighbor} counts as a connection for {@code self}. The same-block case is
     * unconditional on purpose: a missing, empty or misspelled tag degrades to "only connects to
     * itself", never to "connects to nothing".
     *
     * <p>{@code direction} is unused by the default rule; it exists so a concrete block can define
     * asymmetric rules, the way {@code FenceBlock.connectsTo} does in vanilla.</p>
     */
    protected boolean connectsTo(BlockState self, BlockState neighbor, Direction direction) {
        if (neighbor.is(self.getBlock())) {
            return true;
        }
        TagKey<Block> tag = settings.connectTag();
        return tag != null && neighbor.is(tag);
    }

    /** True when {@code neighbor} belongs to the same vertical stack for layer counting. */
    protected boolean stacksWith(BlockState self, BlockState neighbor) {
        if (neighbor.is(self.getBlock())) {
            return true;
        }
        TagKey<Block> tag = settings.stackTag() != null ? settings.stackTag() : settings.connectTag();
        return tag != null && neighbor.is(tag);
    }

    /**
     * Height of {@code pos} inside its vertical run of stack peers, modulo {@code layerCount}.
     * O(1) and exact at unbounded height: one plus the layer of the block below. The block below
     * is always current when this runs - updateShape hands us the already-written DOWN neighbour,
     * and the cascade walks the stack upward one block at a time. A stack peer carrying no layer
     * property (e.g. a platform variant) reads as -1, so the block resting on it restarts at 0.
     * The peer is read through {@link ApparentState}, so a disguised block contributes the layer of
     * the block it copies rather than none at all.
     *
     * <p>The property is passed in rather than read from a field because it is declared per
     * subclass, not here: {@code createBlockStateDefinition} runs before any subclass field is
     * assigned (see this class's header). Shared by {@link LayeredSideConnectedModelBlock} and
     * {@link LayeredVerticalConnectedModelBlock}, whose only difference is which axes they scan.</p>
     */
    protected int computeLayer(BlockState self, BlockGetter level, BlockPos pos,
                               IntegerProperty layerProperty, int layerCount) {
        if (layerCount <= 1) {
            return 0;
        }
        BlockState below = ApparentState.at(level, pos.below());
        if (!stacksWith(self, below)) {
            return 0;
        }
        return (below.getOptionalValue(layerProperty).orElse(-1) + 1) % layerCount;
    }

    // ------------------------------------------------------------------ recompute

    /**
     * Recomputes every neighbour-derived property of {@code state} at {@code pos}. Implemented by
     * each concrete subclass for the axes it declares. Must be pure: it runs during neighbour
     * updates, where a re-entrant {@code setBlock} is a classic source of state corruption.
     */
    protected abstract BlockState computeState(BlockState state, BlockGetter level, BlockPos pos);

    /**
     * The state this block would carry at {@code pos} without ever being placed there - i.e.
     * {@link #computeState} applied to the default state. Lets a disguised block (the CSBoss mimic)
     * resolve a full connected state instead of a bare {@code defaultBlockState()}.
     */
    @Override
    public BlockState apparentStateAt(BlockGetter level, BlockPos pos) {
        return computeState(this.defaultBlockState(), level, pos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeState(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return computeState(state, level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Covers raw level.setBlock: structures, worldgen, /fill, /clone, /setblock - none of which
        // go through getStateForPlacement. The "block actually changed" guard is what stops the
        // loop, since the setBlock done by tick() below re-enters onPlace with the same block.
        if (settings.refreshOnPlace() && !oldState.is(this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState fixed = computeState(state, level, pos);
        if (fixed != state) {
            level.setBlock(pos, fixed, Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ shape / rendering

    /**
     * The shape for a given state. Defaults to the block's single fixed shape; subclasses override
     * to follow their connection properties. Called on every selection and collision query, so
     * implementations must be a lookup, never a computation.
     */
    protected VoxelShape shapeFor(BlockState state) {
        return shape;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    /**
     * The collision shape for a given state, when {@link Collision#SHAPE} is in force. Defaults to
     * the selection shape; subclasses override to decouple the two - a block whose model can be
     * empty still needs a selection box so it can be targeted and broken. Same contract as
     * {@link #shapeFor}: a lookup, never a computation.
     */
    protected VoxelShape collisionShapeFor(BlockState state) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (settings.collision()) {
            case SHAPE -> collisionShapeFor(state);
            case NONE -> Shapes.empty();
            case FULL -> Shapes.block();
        };
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return settings.emptyOcclusion() ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return settings.noAoShadow() ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Filtering by axis is deliberate. On a full cube, dropping every shared face is correct;
     * on a carved model it would hollow the block out sideways. A stacked window declares
     * {@code Direction.Axis.Y} so only the horizontal seam between two superposed panes goes away.
     * The test is on the block itself, not the tag: two differently textured blocks must not eat
     * each other's faces.
     */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        Direction.Axis axis = settings.skipRenderingAxis();
        if (axis != null && side.getAxis() == axis && adjacentState.is(state.getBlock())) {
            return true;
        }
        return super.skipRendering(state, adjacentState, side);
    }

    @Override
    public String getDescriptionId() {
        return settings.descriptionId() != null ? settings.descriptionId() : super.getDescriptionId();
    }
}
