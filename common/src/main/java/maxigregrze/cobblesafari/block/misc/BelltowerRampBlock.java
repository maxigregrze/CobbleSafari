package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.BlockShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Jump ramp: a 1-pixel-thick orientable pad that launches whatever walks or lands on it
 * 3 blocks forward and 1 block up, along a scripted parabola driven by {@link RampLaunchManager}.
 *
 * <p>{@code FACING} is the <b>launch direction</b>, i.e. the front (south) of the authored model.
 * Placement therefore uses {@code getHorizontalDirection()} and <em>not</em> its opposite: the low
 * entry end of the ramp ends up on the placing player's side and the launch goes away from them.
 * This is why the block cannot reuse {@code HorizontalModelBlock}, whose placement always faces
 * the player.</p>
 *
 * <p>The launch face is <b>one-way</b>: the model draws a full-width guardrail across it, and
 * {@link #getCollisionShape} makes that guardrail solid for anything trying to get <em>in</em>
 * through it, while staying absent for anything already on the pad or flying out of it. Plan 177.
 * See {@link #isBeyondFrontFace} for the invariant that makes it safe, and {@code ModBlocks} for
 * why the block must be registered {@code dynamicShape}.</p>
 *
 * <p>Unrelated to {@link BelltowerTrellisBlock} beyond sharing two vanilla plank textures.</p>
 */
public class BelltowerRampBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<BelltowerRampBlock> CODEC = simpleCodec(BelltowerRampBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Selection, and the collision every entity but one gets: a 1-pixel slab, all four orientations. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    /** Vanilla's collision comparison slack. */
    private static final double EPSILON = 1.0E-7;

    /**
     * Front barrier: a 2-pixel plate flush with the launch face, 3 blocks tall. Only ever handed to
     * an entity that is entirely beyond that face (see {@link #isBeyondFrontFace}), so it can never
     * overlap the entity it is shown to - it stops motion into the cell and does nothing else.
     *
     * <p>Thickness matches the 2-pixel footprint of the model's front guardrail (z 14..16); what
     * actually decides where an entity is stopped is the outer face, which is flush with the block
     * face. Height is the sizing constraint: {@link RampLaunchManager}'s arc peaks at exactly
     * 2 blocks, so a 2-block wall would leave a ramp aimed at this one's face crossing it within a
     * thousandth of a block of the top - passing or not depending on air control. 3 blocks settles
     * it, and outlasts a running jump (1.25 blocks) by a wide margin.</p>
     */
    private static final VoxelShape FRONT_WALL_SOUTH = Block.box(0, 0, 14, 16, 48, 16);

    /** {@link #SHAPE} joined with the barrier, one entry per facing. Authored south, like the model. */
    private static final Map<Direction, VoxelShape> BLOCKED_SHAPES = buildBlockedShapes();

    private static Map<Direction, VoxelShape> buildBlockedShapes() {
        Map<Direction, VoxelShape> blocked = new EnumMap<>(Direction.class);
        BlockShapeUtils.precompute(FRONT_WALL_SOUTH, Direction.SOUTH)
                .forEach((facing, wall) -> blocked.put(facing, Shapes.or(SHAPE, wall)));
        return blocked;
    }

    public BelltowerRampBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * A wall on the launch face - for whoever is trying to get in through it, and only for them.
     * The idiom is vanilla's: {@code PowderSnowBlock} reads its entity out of the collision context
     * the same way, as do {@code CsBossMimicBlock} and the reactive distortion flowers here.
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(context instanceof EntityCollisionContext entityContext)) {
            return SHAPE;
        }
        Entity entity = entityContext.getEntity();
        // LivingEntity mirrors RampLaunchManager: what the ramp will not launch, it will not stop
        // either - and projectiles resolve their flight against this very shape, so an arrow shot at
        // the front of a ramp would otherwise stop in mid-air on nothing. The spectator test is
        // redundant (Player.tick sets noPhysics, which short-circuits block collision outright) but
        // says so out loud.
        if (!(entity instanceof LivingEntity) || entity.isSpectator()) {
            return SHAPE;
        }
        // Same exemptions as tryLaunch: a builder flying past a ramp must not hit anything.
        if (entity instanceof Player player && (player.getAbilities().flying || player.isFallFlying())) {
            return SHAPE;
        }
        Direction facing = state.getValue(FACING);
        return isBeyondFrontFace(facing, pos, entity.getBoundingBox()) ? BLOCKED_SHAPES.get(facing) : SHAPE;
    }

    /**
     * Is {@code box} entirely beyond the block's launch face, i.e. in front of the ramp looking back
     * at it? Purely geometric: no velocity, no entity facing, no stored state, so client and server
     * evaluate the same function on the same input and cannot disagree.
     *
     * <p>This is the invariant that makes the barrier safe. The plate's outer face is flush with the
     * block face, so "entirely beyond that face" and "cannot overlap the plate" are the same
     * statement. An entity is therefore never handed a shape it is already inside: the barrier can
     * stop motion into the cell, and can never push, trap or suffocate anything.</p>
     *
     * <p>The consequence at the other end is what keeps the launch working: an entity standing on
     * the pad, or flying out of the cell, still overlaps it, so the barrier simply does not exist
     * for it - no special case needed. It reappears only once the entity is clear of the plate,
     * behind it and moving away.</p>
     *
     * <p>The epsilon widens the zone where the wall exists rather than narrowing it, so an entity
     * pressed flush against the face keeps its wall instead of flickering; vanilla ignores overlaps
     * below 1e-7, so nothing is pushed out.</p>
     */
    private static boolean isBeyondFrontFace(Direction facing, BlockPos pos, AABB box) {
        return switch (facing) {
            case NORTH -> box.maxZ <= pos.getZ() + EPSILON;
            case SOUTH -> box.minZ >= pos.getZ() + 1.0 - EPSILON;
            case WEST -> box.maxX <= pos.getX() + EPSILON;
            case EAST -> box.minX >= pos.getX() + 1.0 - EPSILON;
            default -> false;
        };
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
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * The launch trigger. {@code entityInside} rather than {@code stepOn}/{@code fallOn}: those two
     * are dispatched on the block returned by {@code Entity.getOnPosLegacy()}, which probes
     * {@code floor(position.y - 0.2)}. Standing on a 1-pixel pad puts the feet at {@code y + 0.0625},
     * so the probe lands on the block <em>below</em> the ramp and the ramp never gets the callback.
     * Any block with a collision box thinner than 0.2 (3.2 px) has this problem - which is also why
     * dropping a carpet or another thin block on top "fixes" it: the extra height pushes the probe
     * back onto the ramp. {@code checkInsideBlocks()} instead uses the entity's real bounding box,
     * so it resolves the ramp correctly whether the entity walks on or falls onto it.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Zero the fall distance for anything inside the pad's cell, airborne or not. On the tick an
        // entity actually lands, vanilla has already run checkFallDamage (Entity.move line 665)
        // before checkInsideBlocks (line 777), so a reset here would be one tick late - but a hop
        // spends a tick or two descending through the cell first, and that is where this lands.
        // Effect: each hop starts its fall accounting from scratch, so chaining ramps cannot stack
        // up fall damage. A genuine long drop onto a ramp crosses the cell in a single tick and is
        // still punished, which is the intent: at worst one ramp's worth of fall is ever counted.
        entity.resetFallDistance();
        RampLaunchManager.tryLaunch(level, pos, state, entity);
    }
}
