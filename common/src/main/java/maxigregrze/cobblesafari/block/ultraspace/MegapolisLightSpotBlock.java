package maxigregrze.cobblesafari.block.ultraspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Megapolis Light Spot (action plan 190, B5): a repeater-like part that lights up as soon as it is
 * powered and passes a full-strength signal into the block in front of it, eight ticks late.
 *
 * <p>Two states, deliberately apart. {@link #POWERED} is the input and drives the look and the
 * light with no delay; {@link #EMITTING} is the output and trails it by {@link #DELAY_TICKS} in
 * both directions. As on a repeater, a pulse shorter than the delay comes out stretched to it
 * rather than lost.</p>
 *
 * <p>{@link #FACING} is the <b>front</b>: the face against the block it powers, drawn with the
 * {@code _facing} texture whatever the state. Input is read from all four horizontal sides, the
 * front included, but the front only ever counts a block that emits on its own - read the vanilla
 * way it would see this block's own output come back through the block it powers, or through a
 * dust line, and never switch off again.</p>
 */
public class MegapolisLightSpotBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<MegapolisLightSpotBlock> CODEC = simpleCodec(MegapolisLightSpotBlock::new);

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty EMITTING = BooleanProperty.create("emitting");

    /** Lag between the input and the output, each way. */
    public static final int DELAY_TICKS = 8;
    private static final int FULL_SIGNAL = 15;

    public MegapolisLightSpotBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, Boolean.FALSE)
                .setValue(EMITTING, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, EMITTING);
    }

    /** Placed like a repeater: the front points the way the player looks. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, hasInput(context.getLevel(), context.getClickedPos(), facing));
    }

    // ------------------------------------------------------------------ input

    /**
     * Back, left and right are read the vanilla way. The front reads only the neighbour's own
     * emission, never what it conducts and never a dust line: those are exactly the two paths by
     * which this block's output would feed back into its input.
     */
    private static boolean hasInput(Level level, BlockPos pos, Direction facing) {
        for (Direction side : new Direction[] {facing.getOpposite(), facing.getClockWise(), facing.getCounterClockWise()}) {
            if (level.getSignal(pos.relative(side), side) > 0) {
                return true;
            }
        }
        BlockPos frontPos = pos.relative(facing);
        BlockState front = level.getBlockState(frontPos);
        return !front.is(Blocks.REDSTONE_WIRE) && front.getSignal(level, frontPos, facing) > 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide()) {
            refreshInput(level, pos, state);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            refreshInput(level, pos, state);
        }
    }

    /** The look follows the input at once; the output is handed to a scheduled tick. */
    private void refreshInput(Level level, BlockPos pos, BlockState state) {
        boolean input = hasInput(level, pos, state.getValue(FACING));
        if (input != state.getValue(POWERED)) {
            state = state.setValue(POWERED, input);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        if (input != state.getValue(EMITTING) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, DELAY_TICKS);
        }
    }

    // ------------------------------------------------------------------ output

    /**
     * Repeater semantics: a tick always switches an idle output on - even if the input has already
     * gone, which is what stretches a short pulse - and books its own switch-off when nothing holds
     * it up. An output still held by its input is left alone.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean input = state.getValue(POWERED);
        if (state.getValue(EMITTING)) {
            if (!input) {
                setEmitting(level, pos, state, false);
            }
        } else {
            setEmitting(level, pos, state, true);
            if (!input) {
                level.scheduleTick(pos, this, DELAY_TICKS);
            }
        }
    }

    private void setEmitting(Level level, BlockPos pos, BlockState state, boolean emitting) {
        BlockState updated = state.setValue(EMITTING, emitting);
        level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        updateFront(level, pos, updated);
    }

    /** Same notification a repeater sends: the powered block, then everything around it. */
    private void updateFront(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos frontPos = pos.relative(facing);
        level.neighborChanged(frontPos, this, pos);
        level.updateNeighborsAtExceptFromFacing(frontPos, this, facing.getOpposite());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            if (state.getValue(EMITTING)) {
                updateFront(level, pos, state);
            }
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * {@code direction} is the side the asking block looks from, so the block in front asks with
     * {@code FACING.getOpposite()} - the same convention {@code DiodeBlock} answers.
     */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(EMITTING) && direction == state.getValue(FACING).getOpposite() ? FULL_SIGNAL : 0;
    }

    /** Strong as well as weak: the block in front is powered outright, like a repeater's. */
    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }
}
