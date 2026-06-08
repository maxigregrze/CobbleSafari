package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.safari.hazard.SafariHazardLib;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class DraconicCraterBlock extends SafariCraterBlock {

    public static final MapCodec<DraconicCraterBlock> CODEC = simpleCodec(DraconicCraterBlock::new);
    private static final int COOLDOWN_TICKS = 100;

    public DraconicCraterBlock(Properties properties) {
        super(properties, COOLDOWN_TICKS);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlayerTouch(ServerLevel level, BlockPos pos, ServerPlayer player) {
        SafariHazardLib.launchDracoMeteor(level, spawnPosition(pos), level.getRandom());
    }
}
