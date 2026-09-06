package maxigregrze.cobblesafari.manager;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.DimensionTimerEntry;
import maxigregrze.cobblesafari.config.SafariConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.DungeonInstanceRecord;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.data.TimerScope;
import maxigregrze.cobblesafari.data.TimerSavedData;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.DungeonTeleportHandler;
import maxigregrze.cobblesafari.network.ModNetworking;
import maxigregrze.cobblesafari.unionroom.UnionRoomManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TimerManager {

    private TimerManager() {
        // Utility class; not meant to be instantiated.
    }

    /** Keyed by {@link TimerScope#key()}, never by a bare dimension id on an instanced dimension. */
    private static final Map<UUID, Map<String, PlayerTimerData>> activeTimers = new ConcurrentHashMap<>();

    /**
     * Sticky scope net (mirrors plan 179 V4, and load-bearing here): the last scope key each online
     * player resolved to. In-memory, cleared on disconnect. Losing an objectives key costs progress;
     * losing a timer scope would evacuate the player, so the guard leans on this before acting.
     */
    private static final Map<UUID, String> LAST_SCOPE_KEY = new ConcurrentHashMap<>();

    private static MinecraftServer serverInstance;

    public static void setServer(MinecraftServer server) {
        serverInstance = server;
    }

    public static void clearServer() {
        serverInstance = null;
        activeTimers.clear();
        LAST_SCOPE_KEY.clear();
    }

    // --- Bypass (hoisted to player level, plan 181 § 3.3) ---------------------------------------

    /**
     * The operator escape hatch. A bypassed player is exempt from the timer <em>and</em> from the
     * presence invariant (§ 5.2): they may stand in any timed dimension with no session, no credit
     * and no fee - which is what makes it possible to inspect a badly cleaned instance, watch over a
     * player inside a dungeon, or stay in the Safari indefinitely.
     * <p>
     * It is <b>not</b> an exemption from a region clear: {@code expireSessionAndTeleport} evacuates
     * bypassed players too, because those blocks are about to stop existing.
     */
    public static boolean shouldBypassTimer(ServerPlayer player) {
        if (serverInstance == null) return false;
        return TimerSavedData.get(serverInstance).isBypassed(player.getUUID());
    }

    public static void toggleBypass(ServerPlayer player, Boolean forceState) {
        if (serverInstance == null) return;
        TimerSavedData savedData = TimerSavedData.get(serverInstance);

        boolean newState = forceState != null ? forceState : !savedData.isBypassed(player.getUUID());
        savedData.setBypassed(player.getUUID(), newState);

        PlayerTimerData current = currentTimerData(player);
        if (current != null) {
            syncToClient(player, current, newState);
        }

        CobbleSafari.LOGGER.info("Timer bypass for player {} set to {}",
                player.getName().getString(), newState);
    }

    // --- Scope resolution (plan 181 § 3.5, "by position") ----------------------------------------

    /**
     * The scope the player currently stands in, or {@code null} if they are not in a timed dimension.
     * <p>
     * For an instanced dimension this uses the <em>full</em> three-pass resolver, because it answers
     * "which visit is this?" - a question where a generous match is better than none. The presence
     * guard deliberately does <b>not</b> use it to decide an eviction; see
     * {@link #sweepPresenceInvariant()}.
     */
    public static TimerScope currentScope(ServerPlayer player) {
        Optional<String> dimOpt = getConfiguredDimensionId(player);
        if (dimOpt.isEmpty()) {
            return null;
        }
        String dimensionId = dimOpt.get();
        if (!isInstancedDimension(dimensionId)) {
            return TimerScope.of(dimensionId);
        }
        if (serverInstance == null) {
            return null;
        }
        DungeonInstanceRecord record = DungeonPositionSavedData.get(serverInstance)
                .findInstanceByPosition(dimensionId, player.blockPosition());
        if (record != null) {
            return TimerScope.of(dimensionId, record.instanceId());
        }
        return stickyScope(player, dimensionId);
    }

    /**
     * The sticky net: keep the last resolved scope while the player has not changed dimension, its
     * instance is still live, they still hold its session, <b>and they are still plausibly near it</b>.
     */
    private static TimerScope stickyScope(ServerPlayer player, String dimensionId) {
        String lastKey = LAST_SCOPE_KEY.get(player.getUUID());
        if (lastKey == null) {
            return null;
        }
        TimerScope scope = TimerScope.parse(lastKey);
        if (!scope.isSession() || !scope.dimensionId().equals(dimensionId)) {
            return null;
        }
        if (!isScopeLive(scope) || session(player, scope) == null) {
            return null;
        }
        return isNearAnchor(player, scope) ? scope : null;
    }

    /**
     * Locality bound on the sticky net (action plan 182, correctif C1).
     * <p>
     * Without it the net has no spatial limit at all: a player who visited an instance keeps
     * resolving to it from <em>anywhere</em> in that dimension, so a teleport 400 000 blocks away
     * still reads as "inside". Under plan 179 that only mattered to a HUD; since 181 it decides
     * eviction, and since 182 it decides whether a region may be erased - so an unbounded net
     * pins instances forever and, worse, means {@code currentScope} never returns {@code null},
     * which quietly disables the whole clause (c) failsafe of section 5.2.
     * <p>
     * The window is {@code 2 x zoneSize} from the anchor, deliberately generous: pass 3 of the
     * resolver already covers {@code zoneSize} - the documented maximum reach of a jigsaw - so
     * anything the net still has to catch is a resolution hole near the structure, not a journey.
     */
    private static boolean isNearAnchor(ServerPlayer player, TimerScope scope) {
        if (serverInstance == null) {
            return false;
        }
        DungeonInstanceRecord record;
        try {
            record = DungeonPositionSavedData.get(serverInstance)
                    .getInstance(UUID.fromString(scope.scopeId()));
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (record == null) {
            return false;
        }
        int zone = record.zoneSize() > 0 ? record.zoneSize() : DungeonTeleportHandler.ZONE_SIZE;
        BlockPos pos = player.blockPosition();
        long dx = Math.abs((long) pos.getX() - record.structurePos().getX());
        long dz = Math.abs((long) pos.getZ() - record.structurePos().getZ());
        return Math.max(dx, dz) <= 2L * zone;
    }

    /** Whether a session scope still names a known, non-pending instance. */
    public static boolean isScopeLive(TimerScope scope) {
        if (serverInstance == null || !scope.isSession()) {
            return false;
        }
        try {
            return DungeonPositionSavedData.get(serverInstance)
                    .isInstanceLive(UUID.fromString(scope.scopeId()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isInstancedDimension(String dimensionId) {
        return SafariTimerConfig.getDimensionConfig(dimensionId)
                .map(DimensionTimerEntry::isInstanced)
                .orElseGet(() -> DungeonDimensions.isInstancedDungeonDimension(dimensionId));
    }

    // --- Sessions (plan 181, V2) -----------------------------------------------------------------

    /**
     * The <b>only</b> way a session comes into being. Called from
     * {@code DungeonTeleportHandler.executeDungeonTeleport} <em>after</em> {@code changeDimension}
     * returns, so that "a session exists" is strictly equivalent to "this player got in" - the
     * equivalence the whole presence guard rests on.
     */
    public static PlayerTimerData openSession(ServerPlayer player, TimerScope scope, int ticks,
                                              BlockPos originPos, ResourceKey<Level> originDimension) {
        return openSession(player, scope, ticks, originPos, originDimension, null);
    }

    /**
     * @param parentScope the visit this one is nested inside, or {@code null}. Always {@code null}
     *        today; it is the seam Feature B fills so the release sweep can tell a player who
     *        stepped into a sub-dungeon from one who left (action plan 182 section 5.2).
     */
    public static PlayerTimerData openSession(ServerPlayer player, TimerScope scope, int ticks,
                                              BlockPos originPos, ResourceKey<Level> originDimension,
                                              TimerScope parentScope) {
        if (serverInstance == null || !scope.isSession()) {
            return null;
        }
        UUID playerId = player.getUUID();
        TimerSavedData savedData = TimerSavedData.get(serverInstance);
        PlayerTimerData data = savedData.getOrCreateSession(playerId, scope);
        data.setRemainingTicks(ticks);
        data.setLastResetTimestamp(System.currentTimeMillis());
        if (originPos != null) {
            data.setOriginPos(originPos);
        }
        if (originDimension != null) {
            data.setOriginDimension(originDimension);
        }
        if (parentScope != null) {
            data.setParentScopeId(parentScope.key());
        }
        savedData.setPlayerData(playerId, scope, data);
        activeTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(scope.key(), data);
        CobbleSafari.LOGGER.info("Opened timer session {} for player {} with {} ticks",
                scope.key(), player.getName().getString(), ticks);
        return data;
    }

    /** Reads a session. Returns {@code null} when absent - and never creates one. */
    public static PlayerTimerData session(ServerPlayer player, TimerScope scope) {
        if (scope == null) {
            return null;
        }
        UUID playerId = player.getUUID();
        Map<String, PlayerTimerData> playerTimers = activeTimers.get(playerId);
        if (playerTimers != null) {
            PlayerTimerData cached = playerTimers.get(scope.key());
            if (cached != null) {
                return cached;
            }
        }
        if (serverInstance == null) {
            return null;
        }
        PlayerTimerData stored = TimerSavedData.get(serverInstance).getPlayerData(playerId, scope);
        if (stored != null) {
            activeTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(scope.key(), stored);
        }
        return stored;
    }

    public static PlayerTimerData currentSession(ServerPlayer player) {
        return session(player, currentScope(player));
    }

    /** The record governing the player right now, whether it is an account or a session. */
    public static PlayerTimerData currentTimerData(ServerPlayer player) {
        TimerScope scope = currentScope(player);
        if (scope == null) {
            return null;
        }
        return scope.isSession() ? session(player, scope) : getOrCreateAccount(player, scope.dimensionId());
    }

    /** Every live session the player holds in {@code dimensionId}, newest lookup order irrelevant. */
    public static List<PlayerTimerData> liveSessions(ServerPlayer player, String dimensionId) {
        if (serverInstance == null) {
            return List.of();
        }
        return TimerSavedData.get(serverInstance).getPlayerScopedTimers(player.getUUID()).values().stream()
                .filter(PlayerTimerData::isSession)
                .filter(d -> d.getDimensionId().equals(dimensionId))
                .filter(d -> isScopeLive(d.scope()))
                .toList();
    }

    /** Every live session the player holds, in any dimension. */
    public static List<PlayerTimerData> allLiveSessions(ServerPlayer player) {
        if (serverInstance == null) {
            return List.of();
        }
        return TimerSavedData.get(serverInstance).getPlayerScopedTimers(player.getUUID()).values().stream()
                .filter(PlayerTimerData::isSession)
                .filter(d -> isScopeLive(d.scope()))
                .toList();
    }

    /**
     * Drops every session of one instance, for all players (plan 181, V5). Called from
     * {@code DungeonInstanceCleanup.scheduleRegionClear} <em>after</em> the evacuation, never
     * before: the evacuation needs the session to send the player back to their own origin.
     */
    public static void purgeInstance(MinecraftServer server, String dimensionId, UUID instanceId) {
        String key = TimerScope.of(dimensionId, instanceId).key();
        TimerSavedData.get(server).removeKeyForAll(key);
        for (Map<String, PlayerTimerData> playerTimers : activeTimers.values()) {
            playerTimers.remove(key);
        }
        LAST_SCOPE_KEY.values().removeIf(key::equals);
    }

    /**
     * Boot sweep for sessions whose instance no longer exists, covering a crash between the region
     * clear and the purge. Mirrors {@code reconcileOrphanedSlotsToInstances}; idempotent.
     */
    public static void sweepOrphanedSessions(MinecraftServer server) {
        DungeonPositionSavedData positions = DungeonPositionSavedData.get(server);
        int removed = TimerSavedData.get(server).removeOrphanedSessions(scope -> {
            try {
                return positions.isInstanceLive(UUID.fromString(scope.scopeId()));
            } catch (IllegalArgumentException e) {
                return false;
            }
        });
        if (removed > 0) {
            CobbleSafari.LOGGER.info("Boot sweep removed {} orphaned timer session(s)", removed);
        }
    }

    public static boolean isInSafariDimension(ServerPlayer player) {
        ResourceLocation playerDimension = player.level().dimension().location();
        return playerDimension.equals(SafariTimerConfig.getSafariDimensionRL());
    }

    public static Optional<String> getConfiguredDimensionId(ServerPlayer player) {
        String playerDimension = player.level().dimension().location().toString();
        if (SafariTimerConfig.hasDimensionTimer(playerDimension)) {
            return Optional.of(playerDimension);
        }
        return Optional.empty();
    }

    /** Arms an account timer. Non-instanced dimensions only; sessions use {@link #startSession}. */
    public static void startTimer(ServerPlayer player, String dimensionId) {
        UUID playerId = player.getUUID();
        PlayerTimerData data = getOrCreateAccount(player, dimensionId);

        checkDailyReset(player, data);

        data.setActive(true);
        activeTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(TimerScope.of(dimensionId).key(), data);
        syncToClient(player, data);

        CobbleSafari.LOGGER.debug("Timer started for player {} in dimension {} with {} ticks remaining",
                player.getName().getString(), dimensionId, data.getRemainingTicks());
    }

    /** @deprecated use {@link #startTimer(ServerPlayer, String)} with an explicit dimension id. */
    @Deprecated
    public static void startTimer(ServerPlayer player) {
        startTimer(player, SafariTimerConfig.getSafariDimensionId());
    }

    /** Arms (or re-arms) an existing session. Never creates one - only {@link #openSession} does. */
    public static void startSession(ServerPlayer player, TimerScope scope) {
        PlayerTimerData data = session(player, scope);
        if (data == null) {
            CobbleSafari.LOGGER.warn("startSession called for {} with no session at scope {}",
                    player.getName().getString(), scope.key());
            return;
        }
        data.setActive(true);
        LAST_SCOPE_KEY.put(player.getUUID(), scope.key());
        activeTimers.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(scope.key(), data);
        savePlayerData(player, data);
        syncToClient(player, data);

        CobbleSafari.LOGGER.debug("Timer session {} started for player {} with {} ticks remaining",
                scope.key(), player.getName().getString(), data.getRemainingTicks());
    }

    public static void pauseTimer(ServerPlayer player, TimerScope scope) {
        Map<String, PlayerTimerData> playerTimers = activeTimers.get(player.getUUID());

        if (playerTimers != null) {
            PlayerTimerData data = playerTimers.get(scope.key());
            if (data != null) {
                data.setActive(false);
                savePlayerData(player, data);
                syncToClient(player, data);

                CobbleSafari.LOGGER.debug("Timer paused for player {} at scope {} with {} ticks remaining",
                        player.getName().getString(), scope.key(), data.getRemainingTicks());
            }
        }
    }

    /** @deprecated use {@link #pauseTimer(ServerPlayer, TimerScope)} with an explicit scope. */
    @Deprecated
    public static void pauseTimer(ServerPlayer player) {
        pauseTimer(player, TimerScope.of(SafariTimerConfig.getSafariDimensionId()));
    }

    public static void pauseAllTimers(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Map<String, PlayerTimerData> playerTimers = activeTimers.get(playerId);

        if (playerTimers != null) {
            for (PlayerTimerData data : playerTimers.values()) {
                if (data.isActive()) {
                    data.setActive(false);
                    savePlayerData(player, data);
                    syncToClient(player, data);
                }
            }
        }
    }

    public static void tickAllTimers() {
        if (serverInstance == null) return;

        int tickCount = serverInstance.getTickCount();

        // The presence invariant runs at 20 ticks now that it has absorbed checkDungeonStillExists
        // (plan 181 § 5.4), preserving that guard's eviction latency. It is also cheaper than what
        // it replaced: a map lookup plus one footprint test, instead of an
        // O(active portals x registered dungeons) scan per player.
        if (tickCount % 20 == 0) {
            sweepPresenceInvariant();
        }

        for (Map.Entry<UUID, Map<String, PlayerTimerData>> playerEntry : activeTimers.entrySet()) {
            UUID playerId = playerEntry.getKey();
            Map<String, PlayerTimerData> scopedTimers = playerEntry.getValue();

            // Resolved once per player (all of a player's timers share the same player object,
            // current dimension, current scope, and bypass flag for this tick).
            ServerPlayer player = null;
            boolean playerResolved = false;
            String playerDimension = null;
            String currentScopeKey = null;
            Boolean bypassed = null;

            for (Map.Entry<String, PlayerTimerData> timerEntry : scopedTimers.entrySet()) {
                String scopeKey = timerEntry.getKey();
                PlayerTimerData data = timerEntry.getValue();

                if (!data.isActive()) {
                    continue;
                }

                if (!playerResolved) {
                    player = getPlayerByUUID(playerId);
                    playerResolved = true;
                    if (player != null) {
                        playerDimension = player.level().dimension().location().toString();
                        TimerScope resolved = currentScope(player);
                        currentScopeKey = resolved == null ? null : resolved.key();
                        if (resolved != null && resolved.isSession()) {
                            LAST_SCOPE_KEY.put(playerId, resolved.key());
                        }
                    }
                }

                if (player != null) {
                    // Compare the SCOPE, not the key: a session key is "dim@instance" and would
                    // never equal a dimension id. Getting this wrong freezes every dungeon timer.
                    String timerDimensionId = data.getDimensionId();
                    boolean sameDimension = playerDimension.equals(timerDimensionId);
                    // At most one timer is active per player per tick: anything that is not the
                    // scope they currently stand in gets paused. That is what keeps the HUD sync -
                    // which carries only a dimension id - unambiguous.
                    boolean sameScope = sameDimension
                            && (currentScopeKey == null ? !data.isSession() : currentScopeKey.equals(scopeKey));

                    if (!sameScope) {
                        data.setActive(false);
                        savePlayerData(player, data);
                        syncToClient(player, data);
                        CobbleSafari.LOGGER.debug("Timer auto-paused for player {} - no longer at scope {}",
                                player.getName().getString(), scopeKey);
                        continue;
                    }

                    if (DungeonDimensions.isDungeonDimension(timerDimensionId)) {
                        if (player.getY() < 0) {
                            expireActiveTimerAndTeleport(player, data.scope(), data, true);
                            CobbleSafari.LOGGER.info("Player {} detected in void in dungeon dimension {}, teleporting out",
                                    player.getName().getString(), timerDimensionId);
                            continue;
                        }
                    } else if (UnionRoomManager.DIMENSION_ID.equals(timerDimensionId) && player.getY() < 0) {
                        expireActiveTimerAndTeleport(player, data.scope(), data, true);
                        CobbleSafari.LOGGER.info("Player {} detected in void in union room, teleporting out",
                                player.getName().getString());
                        continue;
                    }

                    if (bypassed == null) {
                        bypassed = shouldBypassTimer(player);
                    }
                    if (bypassed) {
                        if (tickCount % 20 == 0) {
                            syncToClient(player, data, true);
                        }
                        continue;
                    }
                }

                data.tick();

                // A nested visit spends its parent's credit as it goes (action plan 183, section
                // 6.2 as corrected): charging on the way out cannot work, because every eviction
                // path zeroes the credit first and the closing subtraction then bills the whole
                // grant.
                if (player != null && data.getParentScopeId() != null) {
                    maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager.debitParentForTick(player, data);
                }

                if (data.isExpired() && player != null) {
                    expireActiveTimerAndTeleport(player, data.scope(), data, true);
                }

                if (data.getRemainingTicks() % 20 == 0 && player != null) {
                    syncToClient(player, data);
                }
            }
        }
    }

    /**
     * Presence invariant (audit 145 B6, Motif § 5 - guard by invariant, not by vector).
     * <p>
     * On a non-instanced dimension the question is unchanged: re-arm a missing timer, exactly like
     * the entry path. On an <b>instanced</b> dimension the question is no longer "does this player
     * have credit here?" - to which a player who has never seen a portal answers <em>yes</em>
     * (finding S1) - but:
     *
     * <blockquote>Does this player hold a live session for the scope they are standing in?</blockquote>
     *
     * Nothing can forge a session: admin commands may credit one, but only passing through the door
     * creates one. This also subsumes the old {@code checkDungeonStillExists}, which asked a
     * dimension-wide question that any third party's portal could answer (plan 181 § 5.4).
     * <p>
     * Eviction is deliberately decided on <b>two</b> questions rather than one, because
     * {@code findInstanceByPosition}'s third pass matches the nearest anchor within a whole
     * {@code zoneSize} - the exact grid pitch between instances - and so can resolve to a live but
     * wrong neighbour. Evicting on that would eject legitimate players, so:
     * <ol>
     *   <li>the <em>footprint</em> resolves and the player does not hold that session ⇒ evict;</li>
     *   <li>otherwise, the player holds no live session <em>near where they stand</em> ⇒ evict;</li>
     *   <li>otherwise keep them, adopting the sticky scope, else their single nearby session.</li>
     * </ol>
     * Rule 2 is the one that catches a gate-crasher wherever they land; rule 3 is the one that makes
     * a false positive impossible, since a legitimate player always holds a live session here.
     */
    private static void sweepPresenceInvariant() {
        if (serverInstance == null) {
            return;
        }
        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            Optional<String> dimOpt = getConfiguredDimensionId(player);
            if (dimOpt.isEmpty()) {
                continue;
            }
            String dimId = dimOpt.get();

            // The void guard is about physics, not permission or bookkeeping, so it runs before
            // everything else - including the bypass check, and regardless of whether the player
            // has an active timer.
            //
            // It used to live only inside tickAllTimers, which skips records that are not active.
            // A player who had left the dungeon (their session paused) and returned by any other
            // route therefore had no void protection at all, and simply fell out of the world.
            if (DungeonDimensions.isDungeonDimension(dimId) && player.getY() < 0) {
                evacuateFromVoid(player, dimId);
                continue;
            }

            if (shouldBypassTimer(player)) {
                continue;
            }

            if (!isInstancedDimension(dimId)) {
                sweepAccountPresence(player, dimId);
            } else {
                sweepSessionPresence(player, dimId);
            }
        }
    }

    /** Non-instanced dimensions keep the balance-based guard, hardened for the Safari by plan 120. */
    private static void sweepAccountPresence(ServerPlayer player, String dimId) {
        Map<String, PlayerTimerData> timers = activeTimers.get(player.getUUID());
        PlayerTimerData data = timers == null ? null : timers.get(TimerScope.of(dimId).key());
        if (data == null || !data.isActive()) {
            CobbleSafari.LOGGER.debug("Presence sweep: arming missing timer for {} in {}",
                    player.getName().getString(), dimId);
            startTimer(player, dimId);
        }
    }

    private static void sweepSessionPresence(ServerPlayer player, String dimId) {
        DungeonInstanceRecord footprint = DungeonPositionSavedData.get(serverInstance)
                .findInstanceByFootprint(dimId, player.blockPosition());

        // Rule 1 - the footprint is certain, so a missing session here is decisive.
        if (footprint != null) {
            TimerScope scope = TimerScope.of(dimId, footprint.instanceId());
            PlayerTimerData held = session(player, scope);
            if (held == null) {
                evictWithoutSession(player, dimId, "no session for the instance they are standing in");
                return;
            }
            LAST_SCOPE_KEY.put(player.getUUID(), scope.key());
            if (!held.isActive()) {
                startSession(player, scope);
            }
            return;
        }

        // Rule 2 - resolution was uncertain, so fall back to the sessions they hold here. Only the
        // ones they are plausibly STANDING IN count.
        //
        // Holding a live session is not on its own evidence of being anywhere: a session outlives
        // the visit by design (plan 181), so someone who walked out and then teleported into an
        // empty corner of the dimension still holds one. Without the proximity filter this rule
        // kept them indefinitely, and they were only evicted when the instance finally expired -
        // observed in testing at 400 000 blocks from the nearest anchor. It is the same mistake as
        // occupancy clause (c) made before correctif C4, in the opposite direction: there it
        // retained a region for an absent player, here it retained a player for a distant region.
        //
        // The net exists for a resolution hole NEAR the structure, which is exactly what
        // isNearAnchor bounds (2 x zoneSize, against a documented jigsaw reach of zoneSize).
        List<PlayerTimerData> nearby = liveSessions(player, dimId).stream()
                .filter(d -> isNearAnchor(player, d.scope()))
                .toList();
        if (nearby.isEmpty()) {
            evictWithoutSession(player, dimId, "holds no live session anywhere near their position");
            return;
        }

        // Rule 3 - keep them. Prefer the sticky scope, else the single nearby session.
        TimerScope sticky = stickyScope(player, dimId);
        PlayerTimerData chosen;
        if (sticky != null) {
            chosen = session(player, sticky);
        } else if (nearby.size() == 1) {
            chosen = nearby.get(0);
        } else {
            CobbleSafari.LOGGER.warn(
                    "Presence sweep: {} is in {} at {} with {} nearby live sessions and no resolvable "
                            + "footprint; keeping them and leaving the timer as-is",
                    player.getName().getString(), dimId, player.blockPosition(), nearby.size());
            return;
        }
        if (chosen != null && !chosen.isActive()) {
            startSession(player, chosen.scope());
        }
    }

    /** Void rescue: use the session's own origin when there is one, world spawn otherwise. */
    private static void evacuateFromVoid(ServerPlayer player, String dimId) {
        CobbleSafari.LOGGER.info("Player {} detected in void in dungeon dimension {}, teleporting out",
                player.getName().getString(), dimId);
        TimerScope scope = currentScope(player);
        if (scope != null && scope.isSession() && session(player, scope) != null) {
            expireSessionAndTeleport(player, scope, true);
        } else {
            teleportToOverworldSpawn(player);
        }
    }

    private static void evictWithoutSession(ServerPlayer player, String dimId, String reason) {
        CobbleSafari.LOGGER.warn("Presence sweep: evacuating {} from {} at {} - {}",
                player.getName().getString(), dimId, player.blockPosition(), reason);
        // Upgrade day: the § 10 migration threw this player's legacy dungeon wallet away, but kept
        // the origin it recorded. Use it for this one evacuation so they land where they came from
        // rather than at world spawn.
        PlayerTimerData legacy = TimerSavedData.get(serverInstance).takeLegacyOrigin(player.getUUID(), dimId);
        if (legacy != null) {
            teleportOnTimerExpired(player, TimerScope.of(dimId), legacy, true);
        } else {
            teleportToOverworldSpawn(player);
        }
    }

    /**
     * Join-time scope recovery (mirrors plan 179 V4 § 4.2). The sweep runs within 20 ticks of a
     * login and {@link #LAST_SCOPE_KEY} is empty then, so give a returning player one chance to have
     * their single live session adopted before the guard looks at them.
     */
    public static void recoverScopeOnJoin(ServerPlayer player) {
        Optional<String> dimOpt = getConfiguredDimensionId(player);
        if (dimOpt.isEmpty() || serverInstance == null) {
            return;
        }
        String dimId = dimOpt.get();
        if (!isInstancedDimension(dimId)) {
            return;
        }
        if (DungeonPositionSavedData.get(serverInstance)
                .findInstanceByPosition(dimId, player.blockPosition()) != null) {
            return; // position resolves on its own
        }
        // Only a session they are plausibly standing in may be adopted: possession alone says
        // nothing about location, since a session outlives the visit (same reasoning as C9).
        // stickyScope re-checks this anyway, so an unfiltered adoption would merely be inert and
        // log a recovery that never happened.
        List<PlayerTimerData> nearby = liveSessions(player, dimId).stream()
                .filter(d -> isNearAnchor(player, d.scope()))
                .toList();
        if (nearby.size() == 1) {
            TimerScope scope = nearby.get(0).scope();
            LAST_SCOPE_KEY.put(player.getUUID(), scope.key());
            CobbleSafari.LOGGER.info("Recovered timer scope {} for {} on join",
                    scope.key(), player.getName().getString());
        }
    }

    public static void checkDailyReset(ServerPlayer player, PlayerTimerData data) {
        Optional<DimensionTimerEntry> configOpt = SafariTimerConfig.getDimensionConfig(data.getDimensionId());
        boolean allowReset = configOpt.map(DimensionTimerEntry::isAllowReset).orElse(true);
        if (!allowReset) return;

        long lastResetMs = data.getLastResetTimestamp();

        LocalDate lastResetDate = Instant.ofEpochMilli(lastResetMs)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalTime now = LocalTime.now(ZoneId.systemDefault());
        int resetHour = data.getResetHour();

        boolean shouldReset = false;

        if (today.isAfter(lastResetDate)) {
            shouldReset = now.getHour() >= resetHour || today.isAfter(lastResetDate.plusDays(1));
        }

        if (shouldReset) {
            String dimensionId = data.getDimensionId();
            boolean isSafari = dimensionId.equals(SafariTimerConfig.getSafariDimensionId());
            boolean playerInSafari = isSafari && isInSafariDimension(player);

            if (playerInSafari && shouldKickOnDailyReset()) {
                teleportOnTimerExpired(player, data.scope(), data);
                player.sendSystemMessage(Component.translatable("cobblesafari.reset.daily_kick"));
            }

            data.resetForDailyCycle();
            data.resetEntryFeePayDay();
            data.setLastSafariBallGrantDay(0);
            if (isSafari && SafariConfig.isEntryFeeEnabled()) {
                player.sendSystemMessage(Component.translatable("cobblesafari.timer.reset_paid"));
            } else {
                player.sendSystemMessage(Component.translatable("cobblesafari.timer.reset"));
            }
            CobbleSafari.LOGGER.info("Timer reset for player {} in dimension {} (daily reset)",
                    player.getName().getString(), data.getDimensionId());
        }
    }

    public static String formatTimeUntilNextDailyReset(PlayerTimerData data) {
        Optional<DimensionTimerEntry> configOpt = SafariTimerConfig.getDimensionConfig(data.getDimensionId());
        if (configOpt.isPresent() && !configOpt.get().isAllowReset()) {
            return formatDurationHms(0);
        }
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalDate lastResetDate = Instant.ofEpochMilli(data.getLastResetTimestamp())
                .atZone(zone)
                .toLocalDate();
        int resetHour = data.getResetHour();
        ZonedDateTime next = computeNextDailyResetInstant(now, lastResetDate, resetHour, zone);
        long seconds = Duration.between(now, next).getSeconds();
        return formatDurationHms(Math.max(0, seconds));
    }

    private static ZonedDateTime computeNextDailyResetInstant(
            ZonedDateTime now, LocalDate lastResetDate, int resetHour, ZoneId zone) {
        if (wouldDailyResetTriggerAt(now, lastResetDate, resetHour)) {
            return now;
        }
        LocalDate today = now.toLocalDate();
        if (!today.isAfter(lastResetDate)) {
            return lastResetDate.plusDays(1).atTime(resetHour, 0, 0).atZone(zone);
        }
        long daysBetween = ChronoUnit.DAYS.between(lastResetDate, today);
        if (daysBetween == 1) {
            LocalDateTime todayReset = today.atTime(resetHour, 0, 0);
            if (now.toLocalDateTime().isBefore(todayReset)) {
                return todayReset.atZone(zone);
            }
        }
        return now;
    }

    private static boolean wouldDailyResetTriggerAt(ZonedDateTime zdt, LocalDate lastResetDate, int resetHour) {
        LocalDate day = zdt.toLocalDate();
        LocalTime time = zdt.toLocalTime();
        if (!day.isAfter(lastResetDate)) {
            return false;
        }
        if (time.getHour() >= resetHour) {
            return true;
        }
        return day.isAfter(lastResetDate.plusDays(1));
    }

    private static String formatDurationHms(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static boolean shouldKickOnDailyReset() {
        if (SafariConfig.isKickOnReset()) return true;
        return SafariConfig.isEntryFeeEnabled();
    }

    public static void teleportToOverworldSpawn(ServerPlayer player) {
        if (serverInstance == null) return;

        ServerLevel overworld = serverInstance.getLevel(Level.OVERWORLD);

        if (overworld != null) {
            BlockPos spawnPos = overworld.getSharedSpawnPos();

            player.teleportTo(
                    overworld,
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot()
            );
            player.resetFallDistance();
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

            player.sendSystemMessage(Component.translatable("cobblesafari.timer.expired"));
            CobbleSafari.LOGGER.info("Player {} teleported to Overworld spawn (timer expired)",
                    player.getName().getString());
        }
    }

    public static void teleportOnTimerExpired(ServerPlayer player, TimerScope scope, PlayerTimerData data) {
        teleportOnTimerExpired(player, scope, data, true);
    }

    private static void teleportOnTimerExpired(ServerPlayer player, TimerScope scope, PlayerTimerData data, boolean notifyExpired) {
        teleportOnTimerExpired(player, scope, data, notifyExpired, false);
    }

    private static void teleportOnTimerExpired(ServerPlayer player, TimerScope scope, PlayerTimerData data, boolean notifyExpired,
                                              boolean unionGracefulDeparture) {
        if (serverInstance == null) return;

        String dimensionId = scope.dimensionId();
        Optional<DimensionTimerEntry> config = SafariTimerConfig.getDimensionConfig(dimensionId);
        boolean returnToSpawn = config.map(DimensionTimerEntry::isReturnToSpawn).orElse(false);

        BlockPos targetPos;
        ServerLevel targetLevel;

        if (returnToSpawn) {
            targetLevel = serverInstance.getLevel(Level.OVERWORLD);
            if (targetLevel != null) {
                targetPos = targetLevel.getSharedSpawnPos();
            } else {
                CobbleSafari.LOGGER.error("Overworld not found for timer expiration");
                return;
            }
        } else {
            if (data.getOriginPos() != null && data.getOriginDimension() != null) {
                targetPos = data.getOriginPos();
                targetLevel = serverInstance.getLevel(data.getOriginDimension());
                if (targetLevel == null) {
                    CobbleSafari.LOGGER.warn("Origin dimension {} not found, falling back to overworld spawn",
                            data.getOriginDimension().location());
                    targetLevel = serverInstance.getLevel(Level.OVERWORLD);
                    if (targetLevel != null) {
                        targetPos = targetLevel.getSharedSpawnPos();
                    } else {
                        return;
                    }
                }
            } else {
                targetLevel = serverInstance.getLevel(Level.OVERWORLD);
                if (targetLevel != null) {
                    targetPos = targetLevel.getSharedSpawnPos();
                } else {
                    CobbleSafari.LOGGER.error("Overworld not found and no origin position stored");
                    return;
                }
            }
        }

        player.teleportTo(
                targetLevel,
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
        player.resetFallDistance();
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

        if (DungeonDimensions.isDungeonDimension(dimensionId)) {
            DungeonTeleportHandler.clearPlayerData(player.getUUID());
        }

        if (notifyExpired) {
            if (UnionRoomManager.DIMENSION_ID.equals(dimensionId) && unionGracefulDeparture) {
                player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.session_closed.thanks"));
            } else {
                player.sendSystemMessage(Component.translatable("cobblesafari.timer.expired"));
            }
        }
        CobbleSafari.LOGGER.info("Player {} teleported to {} at {} (timer expired, returnToSpawn: {})",
                player.getName().getString(), targetLevel.dimension().location(), targetPos, returnToSpawn);
    }

    /**
     * Every eviction of a <b>nested</b> visit - an expiry, the void guard, a death - goes through
     * the sub-dungeon's own departure instead: it settles, returns the player to the parent rather
     * than to the overworld, re-arms the parent session, and releases the region. Routing it here
     * rather than at each call site is the same "one path, not a list of triggers" rule the release
     * sweep follows (action plan 183, section 7).
     *
     * @return true when the caller must stop, because the departure has been handled
     */
    private static boolean divertNestedVisit(ServerPlayer player, TimerScope scope, PlayerTimerData data,
                                             boolean notify) {
        if (data == null || data.getParentScopeId() == null || serverInstance == null) {
            return false;
        }
        maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager.leave(serverInstance, player, scope, notify);
        return true;
    }

    public static void expireActiveTimerAndTeleport(ServerPlayer player, TimerScope scope, PlayerTimerData data, boolean notifyExpired) {
        expireActiveTimerAndTeleport(player, scope, data, notifyExpired, false);
    }

    /**
     * @param unionGracefulDeparture when true and dimension is the Union Room, players see the friendly
     * departure line instead of {@code cobblesafari.timer.expired} (session close / evac).
     */
    public static void expireActiveTimerAndTeleport(ServerPlayer player, TimerScope scope, PlayerTimerData data,
                                                    boolean notifyExpired, boolean unionGracefulDeparture) {
        // A nested visit never expires "to the overworld": it goes home to its parent.
        if (divertNestedVisit(player, scope, data, notifyExpired)) {
            return;
        }
        String timerDimensionId = scope.dimensionId();
        if (UnionRoomManager.DIMENSION_ID.equals(timerDimensionId)) {
            UnionRoomManager.onBeforeUnionRoomTimerExpire(player);
        }
        data.setRemainingTicks(0);
        teleportOnTimerExpired(player, scope, data, notifyExpired, unionGracefulDeparture);
        if (UnionRoomManager.DIMENSION_ID.equals(timerDimensionId)) {
            UnionRoomManager.onAfterUnionRoomTimerExpire(player);
        }
        data.setActive(false);
        savePlayerData(player, data);
    }

    /**
     * Evacuates a player from a scope the caller already knows, for callers that cannot resolve it
     * by position - notably the region clear, where the player is standing in blocks that are about
     * to be erased and whose instance record is the only reliable answer (plan 181 § 7).
     * <p>
     * A bypassed player <b>is</b> evacuated here: this is not a permission check, it is a region
     * that is about to stop existing.
     */
    /**
     * The record for {@code scope}, without ever creating one on an instanced dimension.
     * <p>
     * A bare scope on an instanced dimension is always a caller that could not resolve the visit;
     * answering it with {@code getOrCreateAccount} would mint a full-credit record - finding S1's
     * constructor hole - on exactly the paths that run when things have already gone wrong
     * (evacuation, death rescue). Returns {@code null} instead and lets the caller cope.
     */
    private static PlayerTimerData resolveRecord(ServerPlayer player, TimerScope scope) {
        if (scope.isSession()) {
            return session(player, scope);
        }
        if (isInstancedDimension(scope.dimensionId())) {
            return serverInstance == null
                    ? null
                    : TimerSavedData.get(serverInstance).getPlayerData(player.getUUID(), scope);
        }
        return getOrCreateAccount(player, scope.dimensionId());
    }

    public static void expireSessionAndTeleport(ServerPlayer player, TimerScope scope, boolean notifyExpired) {
        PlayerTimerData data = resolveRecord(player, scope);
        if (data == null) {
            // No session: an intruder, or a record already purged. Still get them out.
            CobbleSafari.LOGGER.warn("Evacuating {} from {} with no timer record",
                    player.getName().getString(), scope.key());
            teleportToOverworldSpawn(player);
            return;
        }
        expireActiveTimerAndTeleport(player, scope, data, notifyExpired);
    }

    public static void setPlayerOrigin(ServerPlayer player, TimerScope scope, BlockPos originPos, ResourceKey<Level> originDimension) {
        PlayerTimerData data = resolveRecord(player, scope);
        if (data == null) {
            CobbleSafari.LOGGER.warn("setPlayerOrigin: no record for {} at scope {}",
                    player.getName().getString(), scope.key());
            return;
        }
        data.setOriginPos(originPos);
        data.setOriginDimension(originDimension);
        savePlayerData(player, data);
        CobbleSafari.LOGGER.debug("Set origin position for player {} at scope {}: {} in {}",
                player.getName().getString(), scope.key(), originPos, originDimension.location());
    }

    private static final String SAFARI_BALL_ITEM_ID = "cobblemon:safari_ball";

    public static void grantSafariBallsIfFirstUseToday(ServerPlayer player) {
        if (serverInstance == null) return;
        if (!isInSafariDimension(player)) return;

        String safariDimensionId = SafariTimerConfig.getSafariDimensionId();
        PlayerTimerData data = getOrCreateAccount(player, safariDimensionId);
        long todayEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay();

        if (data.getLastSafariBallGrantDay() == todayEpochDay) {
            return;
        }

        int safariBallsCount = SafariConfig.getDailySafariBallsCount();
        int baitCount = SafariConfig.getDailyBaitCount();
        int mudBallCount = SafariConfig.getDailyMudBallCount();

        if (safariBallsCount > 0) {
            Item safariBall = BuiltInRegistries.ITEM.get(ResourceLocation.parse(SAFARI_BALL_ITEM_ID));
            if (safariBall != null && !safariBall.getDefaultInstance().isEmpty()) {
                ItemStack toGive = new ItemStack(safariBall, safariBallsCount);
                if (!player.getInventory().add(toGive)) {
                    player.drop(toGive, false);
                }
            } else {
                CobbleSafari.LOGGER.warn("Safari ball item not found: {}", SAFARI_BALL_ITEM_ID);
            }
        }

        if (baitCount > 0) {
            ItemStack bait = new ItemStack(maxigregrze.cobblesafari.init.ModItems.BAIT, baitCount);
            if (!player.getInventory().add(bait)) {
                player.drop(bait, false);
            }
        }

        if (mudBallCount > 0) {
            ItemStack mudBalls = new ItemStack(maxigregrze.cobblesafari.init.ModItems.MUD_BALL, mudBallCount);
            if (!player.getInventory().add(mudBalls)) {
                player.drop(mudBalls, false);
            }
        }

        data.setLastSafariBallGrantDay(todayEpochDay);
        savePlayerData(player, data);
        player.sendSystemMessage(Component.translatable("cobblesafari.teleporter.safari_items_received", 
                safariBallsCount, baitCount, mudBallCount));
        CobbleSafari.LOGGER.info("Granted {} Safari Balls, {} Bait, {} Mud Balls to {} (first use today)", 
                safariBallsCount, baitCount, mudBallCount, player.getName().getString());
    }

    public static void syncToClient(ServerPlayer player, PlayerTimerData data, boolean bypassed) {
        ModNetworking.sendTimerSync(player, data.getDimensionId(), data.getRemainingTicks(), data.isActive(), bypassed);
    }

    public static void syncToClient(ServerPlayer player, PlayerTimerData data) {
        syncToClient(player, data, false);
    }

    /**
     * Account lookup for a <b>non-instanced</b> dimension (Safari, Union Room). Never returns
     * {@code null}.
     * <p>
     * Renamed from {@code getOrCreateData} by plan 181 (V2): on an instanced dimension, creating a
     * record on read is precisely finding S1 - the constructor credits the full configured duration,
     * so a player who has never seen a portal gets a free run. Renaming rather than adding a
     * {@code null} return turns every call site into a compile error instead of a runtime surprise.
     */
    public static PlayerTimerData getOrCreateAccount(ServerPlayer player, String dimensionId) {
        UUID playerId = player.getUUID();
        String key = TimerScope.of(dimensionId).key();

        if (isInstancedDimension(dimensionId)) {
            // Belt and braces for any call site the rename missed. Log loudly, and hand back a
            // detached zero-credit record: it is neither cached nor persisted, so nothing here can
            // mint the unearned budget of finding S1 or write a bare key on an instanced dimension.
            CobbleSafari.LOGGER.error(
                    "getOrCreateAccount called for instanced dimension {} (player {}); "
                            + "instanced dimensions use openSession/session instead",
                    dimensionId, player.getName().getString());
            return new PlayerTimerData(playerId, dimensionId, null, 0, System.currentTimeMillis(), false);
        }

        Map<String, PlayerTimerData> playerTimers = activeTimers.get(playerId);
        if (playerTimers != null) {
            PlayerTimerData data = playerTimers.get(key);
            if (data != null) {
                return data;
            }
        }

        if (serverInstance != null) {
            TimerSavedData savedData = TimerSavedData.get(serverInstance);
            PlayerTimerData data = savedData.getOrCreateAccount(playerId, dimensionId);
            activeTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(key, data);
            return data;
        }

        PlayerTimerData data = new PlayerTimerData(playerId, dimensionId);
        activeTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(key, data);
        return data;
    }

    /** @deprecated use {@link #getOrCreateAccount(ServerPlayer, String)} with an explicit dimension id. */
    @Deprecated
    public static PlayerTimerData getOrCreateAccount(ServerPlayer player) {
        return getOrCreateAccount(player, SafariTimerConfig.getSafariDimensionId());
    }

    public static void loadPlayerData(ServerPlayer player) {
        if (serverInstance == null) return;

        UUID playerId = player.getUUID();
        TimerSavedData savedData = TimerSavedData.get(serverInstance);

        // Accounts only. Creating a record per configured dimension at every join is the
        // constructor half of S1: it handed every player a full dungeon budget on first login.
        // Sessions are created at the door and nowhere else.
        for (String dimensionId : SafariTimerConfig.getConfiguredDimensionIds()) {
            if (isInstancedDimension(dimensionId)) {
                continue;
            }
            PlayerTimerData data = savedData.getOrCreateAccount(playerId, dimensionId);
            checkDailyReset(player, data);
            activeTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                    .put(TimerScope.of(dimensionId).key(), data);
        }

        String safariDimId = SafariTimerConfig.getSafariDimensionId();
        PlayerTimerData safariData = savedData.getOrCreateAccount(playerId, safariDimId);
        if (safariData.isNeedsEvacuation()) {
            SafariResetManager.addPendingEvacuation(playerId);
            safariData.setNeedsEvacuation(false);
            savedData.setPlayerData(playerId, safariData);
        }

        recoverScopeOnJoin(player);

        TimerScope scope = currentScope(player);
        if (scope != null) {
            PlayerTimerData data = scope.isSession()
                    ? session(player, scope)
                    : getOrCreateAccount(player, scope.dimensionId());
            if (data != null) {
                data.setActive(data.getRemainingTicks() > 0);
                if (scope.isSession()) {
                    LAST_SCOPE_KEY.put(playerId, scope.key());
                    activeTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                            .put(scope.key(), data);
                }
                syncToClient(player, data);
            }
        }

        CobbleSafari.LOGGER.debug("Loaded timer data for player {}", player.getName().getString());
    }

    public static void onDeathWhileTimed(ServerPlayer player, TimerScope scope) {
        // Dying somewhere the scope could not be resolved hands us a bare dimension scope; on an
        // instanced dimension that must not become an account (observed in a live log as an
        // ERROR from getOrCreateAccount during a death rescue).
        PlayerTimerData data = resolveRecord(player, scope);
        if (data == null) {
            // No session to drain (an intruder, or a purged instance): still cancel the death and
            // get them out, which is what the rescue is for.
            CobbleSafari.LOGGER.warn("Death rescue for {} at scope {} with no timer record",
                    player.getName().getString(), scope.key());
            player.setHealth(1.0F);
            player.setAbsorptionAmount(0.0F);
            player.resetFallDistance();
            player.setDeltaMovement(Vec3.ZERO);
            player.invulnerableTime = 20;
            player.sendSystemMessage(Component.translatable("cobblesafari.timer.death_drained"));
            teleportToOverworldSpawn(player);
            return;
        }
        String dimensionId = scope.dimensionId();
        player.setHealth(1.0F);
        player.setAbsorptionAmount(0.0F);
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.invulnerableTime = 20;
        player.sendSystemMessage(Component.translatable("cobblesafari.timer.death_drained"));

        // Dying in a sub-dungeon returns to the PARENT with the debit applied, not to the
        // overworld (action plan 183, section 7.2). The entry stays consumed: spec v2 section 4.8
        // makes that the chosen behaviour, so a death cannot buy a second roll.
        if (data.getParentScopeId() != null && serverInstance != null) {
            maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager.leave(serverInstance, player, scope, true);
            CobbleSafari.LOGGER.info("Death cancelled for {} in sub-dungeon {}: settled and returned to parent",
                    player.getName().getString(), dimensionId);
            return;
        }

        int saves = maxigregrze.cobblesafari.init.ModStats.awardAndGet(
                player, maxigregrze.cobblesafari.init.ModStats.HOOPA_SAVES);
        maxigregrze.cobblesafari.advancement.ModCriteria.HOOPA_SAVE.trigger(player, saves);
        // The session survives at zero credit: that is what lets an operator credit it and the
        // player walk back through the portal into the same instance (plan 181 § 9.1).
        expireActiveTimerAndTeleport(player, scope, data, false);
        CobbleSafari.LOGGER.info("Death cancelled for {} in {}: timer expired (death rescue, same as void evac)",
                player.getName().getString(), dimensionId);
    }

    /** Always writes under the record's own scope - the only key that can be correct for it. */
    public static void savePlayerData(ServerPlayer player, PlayerTimerData data) {
        if (serverInstance == null) return;

        TimerSavedData savedData = TimerSavedData.get(serverInstance);
        savedData.setPlayerData(player.getUUID(), data);
    }

    public static void saveAllData() {
        if (serverInstance == null) return;

        TimerSavedData savedData = TimerSavedData.get(serverInstance);
        int totalTimers = 0;

        for (Map.Entry<UUID, Map<String, PlayerTimerData>> playerEntry : activeTimers.entrySet()) {
            UUID playerId = playerEntry.getKey();
            for (PlayerTimerData data : playerEntry.getValue().values()) {
                savedData.setPlayerData(playerId, data);
                totalTimers++;
            }
        }

        CobbleSafari.LOGGER.info("Saved all timer data ({} players, {} timers)", activeTimers.size(), totalTimers);
    }

    /** Drops the in-memory account of a non-instanced dimension (daily reset). */
    public static void resetDimensionTimers(String dimensionId) {
        String key = TimerScope.of(dimensionId).key();
        for (Map<String, PlayerTimerData> playerTimers : activeTimers.values()) {
            playerTimers.remove(key);
        }
    }

    /**
     * The scope the player is leaving behind by logging out, or {@code null}. Read <em>before</em>
     * {@link #onPlayerDisconnect} tears their state down (action plan 182, A3).
     */
    public static TimerScope scopeOnDisconnect(ServerPlayer player) {
        TimerScope scope = currentScope(player);
        if (scope == null || !scope.isSession()) {
            return null;
        }
        return session(player, scope) == null ? null : scope;
    }

    /**
     * Remembers where to put this player back if the instance they logged out in is cleared while
     * they are away (action plan 182, A3).
     * <p>
     * Written at disconnect rather than at release, because their session - which carries the origin
     * since plan 181 V6 - is purged with the instance, and they are offline when that happens.
     * Recording it here makes the rule independent of ordering.
     */
    public static void recordPendingReturn(ServerPlayer player, TimerScope scope) {
        if (serverInstance == null) return;
        PlayerTimerData data = session(player, scope);
        if (data == null || data.getOriginPos() == null || data.getOriginDimension() == null) {
            return;
        }
        TimerSavedData.get(serverInstance)
                .setPendingReturn(player.getUUID(), data.getOriginPos(), data.getOriginDimension());
        CobbleSafari.LOGGER.debug("Recorded pending dungeon return for {} (was in {})",
                player.getName().getString(), scope.key());
    }

    /**
     * On login: either the player is still inside a live instance they hold - nothing happened, drop
     * the record - or that instance is gone and they are sent back to the origin it recorded, rather
     * than waking up inside erased blocks (action plan 182, A3).
     */
    public static void handleDungeonReturnOnJoin(ServerPlayer player) {
        if (serverInstance == null) return;
        TimerSavedData savedData = TimerSavedData.get(serverInstance);
        TimerSavedData.ReturnPoint point = savedData.getPendingReturn(player.getUUID());
        if (point == null) {
            return;
        }

        TimerScope scope = currentScope(player);
        if (scope != null && scope.isSession() && isScopeLive(scope) && session(player, scope) != null) {
            savedData.clearPendingReturn(player.getUUID());
            return;
        }

        ServerLevel target = serverInstance.getLevel(point.dimension());
        if (target == null) {
            target = serverInstance.getLevel(Level.OVERWORLD);
        }
        if (target != null) {
            player.teleportTo(target,
                    point.pos().getX() + 0.5, point.pos().getY(), point.pos().getZ() + 0.5,
                    player.getYRot(), player.getXRot());
            player.resetFallDistance();
            player.setDeltaMovement(Vec3.ZERO);
            player.sendSystemMessage(
                    Component.translatable("cobblesafari.dungeon.returned_after_disconnect"));
            CobbleSafari.LOGGER.info("Returned {} to {} after their dungeon instance was cleared offline",
                    player.getName().getString(), point.pos());
        }
        savedData.clearPendingReturn(player.getUUID());
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Map<String, PlayerTimerData> playerTimers = activeTimers.remove(playerId);
        LAST_SCOPE_KEY.remove(playerId);

        if (playerTimers != null) {
            for (PlayerTimerData data : playerTimers.values()) {
                data.setActive(false);
                savePlayerData(player, data);
            }
        }
    }

    private static ServerPlayer getPlayerByUUID(UUID uuid) {
        if (serverInstance == null) return null;
        return serverInstance.getPlayerList().getPlayer(uuid);
    }
}
