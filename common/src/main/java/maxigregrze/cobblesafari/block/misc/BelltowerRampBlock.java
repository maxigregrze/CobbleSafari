package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

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
 * <p>Unrelated to {@link BelltowerTrellisBlock} beyond sharing two vanilla plank textures.</p>
 */
public class BelltowerRampBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<BelltowerRampBlock> CODEC = simpleCodec(BelltowerRampBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Selection and collision alike: a 1-pixel slab, identical in all four orientations. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

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
