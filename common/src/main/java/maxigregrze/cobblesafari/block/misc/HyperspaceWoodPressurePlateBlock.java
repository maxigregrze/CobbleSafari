package maxigregrze.cobblesafari.block.misc;

import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** Public-constructor wrapper around the protected vanilla {@link PressurePlateBlock} (NeoForm-mapped common). */
public class HyperspaceWoodPressurePlateBlock extends PressurePlateBlock {
    public HyperspaceWoodPressurePlateBlock(BlockSetType type, BlockBehaviour.Properties properties) {
        super(type, properties);
    }
}
