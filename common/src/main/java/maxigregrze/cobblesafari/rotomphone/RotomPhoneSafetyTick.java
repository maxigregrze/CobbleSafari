package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public final class RotomPhoneSafetyTick {

    private static final int ROTO_FALL_DURATION_TICKS = 20;

    private RotomPhoneSafetyTick() {}

    public static void tryApplyRotoFall(ServerPlayer player) {
        if (player.onGround()) {
            return;
        }
        if (player.isFallFlying()) {
            return;
        }
        if (player.fallDistance > 1.0f && player.getDeltaMovement().y < -0.1) {
            player.addEffect(new MobEffectInstance(ModEffects.ROTO_FALL.holder, ROTO_FALL_DURATION_TICKS, 0, false, false, true));
        }
    }
}
