package maxigregrze.cobblesafari.effect;

import maxigregrze.cobblesafari.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Bridges the loader-specific entity tracking events to the shackle replication, so the loader
 * modules stay simple adapters.
 *
 * <p>Both hooks are guarded by {@link RedShackledEffects#isShackled(LivingEntity)} first: start
 * tracking fires for every entity entering a player's view distance, so in the nominal case - i.e.
 * always, outside the few seconds an entrave lasts - these methods cost one {@code hasEffect} and
 * send nothing.</p>
 *
 * <p>A player never tracks itself, so a shackled player is not covered here; its own client reads
 * the effect locally (vanilla syncs a player's own effect list to it).</p>
 */
public final class RedShackledTrackingSync {

    private RedShackledTrackingSync() {}

    /** A player starts tracking the entity: hand it the ongoing shackle, if any. */
    public static void onStartTracking(Entity tracked, ServerPlayer viewer) {
        if (tracked instanceof LivingEntity living && RedShackledEffects.isShackled(living)) {
            ModNetworking.sendShackleSync(viewer, living, true);
        }
    }

    /** A player stops tracking the entity: purge it from that client's store if it was in it. */
    public static void onStopTracking(Entity tracked, ServerPlayer viewer) {
        if (tracked instanceof LivingEntity living && RedShackledEffects.isShackled(living)) {
            ModNetworking.sendShackleSync(viewer, living, false);
        }
    }
}
