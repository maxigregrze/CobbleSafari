package maxigregrze.cobblesafari.warpride;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.UltraWarpRideConfig;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.warpride.logic.Wave;
import maxigregrze.cobblesafari.warpride.logic.WarpRideDestinationRegistry;
import maxigregrze.cobblesafari.warpride.logic.WaveObject;
import maxigregrze.cobblesafari.warpride.network.WarpRidePayloads;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns every live {@link WarpRideSession}, keyed by <b>player UUID, not session UUID</b>: two
 * concurrent odometers for one player would be a defect.
 */
public final class WarpRideManager {

    private WarpRideManager() {}

    /** How often the authoritative state snapshot is pushed, in ticks. */
    private static final int STATE_SYNC_INTERVAL = 2;

    private static final Map<UUID, WarpRideSession> SESSIONS = new ConcurrentHashMap<>();

    public static WarpRideSession getSession(UUID playerId) {
        return SESSIONS.get(playerId);
    }

    public static boolean isRiding(UUID playerId) {
        return SESSIONS.containsKey(playerId);
    }

    public static int activeCount() {
        return SESSIONS.size();
    }

    public static List<WarpRideSession> allSessions() {
        return List.copyOf(SESSIONS.values());
    }


    /** Why a ride could not be opened. {@link OpenResult#OK} carries the created session. */
    public enum OpenResult {
        OK,
        ALREADY_RIDING,
        NO_DESTINATION
    }

    /** The single entry point a caller uses to start a ride, with or without a parent context. */
    public static OpenResult open(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return OpenResult.NO_DESTINATION;
        }
        if (SESSIONS.containsKey(player.getUUID())) {
            return OpenResult.ALREADY_RIDING;
        }
        if (WarpRideDestinationRegistry.isEmpty()) {
            return OpenResult.NO_DESTINATION;
        }

        UltraWarpRideConfig cfg = UltraWarpRideConfig.get();
        long now = server.getTickCount();
        UUID sessionId = UUID.randomUUID();
        long seed = player.level().getRandom().nextLong();

        WarpRideSession session = new WarpRideSession(
                sessionId, player.getUUID(), seed, cfg, RiderBodyGuard.apply(player), now);
        SESSIONS.put(player.getUUID(), session);

        Services.PLATFORM.sendPayloadToPlayer(player, new WarpRidePayloads.OpenPayload(
                sessionId,
                true,
                cfg.baseSpeed,
                cfg.minSpeed,
                cfg.maxSpeed,
                cfg.waveSpacingLy,
                cfg.visibleDepthLy,
                cfg.ringCount,
                cfg.projectionK,
                cfg.startEnergy,
                cfg.inputCooldownMs,
                cfg.jumpTransitMs,
                cfg.ringFourRadius,
                cfg.fifthPlaneDepth,
                cfg.flyPastMaxScale,
                cfg.starCount,
                cfg.virtualCanvasWidth,
                cfg.virtualCanvasHeight,
                cfg.lagBudgetTicks,
                cfg.laneRotationMs,
                cfg.portalHoldWaves,
                cfg.driftSeconds,
                cfg.warpAnimSeconds,
                cfg.fadeToBlackSeconds));

