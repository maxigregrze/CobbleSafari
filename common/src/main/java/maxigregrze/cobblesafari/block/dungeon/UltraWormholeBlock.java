package maxigregrze.cobblesafari.block.dungeon;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.warpride.logic.WarpRideDestinationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The exit portal generated inside a sub-instance, replacing the {@code hoopa_ring_portal} marker
 * the structure author baked into the {@code .nbt} (action plan 183, section 4.2).
 *
 * <p>{@link RenderShape#INVISIBLE}: there is no baked model at all. The whole appearance is
 * emitted by {@code UltraWormholeBlockEntityRenderer}, because the tint the destination asks for
 * is a per-pixel function and therefore needs a derived texture, which can never live in the block
 * atlas a baked model reads from.</p>
 *
 * <p><b>A hand-placed copy is decorative, and nothing marks it so.</b> Right-clicking starts by
 * reading the player's sub-session; someone holding none has nothing to settle and nowhere to go
 * back to, so the click does nothing. No {@code isDecorative} flag exists - and therefore none can
 * be flipped to manufacture an exit. The session decides, never the block.</p>
 */
public class UltraWormholeBlock extends BaseEntityBlock {

    public static final MapCodec<UltraWormholeBlock> CODEC = simpleCodec(UltraWormholeBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public UltraWormholeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /**
     * A full cube for a creative player, nothing at all for anyone else.
     *
     * <p>In survival the portal is scenery you walk into: an outline box floating in the middle of
     * a three-block vortex would frame one arbitrary cube of it and read as a bug. In creative the
     * builder needs something to aim at, and a full cube is the easiest target - the drawn vortex
     * spins, so any shape following it would be a moving one. Same pattern as {@code CsBossMimicBlock}.</p>
     *
     * <p>This is the <b>outline</b> only. The block is declared {@code noCollission()}, so
     * {@code BlockStateBase.getCollisionShape} short-circuits to empty either way and players walk
     * straight through - which is the point of a portal.</p>
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ec
                && ec.getEntity() instanceof Player player && player.isCreative()) {
            return Shapes.block();
        }
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UltraWormholeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ULTRA_WORMHOLE, UltraWormholeBlockEntity::tick);
    }

    /**
     * A copy placed by hand gets a random destination colour so it reads as a real portal in a
     * build. Generated copies have their {@code typeId} set by the pipeline immediately after
     * placement, which overwrites this.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof UltraWormholeBlockEntity portal) {
            portal.setTypeId(WarpRideDestinationRegistry.randomTypeId(level.getRandom()));
        }
    }

    /**
     * Walking into the portal is what sends you home - there is nothing to right-click.
     *
     * <p>{@code entityInside} fires for any entity whose bounding box overlaps this block, with no
     * collision required, which is exactly how a nether portal works. Re-entry within the same tick
     * is harmless: {@link SubDungeonManager#returnThroughExit} reads the player's current scope, and
     * by the second call they are already standing in the parent dimension, so it returns at once.</p>
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        SubDungeonManager.returnThroughExit(player);
    }
}
