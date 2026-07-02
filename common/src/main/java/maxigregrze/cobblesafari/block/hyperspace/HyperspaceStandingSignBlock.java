package maxigregrze.cobblesafari.block.hyperspace;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * Standing Hyperspace sign. Identical to the vanilla {@link StandingSignBlock} but binds the
 * mod's own sign {@link net.minecraft.world.level.block.entity.BlockEntityType} so the four
 * Hyperspace sign blocks share a single registered block-entity type.
 */
public class HyperspaceStandingSignBlock extends StandingSignBlock {

    public HyperspaceStandingSignBlock(WoodType type, Properties properties) {
        super(type, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SignBlockEntity(ModBlockEntities.HYPERSPACE_SIGN, pos, state);
    }
}
