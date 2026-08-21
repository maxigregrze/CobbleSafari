package maxigregrze.cobblesafari.block.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A drift of fallen maple leaves: eight stacking layers like snow, but warm-proof, walked
 * <em>through</em> rather than on, and softening a landing when it is deep enough.
 *
 * <p>Layer numbering follows the vanilla {@link BlockStateProperties#LAYERS} range of 1..8, so the
 * design's "state N" is {@code layers = N + 1}. Every formula here is written in {@code layers}.</p>
 */
public class MapleLeafPileBlock extends Block implements BonemealableBlock {

    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
    public static final int MAX_LAYERS = 8;

    /**
     * Random ticks a single block receives over one in-game day: {@code 24000 * 3/4096} = 17.58,
     * at the default {@code randomTickSpeed} of 3.
     *
     * <p><b>Every rate below is measured in game ticks, never in seconds.</b> Random ticks are drawn
     * per game tick, so they follow the tick rate exactly: at {@code /tick rate 200} the world runs
     * ten times faster in wall-clock terms and litter accumulates and melts ten times faster with
     * it. Nothing here reads a clock. The one external knob is the {@code randomTickSpeed} gamerule,
     * which scales all of it linearly from its default of 3.</p>
     */
    private static final float RANDOM_TICKS_PER_DAY = 24000.0F * 3.0F / 4096.0F;

    /**
     * Chance, per random tick, that an eligible leaf sheds litter below it: ten times per in-game
     * day, ten times the original one-per-day baseline.
     *
     * <p>This does not run away: {@code accumulateAt} refuses to grow a pile past
     * {@code NATURAL_LAYER_CAP}, so the ground under a canopy saturates at three layers and every
     * further shed is a no-op. Raising this makes the ground fill in faster, not deeper.</p>
     */
    public static final float LITTER_CHANCE = 10.0F / RANDOM_TICKS_PER_DAY;

    /**
     * Chance, per random tick, that rain thins a drift by one layer: ten times per in-game day, ten
     * times the original baseline. A full eight-layer drift left under open sky drains to its last
     * layer in roughly fourteen in-game minutes.
     *
     * <p>Kept as its own constant rather than shared with {@link #LITTER_CHANCE}: the two happen to
     * carry the same multiplier today, but they answer to different questions and are meant to be
     * tuned apart.</p>
     */
    private static final float RAIN_DECAY_CHANCE = 10.0F / RANDOM_TICKS_PER_DAY;

    /** Layers at or below this are walked through at full speed. */
    private static final int FREE_WALK_LAYERS = 2;
    /** Layers above this start softening a fall, by 5% each. */
    private static final int FALL_SOFTEN_FLOOR = 4;
    private static final float PER_LAYER_STEP = 0.05F;

    /**
     * How much a pile of {@code layers} takes off a fall: 0 up to layer 4, then 5% per layer,
     * capped at 20% on a full pile.
     *
     * <p>Applied by {@code LivingEntityMapleLeafPileMixin}, not by {@code fallOn}. The block has no
     * collision at all, so {@code fallOn} is never dispatched to it: an entity standing in a pile
     * rests on the block underneath, and {@code Entity.getOnPosLegacy()} probes
     * {@code floor(y - 0.2)}, which resolves past the pile. An earlier version bought the dispatch
     * with a 3.5px collision floor, the way {@code BelltowerTrellisBlock} does - but that floor
     * lifted the player 0.22 blocks, which both broke two-high passages (1.8 + 0.22 > 2.0) and made
     * stacked piles something to stand inside rather than wade through. Fitting under a two-high
     * ceiling needs a floor of at most 0.2; getting {@code fallOn} needs at least 0.2; and 0.2 is
     * not exactly representable as a double. There is no thickness that satisfies both.</p>
     */
    public static float fallSofteningFactor(int layers) {
        return 1.0F - PER_LAYER_STEP * Math.max(0, layers - FALL_SOFTEN_FLOOR);
    }

    private static final VoxelShape[] SHAPE_BY_LAYER = buildShapes();

    private final Supplier<SimpleParticleType> particle;

    public MapleLeafPileBlock(Properties properties, Supplier<SimpleParticleType> particle) {
        super(properties);
        this.particle = particle;
        this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, 1));
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[MAX_LAYERS + 1];
        for (int layers = 0; layers <= MAX_LAYERS; layers++) {
            shapes[layers] = Block.box(0, 0, 0, 16, layers * 2.0, 16);
        }
        return shapes;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new MapleLeafPileBlock(props, this.particle));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    // ------------------------------------------------------------------ shape

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.LAND;
    }

    // ------------------------------------------------------------------ survival and stacking

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return canSurvive(state, level, pos)
                ? super.updateShape(state, direction, neighborState, level, pos, neighborPos)
                : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        int layers = state.getValue(LAYERS);
        if (!context.getItemInHand().is(this.asItem()) || layers >= MAX_LAYERS) {
            return layers == 1;
        }
        return !context.replacingClickedOnBlock() || context.getClickedFace() == Direction.UP;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) {
            return existing.setValue(LAYERS, Math.min(MAX_LAYERS, existing.getValue(LAYERS) + 1));
        }
        return super.getStateForPlacement(context);
    }

    // ------------------------------------------------------------------ entity interaction

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        int layers = state.getValue(LAYERS);
        if (layers > FREE_WALK_LAYERS) {
            // Applied straight to the delta rather than through makeStuckInBlock, which would call
            // resetFallDistance() and let anyone grazing a pile mid-fall void the whole drop.
            double factor = 1.0 - PER_LAYER_STEP * (layers - FREE_WALK_LAYERS);
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x * factor, motion.y, motion.z * factor);
        }
        if (level.isClientSide) {
            spawnWalkParticles(level, pos, entity, layers);
        }
    }

    private void spawnWalkParticles(Level level, BlockPos pos, Entity entity, int layers) {
        double moved = Math.abs(entity.getX() - entity.xOld) + Math.abs(entity.getZ() - entity.zOld);
        if (moved < 0.01) {
            return;
        }
        RandomSource random = level.random;
        for (int i = 0; i < 2; i++) {
            level.addParticle(particle.get(),
                    pos.getX() + random.nextDouble(),
                    pos.getY() + layers * 2.0 / 16.0,
                    pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.08, 0.03, (random.nextDouble() - 0.5) * 0.08);
        }
    }

    // ------------------------------------------------------------------ weather

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isRainingAt(pos.above()) || random.nextFloat() >= RAIN_DECAY_CHANCE) {
            return;
        }
        int layers = state.getValue(LAYERS);
        // Every layer thins under rain except the last: a drift never washes away entirely, so the
        // block is never removed here - it just stops shrinking at one layer.
        if (layers > 1) {
            level.setBlock(pos, state.setValue(LAYERS, layers - 1), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ bonemeal

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return state.getValue(LAYERS) < MAX_LAYERS || random.nextFloat() < 0.5F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int layers = state.getValue(LAYERS);
        if (layers < MAX_LAYERS) {
            level.setBlock(pos, state.setValue(LAYERS, layers + 1), Block.UPDATE_ALL);
            return;
        }
        // Already full: shed a pile instead of growing.
        popResource(level, pos, new ItemStack(this));
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.GROWER;
    }

    /**
     * Grows an existing pile by one layer, or starts a new one, at {@code pos}. Used by the leaves
     * overhead and by the tree decorator. Returns false when {@code pos} holds something else.
     *
     * @param cap the highest layer count this caller may reach; a pile already above it is left be.
     */
    public boolean accumulateAt(LevelAccessor level, BlockPos pos, int cap) {
        BlockState existing = level.getBlockState(pos);
        if (existing.is(this)) {
            int layers = existing.getValue(LAYERS);
            if (layers >= cap) {
                return false;
            }
            level.setBlock(pos, existing.setValue(LAYERS, layers + 1), Block.UPDATE_ALL);
            return true;
        }
        if (!existing.canBeReplaced() || !canSurvive(defaultBlockState(), level, pos)) {
            return false;
        }
        level.setBlock(pos, defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }
}
