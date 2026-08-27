package maxigregrze.cobblesafari.warpride;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.data.WarpRideSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Makes the rider invulnerable and immovable for a ride. Vanilla API only, so no mixin needed. */
public final class RiderBodyGuard {

    private RiderBodyGuard() {}

    /** @return the anchor position the rider will be pinned to */
    public static Vec3 apply(ServerPlayer player) {
        boolean priorInvulnerable = player.isInvulnerable();
        boolean priorNoGravity = player.isNoGravity();

        if (player.getServer() != null) {
            WarpRideSavedData.get(player.getServer())
                    .markRiding(player.getUUID(), priorInvulnerable, priorNoGravity);
        }

        player.setInvulnerable(true);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.hasImpulse = false;
        player.fallDistance = 0.0F;

        return player.position();
    }

    /** Re-asserts the anchor every server tick, correcting any displacement source after the fact. */
    public static void pin(ServerPlayer player, Vec3 anchor) {
        player.setDeltaMovement(Vec3.ZERO);
        player.hasImpulse = false;
        player.fallDistance = 0.0F;
        if (player.position().distanceToSqr(anchor) > 1.0E-4) {
            player.connection.teleport(anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
        }
    }

    /** Restores the <b>captured</b> prior flags, never a hard false, or a creative rider loses theirs. */
    public static void restore(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }
        WarpRideSavedData.PriorBody prior =
                WarpRideSavedData.get(player.getServer()).consume(player.getUUID());
        if (prior == null) {
            return;
        }
        player.setInvulnerable(prior.invulnerable());
        player.setNoGravity(prior.noGravity());
    }

    /** Login net for a server killed mid-ride, where no exit path ever ran. */
    public static void clearIfMarked(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }
        WarpRideSavedData data = WarpRideSavedData.get(player.getServer());
        if (!data.isMarked(player.getUUID())) {
            return;
        }
        WarpRideSavedData.PriorBody prior = data.consume(player.getUUID());
        if (prior != null) {
            player.setInvulnerable(prior.invulnerable());
            player.setNoGravity(prior.noGravity());
        }
        CobbleSafari.LOGGER.info("[WarpRide] {} logged in still flagged as riding; body protection "
                + "restored (the server was most likely stopped mid-ride)", player.getName().getString());
    }
}
