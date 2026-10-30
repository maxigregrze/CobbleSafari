package maxigregrze.cobblesafari.safari;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.entity.SafariDroneEntity;
import maxigregrze.cobblesafari.manager.TimerManager;
import maxigregrze.cobblesafari.network.ModNetworking;
import maxigregrze.cobblesafari.teleporter.TeleporterTickHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Leaving the Safari through a {@link SafariDroneEntity} (action plan 198, part G), the
 * replacement of the removed {@code /safariexit}. The teleport confirmation screen opens when the
 * player right-clicks a drone or stands still under one; moving while it is open cancels it, and
 * "Yes" takes the player back to where they entered, their remaining time untouched.
 *
 * <p>Server thread only.</p>
 */
public final class SafariDroneExitHandler {

    /** {@code TpAcceptResponsePayload} source routed here by both loaders. */
    public static final String SOURCE = "safari_exit";

    private static final String SAFARI_LOGO_ID = "domedimension";
    /** Three seconds of standing still under a drone (decision 12.5). */
    private static final int STAND_STILL_TICKS = 60;
    /** Horizontal movement, squared, that counts as moving: the dungeon countdown's 0.2 block. */
    private static final double MOVE_THRESHOLD_SQ = 0.04D;
    /** Diamond area: the drone's column and the four next to it (|dx| + |dz| <= 1). */
    private static final int AREA_RADIUS = 1;
    /** How far above the player's feet a drone may hover and still count as "above them". */
    private static final double MIN_DRONE_HEIGHT = 0.5D;
    private static final double MAX_DRONE_HEIGHT = 3.5D;

    /** Players whose screen is open, with where they stood when it opened. */
    private static final Map<UUID, Vec3> OPEN = new HashMap<>();
    /** Players standing under a drone, waiting for the five seconds. */
    private static final Map<UUID, StandStill> STANDING = new HashMap<>();
    /**
     * Players who already got the screen while under a drone: standing still does not ask them
     * again until they step out of the area (right-clicking still does).
     */
    private static final Set<UUID> LOCKED = new HashSet<>();

    private static final class StandStill {
        private Vec3 start;
        private int ticks;

        private StandStill(Vec3 start) {
            this.start = start;
        }
    }

    private SafariDroneExitHandler() {}

    /** Right-click on a drone. Ignored outside the Safari or while the screen is already open. */
    public static void requestExit(ServerPlayer player) {
        if (!isInSafari(player) || OPEN.containsKey(player.getUUID())) {
            return;
        }
        open(player);
    }

    private static void open(ServerPlayer player) {
        UUID id = player.getUUID();
        String dimensionName = Component.translatable("dimension.cobblesafari.domedimension").getString();
        ModNetworking.sendOpenTpAccept(player, dimensionName, SAFARI_LOGO_ID, false, false, 0, "", SOURCE, false);
        OPEN.put(id, player.position());
        STANDING.remove(id);
        LOCKED.add(id);
    }

    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (!isInSafari(player)) {
                clear(id);
                continue;
            }
            Vec3 openedAt = OPEN.get(id);
            if (openedAt != null) {
                // Like every teleport screen: moving while it is open cancels it.
                if (horizontalDistanceSq(player.position(), openedAt) > MOVE_THRESHOLD_SQ) {
                    OPEN.remove(id);
                    ModNetworking.sendCloseTpAccept(player);
                    player.sendSystemMessage(Component.translatable("cobblesafari.safari_drone.cancelled"));
                }
                continue;
            }
            tickStandStill(player, id);
        }
    }

    private static void tickStandStill(ServerPlayer player, UUID id) {
        if (!isUnderDrone(player)) {
            STANDING.remove(id);
            LOCKED.remove(id);
            return;
        }
        if (LOCKED.contains(id)) {
            return;
        }
        StandStill standing = STANDING.computeIfAbsent(id, k -> new StandStill(player.position()));
        if (horizontalDistanceSq(player.position(), standing.start) > MOVE_THRESHOLD_SQ) {
            standing.start = player.position();
            standing.ticks = 0;
            return;
        }
        if (++standing.ticks >= STAND_STILL_TICKS) {
            open(player);
        }
    }

    /** Whether a live drone hovers over the diamond area around the player's column. */
    private static boolean isUnderDrone(ServerPlayer player) {
        double feet = player.getY();
        AABB search = new AABB(
                player.getX() - AREA_RADIUS - 1.0D, feet + MIN_DRONE_HEIGHT, player.getZ() - AREA_RADIUS - 1.0D,
                player.getX() + AREA_RADIUS + 1.0D, feet + MAX_DRONE_HEIGHT, player.getZ() + AREA_RADIUS + 1.0D);
        for (SafariDroneEntity drone : player.serverLevel().getEntitiesOfClass(SafariDroneEntity.class, search, Entity::isAlive)) {
            int dx = Math.abs(drone.getBlockX() - player.getBlockX());
            int dz = Math.abs(drone.getBlockZ() - player.getBlockZ());
            double height = drone.getY() - feet;
            if (dx + dz <= AREA_RADIUS && height >= MIN_DRONE_HEIGHT && height <= MAX_DRONE_HEIGHT) {
                return true;
            }
        }
        return false;
    }

    /** The screen's answer ({@link #SOURCE}). "No" only forgets the request. */
    public static void handleAcceptResponse(ServerPlayer player, boolean accepted) {
        if (OPEN.remove(player.getUUID()) == null || !accepted || !isInSafari(player)) {
            return;
        }
        if (exit(player)) {
            // They land on their entry teleporter: don't offer them the Safari straight back.
            TeleporterTickHandler.setTeleportCooldown(player);
            player.sendSystemMessage(Component.translatable("cobblesafari.safari_drone.exited"));
            CobbleSafari.LOGGER.info("Player {} left the Safari through a Safari drone", player.getName().getString());
        } else {
            ModNetworking.sendCloseTpAccept(player);
            player.sendSystemMessage(Component.translatable("cobblesafari.safari_drone.failed"));
        }
    }

    /**
     * Back to the entry point, or to the overworld spawn when it is missing or refuses them, as on
     * timer expiry (decision 12.6). The time is never touched: the dimension change pauses it.
     */
    private static boolean exit(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        PlayerTimerData data = TimerManager.getOrCreateAccount(player, SafariTimerConfig.getSafariDimensionId());
        BlockPos originPos = data.getOriginPos();
        ServerLevel originLevel = data.getOriginDimension() == null ? null : server.getLevel(data.getOriginDimension());
        if (originPos != null && originLevel != null) {
            if (relocate(player, originLevel, originPos)) {
                return true;
            }
            // A discarded player was already disconnected by relocate: a second try would fail too.
            if (player.isRemoved()) {
                return false;
            }
        }
        return overworld != null && relocate(player, overworld, overworld.getSharedSpawnPos());
    }

    private static boolean relocate(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return TimerManager.relocate(player, level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, "safari drone exit");
    }

    private static boolean isInSafari(ServerPlayer player) {
        return player.level().dimension().location().equals(SafariTimerConfig.getSafariDimensionRL());
    }

    private static double horizontalDistanceSq(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    public static void clear(UUID playerId) {
        OPEN.remove(playerId);
        STANDING.remove(playerId);
        LOCKED.remove(playerId);
    }
}
