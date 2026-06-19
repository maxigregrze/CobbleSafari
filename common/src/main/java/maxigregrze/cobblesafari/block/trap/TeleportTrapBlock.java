package maxigregrze.cobblesafari.block.trap;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

public class TeleportTrapBlock extends TrapBlock {

    public static final MapCodec<TeleportTrapBlock> CODEC = simpleCodec(props -> new TeleportTrapBlock(props, false));

    public TeleportTrapBlock(Properties properties, boolean hard) {
        super(properties, hard);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onTriggered(ServerLevel level, BlockPos pos, BlockState state, @Nullable LivingEntity activator) {
        if (activator == null) {
            return;
        }
        double r = hard ? 32 : 8;
        for (int i = 0; i < 16; i++) {
            double nx = activator.getX() + (activator.getRandom().nextDouble() - 0.5) * 2 * r;
            double ny = Mth.clamp(activator.getY() + (activator.getRandom().nextInt((int) (2 * r + 1)) - r),
                    level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
            double nz = activator.getZ() + (activator.getRandom().nextDouble() - 0.5) * 2 * r;
            if (activator.randomTeleport(nx, ny, nz, true)) {
                level.gameEvent(GameEvent.TELEPORT, activator.position(), GameEvent.Context.of(activator));
                level.playSound(null, nx, ny, nz, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.BLOCKS, 1.0f, 1.0f);
                break;
            }
        }
    }
}
