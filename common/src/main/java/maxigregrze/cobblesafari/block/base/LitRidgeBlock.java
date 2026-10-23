package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/** A {@link RidgeBlock} carrying {@link DaylightLit#LIT}, like {@link LitStairBlock}. See {@link DaylightLit}. */
public class LitRidgeBlock extends RidgeBlock {

    private final BlockState litBaseState;

    public LitRidgeBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
        this.litBaseState = baseState;
        this.registerDefaultState(this.defaultBlockState().setValue(DaylightLit.LIT, false));
    }

    @Override
    public MapCodec<? extends StairBlock> codec() {
        return simpleCodec(props -> new LitRidgeBlock(this.litBaseState, props));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DaylightLit.LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(DaylightLit.LIT, DaylightLit.shouldBeLit(context.getLevel()));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        DaylightLit.tick(state, level, pos);
    }
}
