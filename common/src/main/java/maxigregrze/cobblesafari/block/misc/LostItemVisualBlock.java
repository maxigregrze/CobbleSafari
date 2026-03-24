package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class LostItemVisualBlock extends Block {
    public static final MapCodec<LostItemVisualBlock> CODEC = simpleCodec(LostItemVisualBlock::new);

    public LostItemVisualBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LostItemBlock.FACE, AttachFace.FLOOR)
                .setValue(LostItemBlock.FACING, Direction.NORTH)
                .setValue(LostItemBlock.HAS_ITEM, true));
    }

    @Override
    protected MapCodec<LostItemVisualBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LostItemBlock.FACE, LostItemBlock.FACING, LostItemBlock.HAS_ITEM);
    }
}
