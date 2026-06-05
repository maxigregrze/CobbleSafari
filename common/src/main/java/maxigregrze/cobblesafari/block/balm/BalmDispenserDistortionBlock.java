package maxigregrze.cobblesafari.block.balm;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BalmDispenserDistortionBlock extends BalmDispenserBlock {

    public static final MapCodec<BalmDispenserDistortionBlock> CODEC = simpleCodec(BalmDispenserDistortionBlock::new);

    public BalmDispenserDistortionBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BalmDispenserBlock> codec() {
        return CODEC;
    }

    @Override
    public Item getDispensedItem() {
        return ModItems.BALM_DISTORTION;
    }
}
