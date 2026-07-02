package maxigregrze.cobblesafari.block.hyperspace;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/** Wall-mounted Hyperspace sign; shares the mod's sign block-entity type. */
public class HyperspaceWallSignBlock extends WallSignBlock {

    public HyperspaceWallSignBlock(WoodType type, Properties properties) {
        super(type, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SignBlockEntity(ModBlockEntities.HYPERSPACE_SIGN, pos, state);
    }
}
