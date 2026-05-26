package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Display-only beam model for {@link UnionRoomSpotlightBlockEntity}; not placed in world.
 */
public class UnionRoomSpotlightDisplayLightBlock extends Block implements UnionRoomColoredBlock {

    public static final MapCodec<UnionRoomSpotlightDisplayLightBlock> CODEC = simpleCodec(UnionRoomSpotlightDisplayLightBlock::new);

    public UnionRoomSpotlightDisplayLightBlock(Properties properties) {
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
}
