package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An invisible, indestructible marker that calls lightning down on itself whenever a player is
 * near. Modelled on the vanilla barrier: no collision, no loot, {@code -1} hardness so survival
 * cannot touch it, and a selection box that only exists for a creative player holding the item.
 * Being {@code replaceable}, it behaves like air when someone builds through it.
 *
 * <p>The cadence is a <em>scheduled</em> tick, not a random one. A random tick's rate follows the
 * {@code randomTickSpeed} gamerule and only fires in fully-ticking chunks, so "about once a minute"
 * would drift with server configuration; a scheduled tick is persisted in chunk data, survives a
 * reload, and keeps its period exactly.</p>
 */
public class LightningSpotBlock extends BaseEntityBlock {

    public static final MapCodec<LightningSpotBlock> CODEC = simpleCodec(LightningSpotBlock::new);

    /** Delay between two attempts: 600 to 1200 ticks, i.e. anywhere from 30 to 60 seconds. */
    private static final int MIN_DELAY_TICKS = 600;
    private static final int DELAY_SPREAD_TICKS = 600;

    /**
     * The first wait after the block is placed, from a third of the usual minimum to half the usual
     * maximum: 200 to 600 ticks, so a freshly generated structure does not keep its visitor waiting
     * up to a minute for the first strike (action plan 190, B4).
     */
    private static final int FIRST_MIN_DELAY_TICKS = MIN_DELAY_TICKS / 3;
    private static final int FIRST_DELAY_SPREAD_TICKS = (MIN_DELAY_TICKS + DELAY_SPREAD_TICKS) / 2 - FIRST_MIN_DELAY_TICKS;

    /** Trigger radius, in blocks. */
    private static final double TRIGGER_RADIUS = 32.0;

    public LightningSpotBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LightningSpotBlockEntity(pos, state);
    }

    /**
     * {@code INVISIBLE}, and it has to be: {@code ParticleEngine.crack} skips a block only when its
     * render shape is exactly this, and otherwise goes straight to
     * {@code state.getShape(level, pos).bounds()}. That two-argument overload passes
     * {@code CollisionContext.empty()}, whose entity is null, so {@link #getShape} below hands back
     * an empty shape and {@code bounds()} throws. Vanilla's barrier and light block return
     * {@code INVISIBLE} for the same reason - it is what makes the "no shape unless you hold me"
     * trick safe. The block entity renderer is unaffected: the chunk compiler collects block
     * entities from {@code hasBlockEntity()}, before and independently of the render shape.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // ------------------------------------------------------------------ lightning

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            // Only placement arms the short first wait: a scheduled tick survives a chunk reload,
            // so reloading is never a fresh activation.
            level.scheduleTick(pos, this, FIRST_MIN_DELAY_TICKS + level.getRandom().nextInt(FIRST_DELAY_SPREAD_TICKS));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, TRIGGER_RADIUS)) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(Vec3.atBottomCenterOf(pos));
                level.addFreshEntity(bolt);
            }
        }
        // Unconditional: a tick that lands while nobody is around must still re-arm, or the spot
        // would go dead for the rest of the save.
        scheduleNext(level, pos);
    }

    private void scheduleNext(LevelAccessor level, BlockPos pos) {
        level.scheduleTick(pos, this, MIN_DELAY_TICKS + level.getRandom().nextInt(DELAY_SPREAD_TICKS));
    }

    // ------------------------------------------------------------------ visibility

    /**
     * Selectable only by a creative player holding the block itself, the way {@code LiquidBarrier}
     * and the vanilla barrier work. Everyone else looks straight through it.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof Player player
                && player.isCreative() && isHolding(player)) {
            return Shapes.block();
        }
        return Shapes.empty();
    }

    /** Whether {@code player} carries this block in either hand - the visibility condition. */
    public boolean isHolding(Player player) {
        return player.getMainHandItem().is(this.asItem()) || player.getOffhandItem().is(this.asItem());
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }
}
