package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player timer storage, keyed by {@link TimerScope} since action plan 181: {@code "dim"} for a
 * non-instanced dimension (an account) and {@code "dim@instanceId"} for an instanced one (a session).
 */
public class TimerSavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_timer_data";
    private static final String KEY_PLAYERS = "players";
    private static final String KEY_BYPASS = "Bypass";
    private static final String KEY_PENDING_RETURN = "PendingReturn";

    /** Keyed by {@link TimerScope#key()}, never by a bare dimension id on an instanced dimension. */
    private final Map<UUID, Map<String, PlayerTimerData>> playerData = new HashMap<>();

    /**
     * Timer bypass, hoisted off the Safari record (plan 181 § 3.3). It always was a player-level
     * flag - {@code TimerManager.shouldBypassTimer} read it from the Safari record whatever
     * dimension the player stood in - it was merely stored on a dimension by accident.
     */
    private final Map<UUID, Boolean> bypass = new HashMap<>();

    /**
     * Origins harvested from discarded legacy dungeon records, keyed {@code playerId + "|" + dim}.
     * <p>
     * Upgrade-day only, and deliberately <b>not</b> persisted. The § 10 migration throws the legacy
     * wallet away because it cannot say whether its credit was earned - but the origin it stored is
     * still a true fact about where that player came from. Keeping it for the single evacuation the
     * presence guard is about to perform is what sends the one surprised player per server back to
     * a sensible place instead of world spawn.
     */
    private final Map<String, PlayerTimerData> legacyOrigins = new HashMap<>();

    /**
     * Where to put a player back who logged out inside a dungeon (action plan 182, A3).
     * <p>
     * Persisted, because the instance can be cleared - and their session purged with it - while
     * they are offline, and the session is what carried their origin since plan 181 V6. Recording
     * it at disconnect makes the rule independent of ordering: whether the region dies now, later,
     * or never, the login path has the right answer.
     */
    private final Map<UUID, ReturnPoint> pendingReturns = new HashMap<>();

    /** Where a disconnected dungeon occupant should reappear. */
    public record ReturnPoint(BlockPos pos, ResourceKey<Level> dimension) {}

    private static String originKey(UUID playerId, String dimensionId) {
        return playerId + "|" + dimensionId;
    }

    public TimerSavedData() {
        // Required by the SavedData factory; state is populated in load().
    }

    public static TimerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TimerSavedData data = new TimerSavedData();

        int legacyDropped = 0;
        if (tag.contains(KEY_PLAYERS, Tag.TAG_LIST)) {
            ListTag playerList = tag.getList(KEY_PLAYERS, Tag.TAG_COMPOUND);
            for (int i = 0; i < playerList.size(); i++) {
                CompoundTag playerTag = playerList.getCompound(i);
                PlayerTimerData playerTimerData = PlayerTimerData.fromNbt(playerTag);
                UUID playerId = playerTimerData.getPlayerId();

                // Bypass is hoisted: harvest it from wherever it was stored before dropping the
                // record below. Legacy records carried it on the Safari entry, but harvesting from
                // every record costs nothing and cannot lose the flag.
                if (PlayerTimerData.readLegacyBypass(playerTag)) {
                    data.bypass.put(playerId, true);
                }

                // Plan 181 § 10: a record with no ScopeId on an instanced dimension is a pre-upgrade
                // dungeon wallet. It is DISCARDED, not resumed - it cannot say whether its credit was
                // ever earned, so a player who never saw a portal (credited to the full duration by
                // PlayerTimerData's constructor at first login) is indistinguishable from one
                // mid-run. Resuming would hand sessions to exactly the population S1 describes.
                if (!playerTimerData.isSession() && isInstancedDimension(playerTimerData.getDimensionId())) {
                    if (playerTimerData.getOriginPos() != null && playerTimerData.getOriginDimension() != null) {
                        data.legacyOrigins.put(
                                originKey(playerId, playerTimerData.getDimensionId()), playerTimerData);
                    }
                    legacyDropped++;
                    continue;
                }

                data.playerData
                        .computeIfAbsent(playerId, k -> new HashMap<>())
                        .put(playerTimerData.scope().key(), playerTimerData);
            }
        }

        if (tag.contains(KEY_PENDING_RETURN, Tag.TAG_COMPOUND)) {
            CompoundTag returns = tag.getCompound(KEY_PENDING_RETURN);
            for (String key : returns.getAllKeys()) {
                try {
                    CompoundTag entry = returns.getCompound(key);
                    data.pendingReturns.put(UUID.fromString(key), new ReturnPoint(
                            new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")),
                            ResourceKey.create(Registries.DIMENSION,
                                    ResourceLocation.parse(entry.getString("Dim")))));
                } catch (IllegalArgumentException e) {
                    CobbleSafari.LOGGER.warn("Skipping malformed pending dungeon return '{}'", key);
                }
            }
        }

        if (tag.contains(KEY_BYPASS, Tag.TAG_COMPOUND)) {
            CompoundTag bypassTag = tag.getCompound(KEY_BYPASS);
            for (String key : bypassTag.getAllKeys()) {
                try {
                    if (bypassTag.getBoolean(key)) {
                        data.bypass.put(UUID.fromString(key), true);
                    }
                } catch (IllegalArgumentException e) {
                    CobbleSafari.LOGGER.warn("Skipping malformed timer bypass entry '{}'", key);
                }
            }
        }

        int totalTimers = data.playerData.values().stream()
                .mapToInt(Map::size)
                .sum();
        CobbleSafari.LOGGER.info("Loaded timer data: {} players, {} timers, {} bypassed",
                data.playerData.size(), totalTimers, data.bypass.size());
        if (legacyDropped > 0) {
            CobbleSafari.LOGGER.info(
                    "Discarded {} legacy per-dimension dungeon timer record(s) (plan 181 migration); "
                            + "bypass flags were preserved", legacyDropped);
        }
        return data;
    }

    /**
     * Read through the config so an operator override is honoured, falling back to the registry.
     * Called during load, when the dungeon registry is already populated.
     */
    private static boolean isInstancedDimension(String dimensionId) {
        return SafariTimerConfig.getDimensionConfig(dimensionId)
                .map(maxigregrze.cobblesafari.config.DimensionTimerEntry::isInstanced)
                .orElseGet(() -> maxigregrze.cobblesafari.dungeon.DungeonDimensions
                        .isInstancedDungeonDimension(dimensionId));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag playerList = new ListTag();

        for (Map<String, PlayerTimerData> scopedTimers : playerData.values()) {
            for (PlayerTimerData timerData : scopedTimers.values()) {
                playerList.add(timerData.toNbt());
            }
        }

        tag.put(KEY_PLAYERS, playerList);

        CompoundTag bypassTag = new CompoundTag();
        bypass.forEach((playerId, flag) -> {
            if (Boolean.TRUE.equals(flag)) {
                bypassTag.putBoolean(playerId.toString(), true);
            }
        });
        tag.put(KEY_BYPASS, bypassTag);

        CompoundTag returnsTag = new CompoundTag();
        pendingReturns.forEach((playerId, point) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", point.pos().getX());
            entry.putInt("Y", point.pos().getY());
            entry.putInt("Z", point.pos().getZ());
            entry.putString("Dim", point.dimension().location().toString());
            returnsTag.put(playerId.toString(), entry);
        });
        tag.put(KEY_PENDING_RETURN, returnsTag);

        int totalTimers = playerData.values().stream()
                .mapToInt(Map::size)
                .sum();
        CobbleSafari.LOGGER.info("Saved timer data: {} players, {} timers", playerData.size(), totalTimers);
        return tag;
    }

    public PlayerTimerData getPlayerData(UUID playerId, TimerScope scope) {
        Map<String, PlayerTimerData> scopedTimers = playerData.get(playerId);
        if (scopedTimers == null) {
            return null;
        }
        return scopedTimers.get(scope.key());
    }

    /** @deprecated use {@link #getPlayerData(UUID, TimerScope)} with an explicit scope. */
    @Deprecated
    public PlayerTimerData getPlayerData(UUID playerId) {
        return getPlayerData(playerId, TimerScope.of(SafariTimerConfig.getSafariDimensionId()));
    }

    /**
     * Account lookup for a non-instanced dimension. A session is never created here - only
     * {@code TimerManager.openSession} may create one (plan 181, V2).
     */
    public PlayerTimerData getOrCreateAccount(UUID playerId, String dimensionId) {
        return playerData
                .computeIfAbsent(playerId, k -> new HashMap<>())
                .computeIfAbsent(dimensionId, d -> new PlayerTimerData(playerId, dimensionId));
    }

    /** Creates the session record if absent. Reserved for {@code TimerManager.openSession}. */
    public PlayerTimerData getOrCreateSession(UUID playerId, TimerScope scope) {
        return playerData
                .computeIfAbsent(playerId, k -> new HashMap<>())
                .computeIfAbsent(scope.key(), k -> PlayerTimerData.newSession(playerId, scope));
    }

    public void setPlayerData(UUID playerId, TimerScope scope, PlayerTimerData data) {
        playerData
                .computeIfAbsent(playerId, k -> new HashMap<>())
                .put(scope.key(), data);
        setDirty();
    }

    /** Writes a record under its own scope - the only correct key for it. */
    public void setPlayerData(UUID playerId, PlayerTimerData data) {
        setPlayerData(playerId, data.scope(), data);
    }

    /** Live view of a player's records, keyed by {@link TimerScope#key()}. */
    public Map<String, PlayerTimerData> getPlayerScopedTimers(UUID playerId) {
        return playerData.getOrDefault(playerId, Map.of());
    }

    /**
     * Removes one scope key from every player (instance purge, plan 181 V5). Mirrors
     * {@code ObjectivesSavedData.removeKeyForAll}.
     */
    public void removeKeyForAll(String scopeKey) {
        boolean changed = false;
        for (Map<String, PlayerTimerData> byKey : playerData.values()) {
            changed |= byKey.remove(scopeKey) != null;
        }
        playerData.values().removeIf(Map::isEmpty);
        if (changed) {
            setDirty();
        }
    }

    /**
     * Removes every session whose scope no longer names a live instance. Boot-time sweep covering a
     * crash between the region clear and the purge (plan 181 § 7). Returns how many were removed.
     */
    public int removeOrphanedSessions(java.util.function.Predicate<TimerScope> stillLive) {
        int removed = 0;
        for (Map<String, PlayerTimerData> byKey : playerData.values()) {
            var it = byKey.entrySet().iterator();
            while (it.hasNext()) {
                PlayerTimerData data = it.next().getValue();
                if (data.isSession() && !stillLive.test(data.scope())) {
                    it.remove();
                    removed++;
                }
            }
        }
        playerData.values().removeIf(Map::isEmpty);
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    /**
     * Consumes the origin of a discarded legacy record, if this player had one for this dimension.
     * One-shot: the record is removed as it is handed out, so it can only steer the single
     * evacuation it exists for.
     */
    public PlayerTimerData takeLegacyOrigin(UUID playerId, String dimensionId) {
        return legacyOrigins.remove(originKey(playerId, dimensionId));
    }

    public void setPendingReturn(UUID playerId, BlockPos pos, ResourceKey<Level> dimension) {
        pendingReturns.put(playerId, new ReturnPoint(pos, dimension));
        setDirty();
    }

    public ReturnPoint getPendingReturn(UUID playerId) {
        return pendingReturns.get(playerId);
    }

    public void clearPendingReturn(UUID playerId) {
        if (pendingReturns.remove(playerId) != null) {
            setDirty();
        }
    }

    public boolean isBypassed(UUID playerId) {
        return Boolean.TRUE.equals(bypass.get(playerId));
    }

    public void setBypassed(UUID playerId, boolean bypassed) {
        if (bypassed) {
            bypass.put(playerId, true);
        } else {
            bypass.remove(playerId);
        }
        setDirty();
    }

    /**
     * Daily reset of a non-instanced dimension's accounts. Sessions are never reset this way: they
     * are per visit and die with their instance.
     */
    public void resetDimensionTimers(String dimensionId) {
        String key = TimerScope.of(dimensionId).key();
        for (Map<String, PlayerTimerData> scopedTimers : playerData.values()) {
            PlayerTimerData data = scopedTimers.get(key);
            if (data != null) {
                data.resetForDailyCycle();
                data.setLastSafariBallGrantDay(0);
                data.resetEntryFeePayDay();
                data.setActive(false);
            }
        }
        setDirty();
    }

    public Map<UUID, Map<String, PlayerTimerData>> getAllPlayerData() {
        return playerData;
    }

    public static TimerSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(createFactory(), DATA_NAME);
    }

    private static Factory<TimerSavedData> createFactory() {
        return new Factory<>(TimerSavedData::new, TimerSavedData::load, null);
    }
}
