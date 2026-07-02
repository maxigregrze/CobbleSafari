package maxigregrze.cobblesafari.block.hyperspace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/** Wall-mounted Hyperspace hanging sign; uses the mod's hanging-sign block entity. */
public class HyperspaceWallHangingSignBlock extends WallHangingSignBlock {

    public HyperspaceWallHangingSignBlock(WoodType type, Properties properties) {
        super(type, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HyperspaceHangingSignBlockEntity(pos, state);
    }
}
