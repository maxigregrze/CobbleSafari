package maxigregrze.cobblesafari.block.pokestatue;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.Waterlogged;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Creative-only display statue: a single full block that renders any Cobblemon species, with no
 * pedestal, no waxing and no placement conditions.
 *
 * <p>Hardness is bedrock's ({@code -1} / {@code 3600000}): indestructible and blast-proof in
 * survival and adventure, still breakable by a creative player. The block state is just
 * {@code FACING} - the texture mode lives in the block entity and drives nothing that the light
 * engine or the model baker need to see - plus {@code WATERLOGGED}, so a statue can stand under water
 * (plan 194).</p>
 */
public class CreativeStatueBlock extends AbstractPokeStatueBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<CreativeStatueBlock> CODEC = simpleCodec(CreativeStatueBlock::new);

    public CreativeStatueBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(Waterlogged.WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends AbstractPokeStatueBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, Waterlogged.WATERLOGGED);
    }

    @Override
    public BlockPos dataPos(BlockState state, BlockPos pos) {
        return pos;
    }

    /** No pedestal to stand on: the model starts at the very bottom of the block. */
    @Override
    public double modelOriginY() {
        return 0.0;
    }

    /** Selection and collision are both the full cube; the model itself is renderer-drawn. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Placeable anywhere: no support, no headroom, no survivability check ever. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(Waterlogged.WATERLOGGED, Waterlogged.atPlacement(context));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Waterlogged.scheduleTick(state, level, pos);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Waterlogged.fluidState(state, super.getFluidState(state));
    }

    /**
     * Shift + right-click with an empty hand, in creative. Deliberately <em>not</em> the Tinkhammer
     * gesture: that one belongs to the regular statue, and this block is never available in survival.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.isCreative() || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return openConfig(level, pos, state, player)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeStatueBlockEntity(pos, state);
    }
}
