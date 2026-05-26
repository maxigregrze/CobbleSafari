package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

import static maxigregrze.cobblesafari.block.misc.UnionRoomColoredBlocks.applyColorFromStack;

public class UnionRoomBricksBlock extends Block implements UnionRoomColoredBlock {

    public static final MapCodec<UnionRoomBricksBlock> CODEC = simpleCodec(UnionRoomBricksBlock::new);

    public UnionRoomBricksBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(COLOR, UnionRoomColor.GREEN));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return applyColorFromStack(this.defaultBlockState(), context.getItemInHand());
    }
}
