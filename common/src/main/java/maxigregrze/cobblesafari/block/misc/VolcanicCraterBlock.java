package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.safari.hazard.SafariHazardLib;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class VolcanicCraterBlock extends SafariCraterBlock {

    public static final MapCodec<VolcanicCraterBlock> CODEC = simpleCodec(VolcanicCraterBlock::new);
    private static final int COOLDOWN_TICKS = 160;

    public VolcanicCraterBlock(Properties properties) {
        super(properties, COOLDOWN_TICKS);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlayerTouch(ServerLevel level, BlockPos pos, ServerPlayer player) {
        SafariHazardLib.startFireShadow(level, spawnPosition(pos), player);
    }
}
