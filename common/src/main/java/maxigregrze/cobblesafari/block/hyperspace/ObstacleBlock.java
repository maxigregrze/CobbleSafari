package maxigregrze.cobblesafari.block.hyperspace;

import maxigregrze.cobblesafari.dungeon.hyperspace.obstacle.ObstacleClearAction;
import maxigregrze.cobblesafari.dungeon.hyperspace.obstacle.ObstacleKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Z-A obstacle (action plan 196, § 8.1): unbreakable by hand in survival, cleared by a sent-out
 * Pokémon that knows a compatible move. Not orientable - the blockstate picks one of four model
 * rotations per position, so each selection box is the union of the model over the four rotations.
 * Collision is a fence-high cell (1.5 blocks), so the obstacle cannot be jumped over.
 *
 * <p>Clearing does not remove the block (§ 8.7): it turns {@link #CLEARED} - invisible, no collision,
 * outline only for creative players - and a scheduled tick brings it back after the configured delay,
 * so every visitor of the instance can still finish a "destroy" task.</p>
 */
public class ObstacleBlock extends Block {

    public static final BooleanProperty CLEARED = BooleanProperty.create("cleared");

    /** Retry delay when something stands where the obstacle would come back. */
    private static final int OCCUPIED_RETRY_TICKS = 20;

    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 24, 16);

    // Cross-shaped rock with corner nubs and a cap; the model is already 4-fold symmetric.
    private static final VoxelShape SHAPE_BOULDER = Shapes.or(
            Block.box(-4, 0, 0, 20, 12, 16),
            Block.box(0, 0, -4, 16, 12, 20),
            Block.box(-2, 0, -2, 18, 8, 18),
            Block.box(-2, 12, 3, 18, 15, 13),
            Block.box(3, 12, -2, 13, 15, 18),
            Block.box(0, 12, 0, 16, 18, 16));
    // Four tufts, one per quadrant, 20 to 25 px tall depending on the rotation.
    private static final VoxelShape SHAPE_BRAMBLES = Block.box(-1, 0, -1, 17, 25, 17);
    // Stacked blobs; the flat splats around the base are left out.
    private static final VoxelShape SHAPE_SLUDGE = Shapes.or(
            Block.box(1, 0, 1, 15, 8, 15),
            Block.box(2, 8, 2, 14, 14, 14),
            Block.box(4, 14, 4, 12, 20, 12));
    /** Floor shape of the shard cluster; {@link ShardClusterBlock} turns it to walls and ceilings. */
    static final VoxelShape SHAPE_SHARDCLUSTER = Shapes.or(
            Block.box(0, 0, 0, 16, 5, 16),
            Block.box(1, 5, 1, 15, 10, 15),
            Block.box(-1, 5, 5, 17, 13, 11),
            Block.box(5, 5, -1, 11, 13, 17),
            Block.box(5, 5, 5, 11, 15, 11),
            Block.box(6.5, 15, 6.5, 9.5, 17, 9.5));

    private final ObstacleKind kind;
    private final VoxelShape shape;

    public ObstacleBlock(Properties properties, ObstacleKind kind) {
        super(properties);
        this.kind = kind;
        this.shape = switch (kind) {
            case BOULDER -> SHAPE_BOULDER;
            case BRAMBLES -> SHAPE_BRAMBLES;
            case SLUDGE -> SHAPE_SLUDGE;
            case SHARDCLUSTER -> SHAPE_SHARDCLUSTER;
        };
        registerDefaultState(stateDefinition.any().setValue(CLEARED, false));
    }

    public ObstacleKind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CLEARED);
    }

    /** Hides the obstacle and schedules its return; break particles and sound mark the clear. */
    public void clear(ServerLevel level, BlockPos pos, BlockState state) {
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        level.setBlock(pos, state.setValue(CLEARED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, kind.respawnSeconds() * 20);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CLEARED)) {
            return;
        }
        if (!level.getEntitiesOfClass(LivingEntity.class, standingCollision(state).bounds().move(pos)).isEmpty()) {
            level.scheduleTick(pos, this, OCCUPIED_RETRY_TICKS);
            return;
        }
        level.setBlock(pos, state.setValue(CLEARED, false), Block.UPDATE_ALL);
        SoundType sound = state.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(CLEARED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(CLEARED) && !isCreative(context)) {
            return Shapes.empty();
        }
        return standingShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return state.getValue(CLEARED) ? Shapes.empty() : standingCollision(state);
    }

    /** Selection box of the standing obstacle. */
    protected VoxelShape standingShape(BlockState state) {
        return shape;
    }

    /** Collision of the standing obstacle: a fence-high cell. */
    protected VoxelShape standingCollision(BlockState state) {
        return COLLISION;
    }

    protected static boolean isCreative(CollisionContext context) {
        return context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player player && player.isCreative();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (state.getValue(CLEARED)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            ObstacleClearAction.tryClear(serverLevel, pos, kind, serverPlayer);
        }
        return InteractionResult.CONSUME;
    }
}
