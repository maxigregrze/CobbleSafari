package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.manager.TimerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "changeDimension", at = @At("RETURN"))
    private void onChangeDimension(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        TimerManager.pauseAllTimers(self);

        Optional<String> configuredDimension = TimerManager.getConfiguredDimensionId(self);
        if (configuredDimension.isPresent()) {
            TimerManager.startTimer(self, configuredDimension.get());
        }
    }
}
