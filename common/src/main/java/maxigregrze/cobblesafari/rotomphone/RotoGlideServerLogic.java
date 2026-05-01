package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.item.RotomPhoneItem;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RotoGlideServerLogic {

    private static final int DELAY_TICKS = 6;
    private static final double JUMP_HEIGHT_MULTIPLIER = Math.sqrt(2.0);

    private static final Map<UUID, PlayerState> STATES = new ConcurrentHashMap<>();

    private RotoGlideServerLogic() {}

    public static void removeState(UUID playerId) {
        STATES.remove(playerId);
    }

    public static void tickAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tick(player);
        }
    }

    public static void onRotoGlideRequest(ServerPlayer player, double clientMoveX, double clientMoveZ) {
        CobbleSafari.LOGGER.info("[RotoGlide] Jump key request received | {} | clientHoriz=({}, {})",
                formatPlayer(player), clientMoveX, clientMoveZ);
        if (isGloballyBlocked(player)) {
            CobbleSafari.LOGGER.info("[RotoGlide] Request ignored (blocked) | {} | reason={}", formatPlayer(player), blockReason(player));
            return;
        }
        ItemStack phone = RotomPhoneEquipped.findPhoneForHandOrAccessory(player);
        if (phone.isEmpty() || !RotomPhoneItem.isRotoGlideEnabled(phone)) {
            boolean glideOn = !phone.isEmpty() && RotomPhoneItem.isRotoGlideEnabled(phone);
            CobbleSafari.LOGGER.info("[RotoGlide] Request ignored (no phone or Roto-Glide off) | {} | phoneEmpty={} rotoGlideEnabledOnStack={}",
                    formatPlayer(player), phone.isEmpty(), glideOn);
            return;
        }
        if (player.onGround()) {
            CobbleSafari.LOGGER.info("[RotoGlide] Request ignored (on ground) | {}", formatPlayer(player));
            return;
        }
        PlayerState st = STATES.computeIfAbsent(player.getUUID(), u -> new PlayerState());
        if (st.consumedUntilLanding) {
            CobbleSafari.LOGGER.info("[RotoGlide] Request ignored (already used this air) | {}", formatPlayer(player));
            return;
        }
        if (st.pendingEndTick >= 0) {
            CobbleSafari.LOGGER.info("[RotoGlide] Request ignored (delay already pending until tick {}) | {}",
                    st.pendingEndTick, formatPlayer(player));
            return;
        }
        int endTick = player.getServer().getTickCount() + DELAY_TICKS;
        st.pendingEndTick = endTick;
        st.touchedGroundWhilePending = false;
        st.pendingHorizX = sanitizeHorizontal(clientMoveX);
        st.pendingHorizZ = sanitizeHorizontal(clientMoveZ);
        CobbleSafari.LOGGER.info("[RotoGlide] Delay started ({} ticks) | {} | pendingEndTick={} storedHoriz=({}, {})",
                DELAY_TICKS, formatPlayer(player), endTick, st.pendingHorizX, st.pendingHorizZ);
    }
    private static void tick(ServerPlayer player) {
        PlayerState st = STATES.computeIfAbsent(player.getUUID(), u -> new PlayerState());
        int now = player.getServer().getTickCount();

        if (st.pendingEndTick >= 0) {
            if (player.onGround()) {
                st.touchedGroundWhilePending = true;
            }
            if (isGloballyBlocked(player)) {
                CobbleSafari.LOGGER.info("[RotoGlide] Delay cancelled (blocked mid-wait) | {} | reason={}", formatPlayer(player), blockReason(player));
                clearPending(st);
            } else if (now >= st.pendingEndTick) {
                CobbleSafari.LOGGER.info("[RotoGlide] {} ticks elapsed, resolving | {} | serverTick={} pendingEndTick={} touchedGroundDuringWait={}",
                        DELAY_TICKS, formatPlayer(player), now, st.pendingEndTick, st.touchedGroundWhilePending);
                resolvePending(st, player);
            }
            return;
        }

        if (player.onGround()) {
            st.consumedUntilLanding = false;
        }
    }

    private static void resolvePending(PlayerState st, ServerPlayer player) {
        boolean touchedDuringWait = st.touchedGroundWhilePending;
        double savedHorizX = st.pendingHorizX;
        double savedHorizZ = st.pendingHorizZ;
        clearPending(st);
        if (isGloballyBlocked(player)) {
            CobbleSafari.LOGGER.info("[RotoGlide] Jump not applied (blocked at resolve) | {} | reason={}", formatPlayer(player), blockReason(player));
            return;
        }
        if (player.onGround() || touchedDuringWait) {
            CobbleSafari.LOGGER.info("[RotoGlide] Jump not applied | {} | onGround={} touchedGroundDuringWait={}",
                    formatPlayer(player), player.onGround(), touchedDuringWait);
            return;
        }
        ItemStack phone = RotomPhoneEquipped.findPhoneForHandOrAccessory(player);
        if (phone.isEmpty() || !RotomPhoneItem.isRotoGlideEnabled(phone)) {
            CobbleSafari.LOGGER.info("[RotoGlide] Jump not applied (phone missing or Roto-Glide off) | {}", formatPlayer(player));
            return;
        }
        applyBoost(player, savedHorizX, savedHorizZ);
        st.consumedUntilLanding = true;
    }
    private static void clearPending(PlayerState st) {
        st.pendingEndTick = -1;
        st.touchedGroundWhilePending = false;
    }

    private static void applyBoost(ServerPlayer player, double fromClientX, double fromClientZ) {
        double jump = vanillaJumpVerticalVelocity(player);
        double vy = jump * JUMP_HEIGHT_MULTIPLIER;
        Vec3 srv = player.getDeltaMovement();
        double hx = pickStrongerHorizontal(fromClientX, srv.x);
        double hz = pickStrongerHorizontal(fromClientZ, srv.z);
        player.setDeltaMovement(hx, vy, hz);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), player.getDeltaMovement()));
        player.resetFallDistance();
        CobbleSafari.LOGGER.info("[RotoGlide] Jump applied | {} | baseJumpVy={} boostedVy={} horiz=({}, {}) (client {}, {} vs server {}, {})",
                formatPlayer(player), jump, vy, hx, hz, fromClientX, fromClientZ, srv.x, srv.z);
    }

    private static double pickStrongerHorizontal(double fromClient, double fromServer) {
        return Math.abs(fromServer) > Math.abs(fromClient) ? fromServer : fromClient;
    }

    private static double sanitizeHorizontal(double v) {
        if (!Double.isFinite(v)) {
            return 0.0;
        }
        double cap = 20.0;
        if (v > cap) {
            return cap;
        }
        if (v < -cap) {
            return -cap;
        }
        return v;
    }
    private static double vanillaJumpVerticalVelocity(ServerPlayer player) {
        double jumpStrength = player.getAttributeValue(Attributes.JUMP_STRENGTH);
        double jumpBoost = 0.0;
        MobEffectInstance jump = player.getEffect(MobEffects.JUMP);
        if (jump != null) {
            jumpBoost = 0.1 * (jump.getAmplifier() + 1);
        }
        return jumpStrength + jumpBoost;
    }
    public static boolean isGloballyBlocked(Player player) {
        if (player.isSpectator()) {
            return true;
        }
        if (player.getAbilities().flying) {
            return true;
        }
        if (player.isFallFlying()) {
            return true;
        }
        return false;
    }

    private static String blockReason(Player player) {
        if (player.isSpectator()) {
            return "spectator";
        }
        if (player.getAbilities().flying) {
            return "creative_flight";
        }
        if (player.isFallFlying()) {
            return "elytra_gliding";
        }
        return "none";
    }

    private static String formatPlayer(ServerPlayer player) {
        Vec3 pos = player.position();
        Vec3 d = player.getDeltaMovement();
        return String.format(
                "name=%s uuid=%s dim=%s pos=(%.2f, %.2f, %.2f) vel=(%.3f, %.3f, %.3f) onGround=%s fallFlying=%s creativeFly=%s spectator=%s fallDist=%.2f",
                player.getGameProfile().getName(),
                player.getUUID(),
                player.level().dimension().location(),
                pos.x, pos.y, pos.z,
                d.x, d.y, d.z,
                player.onGround(),
                player.isFallFlying(),
                player.getAbilities().flying,
                player.isSpectator(),
                player.fallDistance
        );
    }
    private static final class PlayerState {
        int pendingEndTick = -1;
        boolean touchedGroundWhilePending;
        boolean consumedUntilLanding;
        double pendingHorizX;
        double pendingHorizZ;
    }
}
