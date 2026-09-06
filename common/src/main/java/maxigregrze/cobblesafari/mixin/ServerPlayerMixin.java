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
        if (configuredDimension.isEmpty()) {
            return;
        }
        String dimensionId = configuredDimension.get();

        // Non-instanced dimensions (Safari, Union Room) keep arming on arrival, as before. An
        // instanced one must NOT: arriving is not being admitted, and creating a timer here would
        // hand credit to anyone who found another way in (finding S1). The session is opened at the
        // door by DungeonTeleportHandler, and the presence sweep evicts whoever holds none.
        if (TimerManager.isInstancedDimension(dimensionId)) {
            return;
        }
        TimerManager.startTimer(self, dimensionId);
    }
}
