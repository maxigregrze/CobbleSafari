package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class RotomPhoneSafetyTick {

    /**
     * How long the guard lasts once applied. Kept short so it never lingers on the ground for more
     * than a moment (a lingering guard would keep suppressing e.g. attack crits). Long enough that a
     * single fall never needs more than one refresh.
     */
    private static final int ROTO_FALL_DURATION_TICKS = 40;

    /** Re-apply only when the remaining duration drops below this, to avoid a per-tick client resync. */
    private static final int REFRESH_BELOW_TICKS = 20;

    private RotomPhoneSafetyTick() {}

    public static void tryApplyRotoFall(ServerPlayer player) {
        if (player.onGround()) {
            // Landed: drop the guard immediately rather than paying for a separate ground-clear tick.
            if (player.hasEffect(ModEffects.ROTO_FALL.holder)) {
                player.removeEffect(ModEffects.ROTO_FALL.holder);
            }
            return;
        }
        if (player.isFallFlying()) {
            return;
        }
        double vy = player.getDeltaMovement().y;
        if (vy >= 0.0) {
            // Rising or level: no imminent fall to protect against.
            return;
        }
        // Only guard once the fall is about to become long enough to actually deal damage. Predict
        // next tick's fall distance (current speed is a conservative lower bound on the next drop) so
        // the effect is already present when causeFallDamage fires, without triggering on jumps.
        double safeFallDistance = player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        double predictedFallDistance = player.fallDistance + (-vy);
        if (predictedFallDistance <= safeFallDistance) {
            return;
        }
        MobEffectInstance current = player.getEffect(ModEffects.ROTO_FALL.holder);
        if (current == null || current.getDuration() < REFRESH_BELOW_TICKS) {
            player.addEffect(new MobEffectInstance(
                    ModEffects.ROTO_FALL.holder, ROTO_FALL_DURATION_TICKS, 0, false, false, true));
        }
    }
}