        syncDestinations(player);
        // The mini-game's own music takes top priority for the whole session.
        maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onWarpRideStart(player, resolveWarpMusicId());
        CobbleSafari.LOGGER.info("[WarpRide] Opened a ride for {} (session {})",
                player.getName().getString(), sessionId);
        return OpenResult.OK;
    }

    /** The debug command's door, kept distinct so removing the command is a mechanical deletion. */
    public static OpenResult openDebugSession(ServerPlayer player) {
        return open(player);
    }

    /** The mini-game music id from config, qualified to {@code cobblesafari:} when unnamespaced. */
    private static String resolveWarpMusicId() {
        String id = maxigregrze.cobblesafari.config.MiscConfig.getUltraWarpDiveCSMusic();
        if (id == null || id.isBlank()) {
            return "";
        }
        return id.contains(":") ? id : "cobblesafari:" + id.trim();
    }

    public static void syncDestinations(ServerPlayer player) {
        List<WarpRidePayloads.TintData> tints = new ArrayList<>();
        for (int[] pair : WarpRideDestinationRegistry.tintSyncView()) {
            tints.add(new WarpRidePayloads.TintData(pair[0], pair[1]));
        }
        Services.PLATFORM.sendPayloadToPlayer(player, new WarpRidePayloads.DestinationSyncPayload(tints));
    }

    public static void syncDestinationsToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncDestinations(player);
        }
    }


    public static void tickAll(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        long watchdogTicks = UltraWarpRideConfig.get().sessionWatchdogSeconds * 20L;

        for (Map.Entry<UUID, WarpRideSession> entry : SESSIONS.entrySet()) {
            UUID playerId = entry.getKey();
            WarpRideSession session = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            if (player == null) {
                // Belt and braces for a player who vanished without the disconnect event firing.
                endSession(server, playerId, WarpRidePayloads.END_REASON_ABORTED);
                continue;
            }

            WarpRidePhase before = session.getPhase();
            session.tick(now);
            RiderBodyGuard.pin(player, session.getAnchor());

            pushWaves(player, session);
            pushImpacts(player, session);

            int step = session.pollCountdownStep(now);
            if (step >= 0) {
                sendEvent(player, session, WarpRidePayloads.EVENT_COUNTDOWN, -1, step, 0, 0.0);
            }

            if (session.getPhase() == WarpRidePhase.PORTAL_TAKEN && before != WarpRidePhase.PORTAL_TAKEN) {
                // The ride is decided; no reason to keep the rider protected through the result screen.
                restoreBody(player, session);
                sendEvent(player, session, WarpRidePayloads.EVENT_PORTAL_TAKEN, -1,
                        session.getOutcomeRank(), session.getOutcomeTypeId(),
                        session.getOutcomeDistance());
                // Portal taken: fade the ride music out and keep everything else silent.
                maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onWarpRideMusicStop(player);
                CobbleSafari.LOGGER.info("[WarpRide] {} took a portal: distance={} type={} tier={}",
                        player.getName().getString(), session.getOutcomeDistance(),
                        session.getOutcomeTypeId(), session.getOutcomeRank());
            }

            if (session.isDrifting() && before == WarpRidePhase.RIDING
                    && session.getPhase() == WarpRidePhase.RIDING) {
                // Drift is edge-triggered client-side; the explicit event drives the message and cue.
                maybeAnnounceDrift(player, session);
            }

            if ((now % STATE_SYNC_INTERVAL) == 0) {
                sendState(player, session, now);
            }

            if (now - session.getLastActivityTick() > watchdogTicks) {
                CobbleSafari.LOGGER.warn("[WarpRide] Reaping session {} of {}: no activity for {}s",
                        session.getSessionId(), player.getName().getString(),
                        UltraWarpRideConfig.get().sessionWatchdogSeconds);
                endSession(server, playerId, WarpRidePayloads.END_REASON_ABORTED);
            }
        }
    }

    private static final Map<UUID, Boolean> DRIFT_ANNOUNCED = new ConcurrentHashMap<>();

    private static void maybeAnnounceDrift(ServerPlayer player, WarpRideSession session) {
        if (DRIFT_ANNOUNCED.putIfAbsent(session.getSessionId(), Boolean.TRUE) == null) {
            sendEvent(player, session, WarpRidePayloads.EVENT_DRIFT, -1, 0, 0, session.getOdometer());
        }
    }

    private static void pushWaves(ServerPlayer player, WarpRideSession session) {
        List<Wave> fresh = session.drainPendingWaves();
        if (fresh.isEmpty()) {
            return;
        }
        List<WarpRidePayloads.WaveData> data = new ArrayList<>(fresh.size());
        for (Wave wave : fresh) {
            List<WarpRidePayloads.ObjectData> objects = new ArrayList<>(wave.objects().size());
            for (WaveObject object : wave.objects()) {
                objects.add(new WarpRidePayloads.ObjectData(
                        object.kind().ordinal(), object.lane(), object.rank(), object.typeId()));
            }
            data.add(new WarpRidePayloads.WaveData(wave.index(), wave.trackPos(), objects));
        }
        Services.PLATFORM.sendPayloadToPlayer(player,
                new WarpRidePayloads.WavesPayload(session.getSessionId(), data));
    }

    private static void pushImpacts(ServerPlayer player, WarpRideSession session) {
        for (WarpRideSession.ImpactEvent impact : session.drainPendingImpacts()) {
            sendEvent(player, session,
                    impact.positive() ? WarpRidePayloads.EVENT_POSITIVE : WarpRidePayloads.EVENT_NEGATIVE,
                    impact.waveIndex(), impact.lane(), 0, impact.energyAfter());
        }
    }

    private static void sendState(ServerPlayer player, WarpRideSession session, long now) {
        Services.PLATFORM.sendPayloadToPlayer(player, new WarpRidePayloads.StatePayload(
                session.getSessionId(),
                session.getOdometer(),
                session.getEnergy(),
                session.getLane(),
                session.getSpeed(),
                session.cooldownTicksLeft(now),
                session.isDrifting(),
                session.isFrozen(now),
                session.getPhase().ordinal(),
                now));
    }

    public static void sendEvent(ServerPlayer player, WarpRideSession session, int event,
                                 int waveIndex, int a, int b, double value) {
        Services.PLATFORM.sendPayloadToPlayer(player, new WarpRidePayloads.EventPayload(
                session.getSessionId(), event, waveIndex, a, b, value));
    }


    public static void endSession(MinecraftServer server, UUID playerId, int reason) {
        WarpRideSession session = SESSIONS.remove(playerId);
        if (session == null) {
            return;
        }
        DRIFT_ANNOUNCED.remove(session.getSessionId());
        session.markEnded();

        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            restoreBody(player, session);
            Services.PLATFORM.sendPayloadToPlayer(player, new WarpRidePayloads.EventPayload(
                    session.getSessionId(), WarpRidePayloads.EVENT_RIDE_ENDED, -1,
                    reason, 0, session.getOdometer()));
            // Screen closed: release the music override so normal (dimension) music resumes.
            maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onWarpRideEnd(player);
        }
    }

    /** Restores the rider's captured flags. Idempotent: several exit paths call it. */
    private static void restoreBody(ServerPlayer player, WarpRideSession session) {
        if (session.isBodyRestored()) {
            return;
        }
        try {
            RiderBodyGuard.restore(player);
        } finally {
            session.markBodyRestored();
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        WarpRideSession session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        DRIFT_ANNOUNCED.remove(session.getSessionId());
        session.markEnded();
        restoreBody(player, session);
        if (server != null) {
            CobbleSafari.LOGGER.info("[WarpRide] {} disconnected mid-ride; session {} discarded",
                    player.getName().getString(), session.getSessionId());
        }
    }

    /** Sessions are in-memory only; a stopping server simply forgets them. */
    public static void onServerStopping() {
        SESSIONS.clear();
        DRIFT_ANNOUNCED.clear();
    }
}
