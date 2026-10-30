package maxigregrze.cobblesafari.dungeon.sub;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.DungeonInstanceRecord;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.data.TimerScope;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.DungeonInstanceCleanup;
import maxigregrze.cobblesafari.effect.RedShackledEffects;
import maxigregrze.cobblesafari.manager.TimerManager;
import maxigregrze.cobblesafari.security.RateLimiter;
import maxigregrze.cobblesafari.warpride.WarpRideManager;
import maxigregrze.cobblesafari.warpride.logic.WarpRideDestination;
import maxigregrze.cobblesafari.warpride.logic.WarpRideDestinationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The sub-dungeon's door and its single settlement path (action plan 183, B4 to B7).
 *
 * <p>Six ways out of a sub-instance - the exit portal, the timer, death, a disconnect, a restart,
 * and anything else at all - and exactly one {@link #settle}. That is the audit's Motif section 5
 * applied to departure: a list of triggers is correct only while it is exhaustive, and stops being
 * so at the first exit path someone adds later.</p>
 */
public final class SubDungeonManager {

    private SubDungeonManager() {}

    /**
     * The shared sub-dimension. Kept only as the default for a destination that names none; every
     * <em>test</em> for "am I in a sub-dungeon" now goes through
     * {@link DungeonDimensions#getDungeonByDimensionId} and {@code isSubDungeon()}, because five
     * dimensions answer yes since action plan 187 (§ 3.6).
     */
    public static final String SHARED_SUB_DIMENSION_ID = "cobblesafari:dungeon_ultraspace";

    /** @return true when {@code dimensionId} is one of the registered sub-dungeon dimensions. */
    public static boolean isSubDungeonDimension(String dimensionId) {
        DungeonConfig config = DungeonDimensions.getDungeonByDimensionId(dimensionId);
        return config != null && config.isSubDungeon();
    }

    /**
     * Rate-limit action id for the device. Under {@code SERVICE_WARPRIDE}, 0 is the ride's input and
     * 1 its ready: this used to be 1 as well, so a device click stamped the ready's slot and a ready
     * sent within 500 ms of it was silently dropped - the ride then sat on its tutorial page for good
     * (action plan 190, P4).
     */
    private static final int ACTION_DEVICE = 2;

    /**
     * Ticks already debited from the parent for the visit in progress, per player. In memory only,
     * and used solely for the "time spent" message - the debit itself is on the parent's own record
     * and is persisted there.
     */
    private static final Map<UUID, Integer> SPENT = new ConcurrentHashMap<>();

    // --- The door (section 8.1) ------------------------------------------------------------------

    /**
     * The eight checks of W5, cheapest first, with <b>nothing consumed</b> until all of them pass.
     *
     * <p>S13's distance and block-entity guards have no object here: there is no confirm payload.
     * The device opens on a vanilla interaction, so reach is enforced by the server, the position
     * never comes from the client, and the block is read from the world. Only the rate limit
     * survives, against a held right-click.</p>
     */
    public static void openDevice(ServerPlayer player, BlockPos devicePos) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID playerId = player.getUUID();

        // 1 - rate limit, silent.
        if (!RateLimiter.allow(playerId, RateLimiter.key(RateLimiter.SERVICE_WARPRIDE, ACTION_DEVICE),
                MiscConfig.getUltraWormholeDeviceRateLimitMs())) {
            return;
        }
        // 2 - one ride per player (W8).
        if (WarpRideManager.isRiding(playerId) || SubDungeonGeneration.isGenerating(playerId)) {
            refuse(player, "cobblesafari.wormhole.refused.already_riding");
            return;
        }
        // 3 - standing in a live instance, resolved by footprint (plan 179, V1).
        TimerScope parentScope = TimerManager.currentScope(player);
        if (parentScope == null || !parentScope.isSession() || !TimerManager.isScopeLive(parentScope)) {
            refuse(player, "cobblesafari.wormhole.refused.no_instance");
            return;
        }
        // 4 - depth <= 1, refused in code and not by convention (S16).
        DungeonConfig here = DungeonDimensions.getDungeonByDimension(player.level().dimension());
        if (here != null && here.isSubDungeon()) {
            refuse(player, "cobblesafari.wormhole.refused.nested");
            return;
        }
        UUID parentInstanceId;
        try {
            parentInstanceId = UUID.fromString(parentScope.scopeId());
        } catch (IllegalArgumentException e) {
            refuse(player, "cobblesafari.wormhole.refused.no_instance");
            return;
        }
        DungeonPositionSavedData positions = DungeonPositionSavedData.get(server);
        DungeonInstanceRecord parentRecord = positions.getInstance(parentInstanceId);
        if (parentRecord == null || parentRecord.pendingDeletion()) {
            refuse(player, "cobblesafari.wormhole.refused.no_instance");
            return;
        }
        // 5 - once per device (plan 187, C4). A megalopolis carrying three wormholes therefore
        // grants three rides, and the count follows the structure rather than a config key.
        if (parentRecord.hasSpentSubEntry(playerId, devicePos)) {
            refuse(player, "cobblesafari.wormhole.refused.already_used");
            return;
        }
        // 6 - opening floor on parent credit.
        PlayerTimerData parent = TimerManager.session(player, parentScope);
        int parentRemaining = parent == null ? 0 : parent.getRemainingTicks();
        int floorTicks = MiscConfig.getUltraWormholeMinParentSeconds() * 20;
        if (parentRemaining < floorTicks) {
            int missing = Math.max(1, (floorTicks - parentRemaining) / 20);
            player.displayClientMessage(
                    Component.translatable("cobblesafari.wormhole.refused.no_credit", missing), true);
            return;
        }
        // 7 - not mid-battle (S18): a dimension change during one is a classic stuck battle.
        if (isInBattle(player)) {
            refuse(player, "cobblesafari.wormhole.refused.in_battle");
            return;
        }
        // 8 - not shackled, matching dungeon entry.
        if (RedShackledEffects.isShackled(player)) {
            refuse(player, "cobblesafari.wormhole.refused.in_battle");
            return;
        }
        if (WarpRideDestinationRegistry.isEmpty()) {
            refuse(player, "cobblesafari.wormhole.generation_failed");
            return;
        }

        // S14 - consume and persist BEFORE anything else, so two clicks in one tick cannot both
        // read "unused". The ride itself is what the entry buys; a failed ride does not refund it.
        positions.updateInstance(parentRecord.withSubEntryConsumed(playerId, devicePos));

        WarpRideManager.ParentContext context = new WarpRideManager.ParentContext(
                parentScope.key(), parentInstanceId, devicePos, player.level().dimension());
        WarpRideManager.OpenResult result = WarpRideManager.open(player, context);
        if (result != WarpRideManager.OpenResult.OK) {
            // Server-side failure before the ride even started: give the entry back (section 8.4).
            refundEntry(server, parentInstanceId, playerId, devicePos);
            refuse(player, "cobblesafari.wormhole.generation_failed");
        }
    }

    private static boolean isInBattle(ServerPlayer player) {
        try {
            return com.cobblemon.mod.common.Cobblemon.INSTANCE.getBattleRegistry()
                    .getBattleByParticipatingPlayer(player) != null;
        } catch (Throwable t) {
            // A Cobblemon API shift must not make the device unusable; the guard degrades to open.
            CobbleSafari.LOGGER.debug("[SubDungeon] battle check unavailable", t);
            return false;
        }
    }

    private static void refuse(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    /**
     * Gives one device's entry back. Used only by the two server-initiated failures, and scoped to
     * the device it was spent on: the player's other wormholes stay available (plan 187, C4).
     */
    public static void refundEntry(MinecraftServer server, UUID parentInstanceId, UUID playerId,
                                   BlockPos devicePos) {
        if (parentInstanceId == null) {
            return;
        }
        DungeonPositionSavedData positions = DungeonPositionSavedData.get(server);
        DungeonInstanceRecord record = positions.getInstance(parentInstanceId);
        if (record != null) {
            positions.updateInstance(record.withSubEntryRefunded(playerId, devicePos));
        }
    }

    /**
     * Clears every wormhole entry a player has spent on one instance, whatever the device. Used
     * when they pay to re-enter it: a new run buys back all of its wormholes, not just the last
     * one used (plan 187, C4).
     */
    public static void clearEntriesForNewRun(MinecraftServer server, UUID instanceId, UUID playerId) {
        if (instanceId == null) {
            return;
        }
        DungeonPositionSavedData positions = DungeonPositionSavedData.get(server);
        DungeonInstanceRecord record = positions.getInstance(instanceId);
        if (record != null) {
            positions.updateInstance(record.withSubEntriesClearedFor(playerId));
        }
    }

    // --- The handoff (section 8.3) ---------------------------------------------------------------

    /**
     * Called on {@code PORTAL_TAKEN}, in the same tick the simulation freezes. The three values
     * come from the server's own odometer and roll; none has ever been in a C2S payload.
     */
    public static boolean beginGeneration(MinecraftServer server, ServerPlayer player,
                                          WarpRideManager.ParentContext parent,
                                          int typeId, int tier, int distance) {
        WarpRideDestination destination = WarpRideDestinationRegistry.byTypeId(typeId);
        if (destination == null) {
            CobbleSafari.LOGGER.error("[SubDungeon] no destination for typeId={}", typeId);
            return false;
        }
        SubDungeonRequest request = new SubDungeonRequest(
                player.getUUID(),
                parent == null ? null : TimerScope.parse(parent.parentScopeKey()),
                parent == null ? null : parent.parentInstanceId(),
                parent == null ? player.blockPosition() : parent.devicePos(),
                parent == null ? player.level().dimension() : parent.parentDimension(),
                destination.structurePool(),
                destination.dungeonId(),
                destination.effectiveBiome(),
                destination.typeId(),
                tier,
                distance);
        return SubDungeonGeneration.begin(server, request);
    }

    /** The abort path of section 8.4: the entry goes back, and the rider stays in the parent. */
    static void onGenerationFailed(MinecraftServer server, UUID playerId, SubDungeonRequest request) {
        refundEntry(server, request.parentInstanceId(), playerId, request.devicePos());
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("cobblesafari.wormhole.generation_failed"));
            WarpRideManager.endHandoffFailure(server, playerId);
        }
    }

    // --- Arrival (section 5.4 + 6.1) -------------------------------------------------------------

    /** Creates the record, opens the sub-session naming its parent, and teleports. */
    static DungeonInstanceRecord registerAndEnter(MinecraftServer server, ServerPlayer player,
                                                  SubDungeonRequest request, DungeonConfig config,
                                                  int slot, BlockPos anchor,
                                                  int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ,
                                                  int clearSectionsBelow, int clearSectionsAbove,
                                                  BlockPos exitPortal, Direction exitFacing) {
        // Aimed at the destination's own dimension rather than a constant: each of the five
        // sub-dimensions therefore carries its own time budget, for free (plan 187, § 3.6).
        int subTimerTicks = SafariTimerConfig.getDimensionConfig(config.getDimensionId())
                .map(entry -> entry.getTimerDurationSeconds() * 20)
                .orElse(config.getTimerDurationTicks());

        int granted = subTimerTicks;
        if (request.hasParent()) {
            PlayerTimerData parent = TimerManager.session(player, request.parentScope());
            granted = Math.min(subTimerTicks, parent == null ? 0 : parent.getRemainingTicks());
        }
        DungeonInstanceRecord record = DungeonInstanceRecord.createSub(
                config.getDimensionId(), slot, anchor,
                chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ,
                clearSectionsBelow, clearSectionsAbove, config.getZoneSize(),
                request.parentInstanceId(), player.getUUID(), granted,
                request.typeId(), request.tier(), request.distance());
        DungeonPositionSavedData.get(server).putInstance(record);

        // Registered BEFORE any bail-out below. The structure is already in the world at this
        // point, so a failure that only freed the slot would leave the region standing forever:
        // the record is what the clear pipeline needs to erase it.
        int floorTicks = MiscConfig.getUltraWormholeReserveSeconds() * 20;
        ServerLevel subLevel = server.getLevel(config.getDimensionKey());
        if (granted < floorTicks || subLevel == null) {
            CobbleSafari.LOGGER.warn("[SubDungeon] aborting entry for {}: granted={} floor={} level={}",
                    player.getName().getString(), granted, floorTicks, subLevel);
            DungeonInstanceCleanup.forceReleaseInstance(server, record.instanceId());
            return null;
        }
        BlockPos spawn = exitPortal == null
                ? anchor.above()
                : SubDungeonGeneration.spawnBeside(exitPortal, exitFacing);
        float yaw = SubDungeonGeneration.yawFor(exitFacing);

        TimerScope subScope = TimerScope.of(config.getDimensionId(), record.instanceId());
        // The origin is the wormhole block in the parent dimension: the return path costs no new
        // state, because teleportOnTimerExpired already sends a player to their session's origin.
        TimerManager.openSession(player, subScope, granted,
                request.devicePos(), request.parentDimension(), request.parentScope());

        player.teleportTo(subLevel, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, yaw, 0.0f);
        player.resetFallDistance();
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        TimerManager.startSession(player, subScope);
        WarpRideDestination destination = WarpRideDestinationRegistry.byTypeId(request.typeId());
        if (destination != null) {
            maxigregrze.cobblesafari.advancement.ModCriteria.WARP_DESTINATION_REACHED.trigger(
                    player, destination.destinationId());
        }

        CobbleSafari.LOGGER.info("[SubDungeon] {} entered {} (instance {}, type={} tier={} distance={}, {} ticks)",
                player.getName().getString(), config.getDimensionId(), record.instanceId(),
                request.typeId(), request.tier(), request.distance(), granted);
        return record;
    }

    // --- Departure: the one settlement path (section 6.2 + 7) -----------------------------------

    /**
     * Settles a sub-session: debit the parent by the time actually spent, then close.
     *
     * <p>{@code elapsed = granted - remaining}, not {@code now - entryTick}. A wall clock is wrong
     * the moment the session pauses - which it does on every disconnect and every server stop - and
     * would bill a player for three minutes they spent offline. The remaining credit <em>is</em>
     * the unspent time by construction, so the difference is exact, immune to pauses, and survives
     * a restart with no extra code. Clamped at both ends: a {@code remaining} above {@code granted}
     * (a level-2 command) must not <em>credit</em> the parent.</p>
     *
     * <p>Idempotent: a session already closed settles to a no-op.</p>
     */
    public static int settle(MinecraftServer server, ServerPlayer player, TimerScope subScope) {
        PlayerTimerData sub = TimerManager.session(player, subScope);
        if (sub == null) {
            return 0;
        }
        String parentKey = sub.getParentScopeId();
        Integer spent = SPENT.remove(player.getUUID());
        int elapsed = spent == null ? 0 : spent;
        if (parentKey != null) {
            TimerScope parentScope = TimerScope.parse(parentKey);
            PlayerTimerData parent = TimerManager.session(player, parentScope);
            if (parent != null) {
                // The running debit is already on the record; this is the flush that guarantees the
                // last few ticks reached disk.
                TimerManager.savePlayerData(player, parent);
                CobbleSafari.LOGGER.info("[SubDungeon] {} spent {} ticks of {} on this trip",
                        player.getName().getString(), elapsed, parentScope.key());
            }
        }
        return elapsed;
    }

    /**
     * Spends one tick of the parent's credit for one tick spent in the sub-dungeon. Called from
     * {@code TimerManager.tickAllTimers} for any session that names a parent.
     *
     * <p><b>Why the debit is incremental and not computed on the way out.</b> The plan settled a
     * closing subtraction, {@code granted - remaining}, because a wall clock over-bills a session
     * that pauses. It does - but so does the subtraction, in a way the plan did not foresee: every
     * eviction path (the void guard, the death rescue, an expiry) <em>zeroes</em> the credit before
     * anything settles, so {@code remaining} reads 0 and the parent is billed the <em>whole</em>
     * grant. Observed in testing: a player who died six seconds into a 198-second trip had 198
     * seconds taken off their parent run, which promptly expired and threw them to the overworld.
     *
     * <p>Charging as the time is actually used removes the arithmetic altogether. There is no exit
     * path to get right, nothing to clamp, and the parent's credit is already correct at the instant
     * the player lands back in it - which is also what stops the HUD showing a stale value for a
     * second after the return.</p>
     */
    public static void debitParentForTick(ServerPlayer player, PlayerTimerData sub) {
        String parentKey = sub.getParentScopeId();
        if (parentKey == null) {
            return;
        }
        PlayerTimerData parent = TimerManager.session(player, TimerScope.parse(parentKey));
        if (parent == null) {
            return;
        }
        int remaining = parent.getRemainingTicks();
        if (remaining > 0) {
            parent.setRemainingTicks(remaining - 1);
        }
        int spent = SPENT.merge(player.getUUID(), 1, Integer::sum);
        // The record is mutated in place every tick; persisting once a second is enough to bound
        // what a crash could lose to the same second the timer itself would lose.
        if ((spent % 20) == 0) {
            TimerManager.savePlayerData(player, parent);
        }
    }

    /** Drops the running counter for a player who left without settling (disconnect, shutdown). */
    public static void forgetSpent(UUID playerId) {
        SPENT.remove(playerId);
    }

    /**
     * Voluntary return through the exit portal (section 7.1). The order is load-bearing:
     * settle while still inside (the scope only resolves from in there - plan 182, C2), teleport,
     * re-arm the parent explicitly rather than waiting a second for the presence sweep, and only
     * then release - because the clear purges the session the teleport needed (plan 181, V5).
     */
    public static void returnThroughExit(ServerPlayer player) {
        // Queued, never performed here (action plan 190, P5). The portal calls this from
        // entityInside, which for a player runs INSIDE ServerGamePacketListenerImpl.handleMovePlayer:
        // a dimension change there makes vanilla skip its "moved wrongly" check (isChangingDimension)
        // and finish with absMoveTo(the client's position) - the sub-dungeon's coordinates, applied
        // in the parent dimension. Back from the desert, that left the player at Y 14 in the
        // Megalopolis, under its void guard, and the guard threw them out with time left on both runs.
        PENDING_EXITS.add(player.getUUID());
    }

    /** Players who walked into an exit portal; their return runs on the next server tick. */
    private static final Set<UUID> PENDING_EXITS = ConcurrentHashMap.newKeySet();

    /**
     * Performs the queued exit-portal returns. Runs at the head of the server tick, outside any
     * packet handler, so the teleport is the last word on the player's position.
     */
    public static void runPendingExits(MinecraftServer server) {
        if (PENDING_EXITS.isEmpty()) {
            return;
        }
        for (UUID playerId : List.copyOf(PENDING_EXITS)) {
            PENDING_EXITS.remove(playerId);
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                continue;
            }
            TimerScope subScope = TimerManager.currentScope(player);
            if (subScope == null || !subScope.isSession()) {
                continue;                                    // a decorative portal: nothing to do
            }
            PlayerTimerData sub = TimerManager.session(player, subScope);
            if (sub == null || !isSubDungeonDimension(subScope.dimensionId())) {
                continue;
            }
            leave(server, player, subScope, true);
        }
    }

    /** Discards encounter Pokemon whose instance is gone; see SubDungeonEncounter (plan 190, P6). */
    public static void sweepOrphanedEncounters(MinecraftServer server) {
        SubDungeonEncounter.sweepOrphanedEncounters(server);
    }

    public static void onServerStoppingExits() {
        PENDING_EXITS.clear();
    }

    /**
     * The whole departure, shared by every exit. {@code notify} is false for the silent paths
     * (a sweep catching a {@code /tp}) and true when the player asked to leave.
     */
    public static void leave(MinecraftServer server, ServerPlayer player, TimerScope subScope, boolean notify) {
        PlayerTimerData sub = TimerManager.session(player, subScope);
        if (sub == null) {
            return;
        }
        UUID subInstanceId;
        try {
            subInstanceId = UUID.fromString(subScope.scopeId());
        } catch (IllegalArgumentException e) {
            return;
        }
        int elapsed = settle(server, player, subScope);
        // Close the sub-session here rather than through expireActiveTimerAndTeleport, which now
        // routes nested visits back to this method and would otherwise recurse.
        sub.setRemainingTicks(0);
        sub.setActive(false);
        TimerManager.savePlayerData(player, sub);
        TimerManager.syncToClient(player, sub);

        String parentKey = sub.getParentScopeId();
        TimerScope parentScope = parentKey == null ? null : TimerScope.parse(parentKey);
        boolean parentAlive = parentScope != null && TimerManager.isScopeLive(parentScope);
        PlayerTimerData parent = parentScope == null ? null : TimerManager.session(player, parentScope);

        // Three outcomes, and only the first goes back into the parent dungeon.
        boolean orphaned = parentScope != null && !parentAlive;
        // The running debit can empty the parent run while its owner is away. Sending them into it
        // anyway would put them there for a single tick before the ordinary expiry threw them out
        // again - a dimension they never really see, and a "Time's up" that reads as a bug. They go
        // straight to where that expiry would have sent them, with a message that says why.
        boolean parentSpent = !orphaned && parentAlive
                && (parent == null || parent.getRemainingTicks() <= 0);

        BlockPos target = sub.getOriginPos();
        ResourceKey<Level> targetDimension = sub.getOriginDimension();

        if (orphaned || parentSpent) {
            // Both fall back to the parent session's own origin - the player's overworld entry
            // point for that run. For an orphan that is the only safe destination left; for a spent
            // run it is exactly where the parent's expiry would have delivered them.
            if (parent != null && parent.getOriginPos() != null && parent.getOriginDimension() != null) {
                target = parent.getOriginPos();
                targetDimension = parent.getOriginDimension();
            } else {
                target = null;
            }
        }

        if (target != null && targetDimension != null) {
            ServerLevel level = server.getLevel(targetDimension);
            // A return that lands below the destination's own guard is evacuated on arrival, and
            // the player is thrown out of a run they still had credit for. That reads as a timer
            // bug and is not one - it is a voidGuardY set above the geometry the player returns to.
            // Say so, loudly, naming both numbers: the eviction itself is unavoidable from here.
            DungeonConfig targetConfig =
                    DungeonDimensions.getDungeonByDimensionId(targetDimension.location().toString());
            if (targetConfig != null && target.getY() < targetConfig.getVoidGuardY()) {
                CobbleSafari.LOGGER.error("[SubDungeon] return point {} for {} is BELOW that dimension's "
                        + "voidGuardY of {}; the player will be evacuated on arrival. Lower the guard for {} "
                        + "or move the device above it",
                        target, targetDimension.location(), targetConfig.getVoidGuardY(),
                        targetConfig.getId());
            }
            if (level != null) {
                // Checked (plan 190, J3): a discarded player is disconnected by relocate rather than
                // left in the sub-dimension believing they went home.
                TimerManager.relocate(player, level, target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                        "sub-dungeon departure from " + subScope.key());
            } else {
                TimerManager.teleportToOverworldSpawn(player);
            }
        } else {
            TimerManager.teleportToOverworldSpawn(player);
        }

        // Re-armed only when there is a run left to go back to. A spent one stays paused at zero,
        // exactly as a normal expiry leaves it, so an operator can still credit it and let the
        // player walk back in through the portal (plan 181, section 9.1).
        if (parentAlive && !parentSpent && parent != null) {
            TimerManager.startSession(player, parentScope);
        }

        if (notify) {
            if (orphaned) {
                player.sendSystemMessage(
                        Component.translatable("cobblesafari.wormhole.returned.parent_gone"));
            } else if (parentSpent) {
                player.sendSystemMessage(
                        Component.translatable("cobblesafari.wormhole.returned.parent_spent"));
            } else {
                player.displayClientMessage(Component.translatable(
                        "cobblesafari.wormhole.returned", formatSeconds(elapsed)), true);
            }
        }

        DungeonInstanceCleanup.maybeReleaseInstance(server, subInstanceId);
    }

    private static String formatSeconds(int ticks) {
        int seconds = ticks / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    // --- Guarantees (section 7.3, 7.4) -----------------------------------------------------------

    /**
     * S15, the debit that cannot be evaded: settlement hangs off <b>having left the sub-dimension</b>,
     * never off a portal. A {@code /tp}, an addon teleporter, a waystone - any of them would
     * otherwise keep the parent credit and make the Ultra Space time free.
     */
    public static void sweepSettlements(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String dimension = player.level().dimension().location().toString();
            for (PlayerTimerData data : TimerManager.allLiveSessions(player)) {
                if (data.getParentScopeId() == null) {
                    continue;
                }
                if (dimension.equals(data.getDimensionId())) {
                    continue;                                 // still inside: nothing to settle
                }
                CobbleSafari.LOGGER.info("[SubDungeon] settling {} for {}: left {} without the exit portal",
                        data.scope().key(), player.getName().getString(), data.getDimensionId());
                settle(server, player, data.scope());
                try {
                    DungeonInstanceCleanup.maybeReleaseInstance(
                            server, UUID.fromString(data.scope().scopeId()));
                } catch (IllegalArgumentException ignored) {
                    // Non-UUID scope: the 100-tick detached sweep still covers it.
                }
            }
        }
    }

    /**
     * Boot sweep (section 7.4): nobody can be inside a sub-instance across a restart - sessions
     * persist but players do not - so every one of them is marked pending. The login path of
     * plan 182 A3 brings the returning player back to their recorded origin.
     */
    public static void sweepSubInstancesOnBoot(MinecraftServer server) {
        int marked = 0;
        for (DungeonInstanceRecord record : DungeonPositionSavedData.get(server).getInstancesSnapshot()) {
            if (record.isSubInstance() && !record.pendingDeletion()) {
                DungeonInstanceCleanup.forceReleaseInstance(server, record.instanceId());
                marked++;
            }
        }
        if (marked > 0) {
            CobbleSafari.LOGGER.info("[SubDungeon] boot sweep marked {} sub-instance(s) for deletion", marked);
        }
    }
}
