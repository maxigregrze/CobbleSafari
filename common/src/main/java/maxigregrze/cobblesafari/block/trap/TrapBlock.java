package maxigregrze.cobblesafari.block.trap;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class TrapBlock extends BaseEntityBlock {

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    protected static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 1, 13);
    /** Taller column used only to catch projectiles for projectile-activable traps (fart/explosion). */
    private static final VoxelShape PROJECTILE_SHAPE = Block.box(3, 0, 3, 13, 13, 13);
    private static final Map<Long, Long> NEXT_TRIGGER = new HashMap<>();
    private static final int DEFAULT_COOLDOWN_TICKS = 5;
    /** Above this size, expired cooldown entries are swept so the static map cannot grow unbounded. */
    private static final int COOLDOWN_SWEEP_THRESHOLD = 512;

    protected final boolean hard;

    protected TrapBlock(Properties properties, boolean hard) {
        super(properties);
        this.hard = hard;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    protected abstract void onTriggered(ServerLevel level, BlockPos pos, BlockState state, @Nullable LivingEntity activator);

    protected boolean projectileActivable() {
        return false;
    }

    /** Per-entity re-trigger cooldown in ticks. Overridden by traps that need a longer delay. */
    protected int cooldownTicks() {
        return DEFAULT_COOLDOWN_TICKS;
    }

    private static long triggerKey(BlockPos pos, int entityId) {
        return pos.asLong() ^ entityId;
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Face the same way the player is looking (not toward them).
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Needs a sturdy block underneath: mining the support breaks the trap (and it drops itself).
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // Only trigger for entities actually standing on the trap, not flying/hovering over it.
        if (!living.onGround()) {
            return;
        }
        long key = triggerKey(pos, living.getId());
        long now = serverLevel.getGameTime();
        // Drop expired cooldowns once the map gets large so it cannot grow without bound over a long
        // server uptime (entries are added for every distinct (position, entity) that steps on a trap).
        if (NEXT_TRIGGER.size() > COOLDOWN_SWEEP_THRESHOLD) {
            NEXT_TRIGGER.values().removeIf(expiry -> expiry <= now);
        }
        if (now < NEXT_TRIGGER.getOrDefault(key, 0L)) {
            return;
        }
        NEXT_TRIGGER.put(key, now + cooldownTicks());
        onTriggered(serverLevel, pos, state, living);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        super.onProjectileHit(level, state, hit, projectile);
        if (projectileActivable() && level instanceof ServerLevel serverLevel) {
            LivingEntity owner = projectile.getOwner() instanceof LivingEntity le ? le : null;
            onTriggered(serverLevel, hit.getBlockPos(), state, owner);
        }
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (player.getMainHandItem().is(Items.SHEARS) || player.getOffhandItem().is(Items.SHEARS)) {
            return 1.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Living entities always walk through. Projectile-activable traps (fart/explosion) keep a
        // collision column for every other context so thrown/shot projectiles hit and trigger them.
        if (!projectileActivable()) {
            return Shapes.empty();
        }
        if (context instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof LivingEntity) {
            return Shapes.empty();
        }
        return PROJECTILE_SHAPE;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrapBlockEntity(pos, state);
    }
}
