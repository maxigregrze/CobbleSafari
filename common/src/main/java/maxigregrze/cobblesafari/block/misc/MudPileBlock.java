package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class MudPileBlock extends PileBlock {

    public static final MapCodec<MudPileBlock> CODEC = simpleCodec(MudPileBlock::new);

    public MudPileBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
