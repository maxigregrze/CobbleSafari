package maxigregrze.cobblesafari.block.misc;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** Public-constructor wrapper around the protected vanilla {@link ButtonBlock} (NeoForm-mapped common). */
public class HyperspaceWoodButtonBlock extends ButtonBlock {
    public HyperspaceWoodButtonBlock(BlockSetType type, int ticksToStayPressed, BlockBehaviour.Properties properties) {
        super(type, ticksToStayPressed, properties);
    }
}
