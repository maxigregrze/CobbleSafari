package maxigregrze.cobblesafari.mixin;

import com.cobblemon.mod.common.api.multiblock.MultiblockBlock;
import com.cobblemon.mod.common.block.MonitorBlock;
import maxigregrze.cobblesafari.block.misc.HauntedMonitorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Gives Cobblemon's data monitor a random tick, so that a lit candle on it can draw the lightning
 * that haunts it (action plan 198, part I). Neither {@code MonitorBlock} nor {@code MultiblockBlock}
 * declares these two methods: they are added as overrides, not overwritten.
 */
@Mixin(MonitorBlock.class)
public abstract class MonitorBlockHauntMixin extends MultiblockBlock {

    protected MonitorBlockHauntMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HauntedMonitorBlock.tryHaunt(state, level, pos, random);
    }
}
