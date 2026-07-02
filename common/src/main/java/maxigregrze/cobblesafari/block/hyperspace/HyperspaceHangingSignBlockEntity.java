package maxigregrze.cobblesafari.block.hyperspace;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hanging-sign block entity bound to the mod's own type. {@link HangingSignBlockEntity} hard-codes
 * the vanilla type in its constructor, so this subclass overrides {@link #getType()} to report the
 * Hyperspace type instead (the persisted id then round-trips through the mod registry).
 */
public class HyperspaceHangingSignBlockEntity extends HangingSignBlockEntity {

    public HyperspaceHangingSignBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.HYPERSPACE_HANGING_SIGN;
    }
}
