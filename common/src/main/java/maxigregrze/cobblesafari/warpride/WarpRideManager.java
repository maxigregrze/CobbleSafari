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

    /**
     * The parent run a ride is nested inside. The debug launcher that opened parentless rides is
     * gone (action plan 200, section 11); a missing parent is still handled, and hands off to nothing.
     */
    public record ParentContext(String parentScopeKey, java.util.UUID parentInstanceId,
                                net.minecraft.core.BlockPos devicePos,
                                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> parentDimension) {}

    private static final Map<UUID, ParentContext> PARENTS = new ConcurrentHashMap<>();

    /** Players whose ride ended on a portal and whose sub-dungeon is being generated right now. */
    private static final Map<UUID, ParentContext> HANDOFF = new ConcurrentHashMap<>();

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

    /**
     * The single entry point a caller uses to start a ride. Since action plan 200 (section 11) the
     * only caller is the wormhole device, so a ride always has a parent; a null one is still
     * tolerated and simply hands off to nothing.
     */
    public static OpenResult open(ServerPlayer player, ParentContext parent) {
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

        // Read before the body guard touches anything; the armour cannot change once the ride runs.
        boolean recon = maxigregrze.cobblesafari.item.ultraloot.ReconArmorLogic.isFullSet(player);
        boolean tutorial = !maxigregrze.cobblesafari.data.WarpRideTutorialSavedData.get(server)
                .hasSeen(player.getUUID());
        WarpRideSession session = new WarpRideSession(
                sessionId, player.getUUID(), seed, cfg, RiderBodyGuard.apply(player), now, recon, tutorial);
        SESSIONS.put(player.getUUID(), session);
        if (parent != null) {
            PARENTS.put(player.getUUID(), parent);
        } else {
            PARENTS.remove(player.getUUID());
        }

        Services.PLATFORM.sendPayloadToPlayer(player, new WarpRidePayloads.OpenPayload(
                sessionId,
                tutorial,
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
                cfg.fadeToBlackSeconds,
                cfg.steeringBase,
                cfg.steeringPerEnergy,
                cfg.introTicks,
                cfg.introLaunchTick));

        syncDestinations(player);
        // No music yet: it starts with the intro, never over the tutorial page (see onRideStarted).
        CobbleSafari.LOGGER.info("[WarpRide] Opened a ride for {} (session {}, recon={}, tutorial={})",
                player.getName().getString(), sessionId, recon, tutorial);
        return OpenResult.OK;
    }

    public static ParentContext parentOf(UUID playerId) {
        return PARENTS.get(playerId);
    }

    /** True while the server is generating this player's sub-dungeon and holding their screen. */
    public static boolean isHandingOff(UUID playerId) {
        return HANDOFF.containsKey(playerId);
    }

    /** Generation failed after a won ride: close the screen and let the caller message them. */
    public static void endHandoffFailure(MinecraftServer server, UUID playerId) {
        HANDOFF.remove(playerId);
        endSession(server, playerId, WarpRidePayloads.END_REASON_ABORTED);
    }

    /** Generation succeeded: the player is already in the sub-dungeon, so just close the screen. */
    public static void endHandoffSuccess(MinecraftServer server, UUID playerId) {
        HANDOFF.remove(playerId);
        endSession(server, playerId, WarpRidePayloads.END_REASON_HANDOFF);
    }

    /**
     * The tutorial was dismissed and the intro begins: only now does the mini-game's own music take
     * top priority, so the "press to start" page plays over whatever was already playing.
     */
    public static void onRideStarted(ServerPlayer player) {
        maxigregrze.cobblesafari.csmusic.DimensionalMusicManager.onWarpRideStart(player, resolveWarpMusicId());
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
        UltraWarpRideConfig cfg = UltraWarpRideConfig.get();

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
            if (pushParentClock(server, player, session)) {
                sendEvent(player, session, WarpRidePayloads.EVENT_PANIC, -1, 0, 0, 0.0);
            }
            if (tutorialOutOfTime(player, session)) {
                // The panic decay only covers the ride itself: a rider still reading the tutorial
                // with too little parent time left is sent back. The device stays spent, and the
                // tutorial is only marked seen on Ready, so it shows again next time (plan 201, D7).
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.cobblesafari.warpride.tutorial_time_out"));
                endSession(server, playerId, WarpRidePayloads.END_REASON_ABORTED);
                continue;
            }
            session.tick(now);
            RiderBodyGuard.pin(player, session.getAnchor());

            pushWaves(player, session);
            pushImpacts(player, session);

            if (session.pollLaunch(now)) {
                sendEvent(player, session, WarpRidePayloads.EVENT_LAUNCH, -1, 0, 0, 0.0);
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
                // Ultra Space ribbon: 1 % per portal tier (tier 0 never, tier 3 = 3 %).
                maxigregrze.cobblesafari.mark.RibbonAwards.rollRibbon(player,
                        maxigregrze.cobblesafari.mark.RibbonAwards.RIBBON_ULTRASPACE,
                        Math.max(0, session.getOutcomeRank()) * 0.01);
                if (session.getOutcomeDistance() > 0) {
                    int totalLy = maxigregrze.cobblesafari.init.ModStats.addAndGet(player,
                            maxigregrze.cobblesafari.init.ModStats.WARP_RIDE_DISTANCE, session.getOutcomeDistance());
                    maxigregrze.cobblesafari.advancement.ModCriteria.WARP_RIDE_DISTANCE.trigger(player, totalLy);
                }
                startHandoff(server, player, session);
            }

            if (session.isDrifting() && before == WarpRidePhase.RIDING
                    && session.getPhase() == WarpRidePhase.RIDING) {
                // Drift is edge-triggered client-side; the explicit event drives the message and cue.
                maybeAnnounceDrift(player, session);
            }

            if ((now % STATE_SYNC_INTERVAL) == 0) {
                sendState(player, session, now);
            }

            // Reading the tutorial sends nothing, and thirteen pages outlast the ride's own watchdog.
            int watchdogSeconds = session.getPhase() == WarpRidePhase.AWAITING_READY
                    ? cfg.tutorialWatchdogSeconds
                    : cfg.sessionWatchdogSeconds;
            if (now - session.getLastActivityTick() > watchdogSeconds * 20L) {
                CobbleSafari.LOGGER.warn("[WarpRide] Reaping session {} of {}: no activity for {}s",
                        session.getSessionId(), player.getName().getString(), watchdogSeconds);
                endSession(server, playerId, WarpRidePayloads.END_REASON_ABORTED);
            }
        }
    }

    /**
     * {@code true} while the tutorial is up and the parent session is at or below
     * {@code ultraWormholeMinParentSeconds}, the credit needed to open a device.
     */
    private static boolean tutorialOutOfTime(ServerPlayer player, WarpRideSession session) {
        if (session.getPhase() != WarpRidePhase.AWAITING_READY || !session.isTutorialShown()) {
            return false;
        }
        ParentContext parent = PARENTS.get(player.getUUID());
        if (parent == null) {
            return false;
        }
        maxigregrze.cobblesafari.data.PlayerTimerData data = maxigregrze.cobblesafari.manager.TimerManager.session(
                player, maxigregrze.cobblesafari.data.TimerScope.parse(parent.parentScopeKey()));
        int remaining = data == null ? 0 : data.getRemainingTicks();
        return remaining <= maxigregrze.cobblesafari.config.MiscConfig.getUltraWormholeMinParentSeconds() * 20;
    }

    /**
     * Reads the parent session's remaining credit and feeds it to the simulation as a plain int.
     *
     * @return true on the tick the panic decay first fires, so the caller sends the warning
     */
    private static boolean pushParentClock(MinecraftServer server, ServerPlayer player, WarpRideSession session) {
        ParentContext parent = PARENTS.get(player.getUUID());
        if (parent == null) {
            session.setParentRemainingTicks(WarpRideSession.NO_PARENT, 0, 0.0);
            return false;
        }
        maxigregrze.cobblesafari.data.TimerScope scope =
                maxigregrze.cobblesafari.data.TimerScope.parse(parent.parentScopeKey());
        maxigregrze.cobblesafari.data.PlayerTimerData data =
                maxigregrze.cobblesafari.manager.TimerManager.session(player, scope);
        int remaining = data == null ? 0 : data.getRemainingTicks();
        return session.setParentRemainingTicks(
                remaining,
                maxigregrze.cobblesafari.config.MiscConfig.getUltraWormholePanicTriggerSeconds() * 20,
                maxigregrze.cobblesafari.config.MiscConfig.getUltraWormholePanicDecayPerSecond());
    }

    /**
     * Starts the sub-dungeon generation in the same tick the portal was taken, and keeps the
     * screen open on black until it completes.
     *
     * <p>There is deliberately <b>no</b> "animation complete" packet from the client, and there
     * must never be one: a teleport gated on a client acknowledgement is a stall vector - a client
     * that never sends it would hold a generated sub-instance and its slot open indefinitely. The
     * server closes the screen when <em>generation</em> is done (W6).</p>
     */
    private static void startHandoff(MinecraftServer server, ServerPlayer player, WarpRideSession session) {
        ParentContext parent = PARENTS.remove(player.getUUID());
        if (parent == null) {
            return;                                    // parentless debug ride: nothing to hand to
        }
        HANDOFF.put(player.getUUID(), parent);
        // Tell the screen to hold on black. There is no reply, and there must never be one: a
        // teleport gated on a client acknowledgement would let a silent client pin a generated
        // sub-instance and its slot open indefinitely (W6).
        sendEvent(player, session, WarpRidePayloads.EVENT_HANDOFF, -1, 0, 0, 0.0);
        boolean started = maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager.beginGeneration(
                server, player, parent,
                session.getOutcomeTypeId(), session.getOutcomeRank(), session.getOutcomeDistance());
        if (!started) {
            HANDOFF.remove(player.getUUID());
            maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager.refundEntry(
                    server, parent.parentInstanceId(), player.getUUID(), parent.devicePos());
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "cobblesafari.wormhole.generation_failed"));
            endSession(server, player.getUUID(), WarpRidePayloads.END_REASON_ABORTED);
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
            // b = 1 marks a bonus that raised the energy ceiling (the source's "maxup" aura).
            sendEvent(player, session,
                    impact.positive() ? WarpRidePayloads.EVENT_POSITIVE : WarpRidePayloads.EVENT_NEGATIVE,
                    impact.waveIndex(), impact.lane(), impact.ceilingRaised() ? 1 : 0, impact.energyAfter());
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
        PARENTS.remove(playerId);
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
        PARENTS.remove(player.getUUID());
        HANDOFF.remove(player.getUUID());
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
        PARENTS.clear();
        HANDOFF.clear();
    }
}
