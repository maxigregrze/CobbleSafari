package maxigregrze.cobblesafari.rotomphone;

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
        if (isGloballyBlocked(player)) {
            return;
        }
        ItemStack phone = RotomPhoneEquipped.findPhoneForHandOrAccessory(player);
        if (phone.isEmpty() || !RotomPhoneItem.isRotoGlideEnabled(phone)) {
            return;
        }
        if (player.onGround()) {
            return;
        }
        PlayerState st = STATES.computeIfAbsent(player.getUUID(), u -> new PlayerState());
        if (st.consumedUntilLanding) {
            return;
        }
        if (st.pendingEndTick >= 0) {
            return;
        }
        st.pendingEndTick = player.getServer().getTickCount() + DELAY_TICKS;
        st.touchedGroundWhilePending = false;
        st.pendingHorizX = sanitizeHorizontal(clientMoveX);
        st.pendingHorizZ = sanitizeHorizontal(clientMoveZ);
    }
    private static void tick(ServerPlayer player) {
        PlayerState st = STATES.get(player.getUUID());
        if (st == null) {
            return;
        }
        int now = player.getServer().getTickCount();

        if (st.pendingEndTick >= 0) {
            if (player.onGround()) {
                st.touchedGroundWhilePending = true;
            }
            if (isGloballyBlocked(player)) {
                clearPending(st);
            } else if (now >= st.pendingEndTick) {
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
            return;
        }
        if (player.onGround() || touchedDuringWait) {
            return;
        }
        ItemStack phone = RotomPhoneEquipped.findPhoneForHandOrAccessory(player);
        if (phone.isEmpty() || !RotomPhoneItem.isRotoGlideEnabled(phone)) {
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
        int jumps = maxigregrze.cobblesafari.init.ModStats.awardAndGet(
                player, maxigregrze.cobblesafari.init.ModStats.ROTO_GLIDE_JUMPS);
        maxigregrze.cobblesafari.advancement.ModCriteria.ROTO_GLIDE.trigger(player, jumps);
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
        return player.isFallFlying();
    }

    private static final class PlayerState {
        int pendingEndTick = -1;
        boolean touchedGroundWhilePending;
        boolean consumedUntilLanding;
        double pendingHorizX;
        double pendingHorizZ;
    }
}
