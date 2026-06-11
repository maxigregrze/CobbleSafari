package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.effect.RedShackledSynced;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a client-replicated "shackled" flag to every {@link LivingEntity}. Set server-side by
 * {@link maxigregrze.cobblesafari.effect.RedShackledEffectHandler}; read client-side by the chain
 * renderer through {@link RedShackledSynced}. See {@link RedShackledSynced} for why the raw effect
 * list cannot be used on the client.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityShackledSyncMixin implements RedShackledSynced {

    @Unique
    private static final EntityDataAccessor<Boolean> COBBLESAFARI$SHACKLED =
            SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void cobblesafari$defineShackledData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(COBBLESAFARI$SHACKLED, false);
    }

    @Override
    public void cobblesafari$setShackledSynced(boolean shackled) {
        SynchedEntityData data = ((Entity) (Object) this).getEntityData();
        if (data.get(COBBLESAFARI$SHACKLED) != shackled) {
            data.set(COBBLESAFARI$SHACKLED, shackled);
        }
    }

    @Override
    public boolean cobblesafari$isShackledSynced() {
        return ((Entity) (Object) this).getEntityData().get(COBBLESAFARI$SHACKLED);
    }
}
