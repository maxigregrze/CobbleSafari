package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Même logique, GUI et {@link AuspiciousPokeballBlockEntity} que {@link AuspiciousPokeballBlock} ; variante visuelle plus petite.
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
