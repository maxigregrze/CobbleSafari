package maxigregrze.cobblesafari.block.trap;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.misc.WhirlwindWindBurst;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class WindTrapBlock extends TrapBlock {

    public static final MapCodec<WindTrapBlock> CODEC = simpleCodec(props -> new WindTrapBlock(props, false));

    public WindTrapBlock(Properties properties, boolean hard) {
        super(properties, hard);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** Extra synced impulse applied by the hard variant on top of the wind-charge burst. */
    private static final double HARD_HORIZONTAL = 1.6;
    private static final double HARD_VERTICAL = 0.9;

    @Override
    protected void onTriggered(ServerLevel level, BlockPos pos, BlockState state, @Nullable LivingEntity activator) {
        Vec3 center = Vec3.atCenterOf(pos);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.NEUTRAL, 1.0f, 1.0f);
        new WhirlwindWindBurst(level, center).burst(center);
        if (hard && activator != null) {
            Vec3 diff = activator.position().subtract(center);
            Vec3 horizontal = diff.lengthSqr() > 1.0E-4
                    ? new Vec3(diff.x, 0, diff.z).normalize()
                    : new Vec3(0, 0, 0);
            Vec3 away = new Vec3(horizontal.x * HARD_HORIZONTAL, HARD_VERTICAL, horizontal.z * HARD_HORIZONTAL);
            // setDeltaMovement + hurtMarked so the knockback is actually sent to (player) clients.
            activator.setDeltaMovement(activator.getDeltaMovement().add(away));
            activator.hurtMarked = true;
        }
    }
}
