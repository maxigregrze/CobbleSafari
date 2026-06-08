package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Same logic, GUI, and {@link AuspiciousPokeballBlockEntity} as {@link AuspiciousPokeballBlock}; smaller visual variant.
 */
public class AuspiciousPokeballSmallBlock extends AuspiciousPokeballBlock {

    public static final MapCodec<AuspiciousPokeballSmallBlock> CODEC = simpleCodec(AuspiciousPokeballSmallBlock::new);

    public AuspiciousPokeballSmallBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
