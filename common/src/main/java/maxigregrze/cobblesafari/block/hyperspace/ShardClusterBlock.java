package maxigregrze.cobblesafari.block.hyperspace;

import maxigregrze.cobblesafari.block.base.BlockShapeUtils;
import maxigregrze.cobblesafari.block.base.LoweredSupport;
import maxigregrze.cobblesafari.dungeon.hyperspace.obstacle.ObstacleKind;
import maxigregrze.cobblesafari.dungeon.hyperspace.obstacle.ShardClusterSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Mega Shard cluster (action plan 196, § 8.8): an obstacle cleared by any attacking move, placed on
 * a floor, a wall or a ceiling. On the floor it keeps the fence-high collision of the other obstacles;
 * on a wall or a ceiling the collision is the model shape.
 *
 * <p>A floor cluster may also stand on a road-like block ({@link LoweredSupport}, 15 px top): {@link #LOWERED} then swaps in the {@code _lowered} model and moves both shapes down 1 px.</p>
 */
public class ShardClusterBlock extends ObstacleBlock {

    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LOWERED = BooleanProperty.create("lowered");

    private static final double PX = 1.0 / 16.0;

    private static final VoxelShape FLOOR = SHAPE_SHARDCLUSTER;
    private static final VoxelShape FLOOR_LOWERED = FLOOR.move(0, -PX, 0);
    private static final VoxelShape CEILING = BlockShapeUtils.flipVertical(FLOOR);
    /** Wall shapes follow the blockstate's x = 90 turn: the cluster grows out of the wall, towards FACING. */
    private static final Map<Direction, VoxelShape> WALL =
            BlockShapeUtils.precompute(floorToWallNorth(FLOOR), Direction.NORTH);

    public ShardClusterBlock(Properties properties) {
        super(properties, ObstacleKind.SHARDCLUSTER);
        registerDefaultState(stateDefinition.any()
                .setValue(CLEARED, false)
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(FACING, Direction.NORTH)
                .setValue(LOWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACE, FACING, LOWERED);
    }

    /** Floor, ceiling or wall from the clicked face, on a sturdy support - or, on the floor, a road-like one. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        AttachFace face = clicked == Direction.UP ? AttachFace.FLOOR
                : clicked == Direction.DOWN ? AttachFace.CEILING : AttachFace.WALL;
        Direction facing = face == AttachFace.WALL ? clicked : context.getHorizontalDirection().getOpposite();
        BlockPos support = context.getClickedPos().relative(clicked.getOpposite());
        BlockState supportState = context.getLevel().getBlockState(support);
        boolean lowered = face == AttachFace.FLOOR && LoweredSupport.test(supportState, context.getLevel(), support);
        if (!lowered && !supportState.isFaceSturdy(context.getLevel(), support, clicked)) {
            return null;
        }
        return defaultBlockState().setValue(FACE, face).setValue(FACING, facing).setValue(LOWERED, lowered);
    }

    /** Follows the block below, so a road laid or removed under a floor cluster moves it accordingly. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && state.getValue(FACE) == AttachFace.FLOOR) {
            return state.setValue(LOWERED, LoweredSupport.test(neighborState, level, neighborPos));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // --- enableShardsClusters off (plan 196, § 8.9): only creative players see, touch or target it ---

    /** Hidden from {@code context}'s entity; an entity-less context (shape caches) still gets the real shape. */
    private static boolean hiddenFor(BlockGetter level, CollisionContext context) {
        return !ShardClusterSettings.enabled(level)
                && context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() != null
                && !isCreative(context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return ShardClusterSettings.hiddenOnClient() ? RenderShape.INVISIBLE : super.getRenderShape(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return hiddenFor(level, context) ? Shapes.empty() : super.getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return hiddenFor(level, context) ? Shapes.empty() : super.getCollisionShape(state, level, pos, context);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!ShardClusterSettings.enabled(level)) {
            return InteractionResult.PASS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected VoxelShape standingShape(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> state.getValue(LOWERED) ? FLOOR_LOWERED : FLOOR;
            case CEILING -> CEILING;
            case WALL -> WALL.get(state.getValue(FACING));
        };
    }

    @Override
    protected VoxelShape standingCollision(BlockState state) {
        if (state.getValue(FACE) != AttachFace.FLOOR) {
            return standingShape(state);
        }
        VoxelShape fenceHigh = super.standingCollision(state);
        return state.getValue(LOWERED) ? fenceHigh.move(0, -PX, 0) : fenceHigh;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Floor → north wall (support on the south side): model y becomes depth from the wall, model z becomes height. */
    private static VoxelShape floorToWallNorth(VoxelShape floor) {
        VoxelShape[] acc = {Shapes.empty()};
        floor.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                acc[0] = Shapes.or(acc[0], Shapes.box(minX, minZ, 1 - maxY, maxX, maxZ, 1 - minY)));
        return acc[0];
    }
}
