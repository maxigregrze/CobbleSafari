package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.LayeredVerticalConnectedModelBlock;
import maxigregrze.cobblesafari.block.base.Waterlogged;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/**
 * Belltower rod: a stackable ornamental pole whose model alternates floor by floor and is always
 * capped by a finial at the top of its run.
 *
 * <p>All of that comes from {@link LayeredVerticalConnectedModelBlock}. This class exists only to
 * carry {@code WATERLOGGED}, which a shared socle cannot declare optionally - see
 * {@link Waterlogged}. It holds no logic of its own.</p>
 */
public class BelltowerRodBlock extends LayeredVerticalConnectedModelBlock implements SimpleWaterloggedBlock {

    /** Kept because the parent's own {@code layerCount} is private; {@code settings} is inherited. */
    private final int rodLayerCount;

    public BelltowerRodBlock(Properties properties, Settings settings, int layerCount) {
        super(properties, settings, layerCount);
        this.rodLayerCount = layerCount;
        this.registerDefaultState(this.defaultBlockState().setValue(Waterlogged.WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new BelltowerRodBlock(props, this.settings, this.rodLayerCount));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(Waterlogged.WATERLOGGED);
    }

    /**
     * The parent returns {@code computeState(defaultBlockState(), ...)} and never looks at fluids,
     * so the waterlog has to be applied on top of its result rather than instead of it.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(Waterlogged.WATERLOGGED, Waterlogged.atPlacement(context));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Waterlogged.fluidState(state, super.getFluidState(state));
    }

    /** The parent's updateShape recomputes the connection state; the fluid tick is scheduled first. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Waterlogged.scheduleTick(state, level, pos);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
