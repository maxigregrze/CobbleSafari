package maxigregrze.cobblesafari.block.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensionEntry;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.PortalSpawnConfig;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.network.OpenPortalSpawnerConfigPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * State and scheduler of a Portal Spawner (action plan 188, section 12).
 *
 * <p>The schedule is anchored on game time, not on this block entity's own ticks: a spawner whose
 * chunk was unloaded replays the cycles it missed on its first tick back and spawns the current
 * cycle's portal back-dated, so players find the state they would have seen had the chunk stayed
 * loaded. The generated portal is an ordinary {@code dungeon_portal} carrying its own lifetime, so
 * expiry, instance detachment and the Jade/WTHIT tooltips all go through the existing paths.
 */
public class PortalSpawnerBlockEntity extends BlockEntity {

    public static final int MIN_LIFESPAN_SECONDS = 10;
    public static final int MAX_SECONDS = 86_400;
    public static final int MIN_SIGNAL = 1;
    public static final int MAX_SIGNAL = 15;

    private static final int DEFAULT_LIFESPAN_SECONDS = 1800;
    private static final int DEFAULT_INTERVAL_SECONDS = 300;
    private static final int DEFAULT_MIN_INTERVAL_SECONDS = 300;
    private static final int DEFAULT_MAX_INTERVAL_SECONDS = 900;

    private static final long UNSET = Long.MIN_VALUE;
    private static final int CHECK_INTERVAL_TICKS = 20;
    /** Bound on the catch-up loop; past it the schedule simply resynchronises on "now". */
    private static final int MAX_CATCH_UP_CYCLES = 10_000;
    private static final long CYCLE_SEED_MIX = 0x9E3779B97F4A7C15L;

    private static final String KEY_ENABLED = "Enabled";
    private static final String KEY_DESTINATION = "Destination";
    private static final String KEY_LIFESPAN = "LifespanSeconds";
    private static final String KEY_MODE = "Mode";
    private static final String KEY_INTERVAL = "IntervalSeconds";
    private static final String KEY_MIN_INTERVAL = "MinIntervalSeconds";
    private static final String KEY_MAX_INTERVAL = "MaxIntervalSeconds";
    private static final String KEY_MIN_SIGNAL = "MinSignal";
    private static final String KEY_PORTAL_ID = "PortalId";
    private static final String KEY_NEXT_SPAWN = "NextSpawnTick";
    private static final String KEY_CYCLE = "CycleIndex";
    private static final String KEY_LAST_POWERED = "LastPowered";
    private static final String KEY_PENDING_PULSE = "PendingPulse";

    /** The settings edited in the GUI, already clamped by {@link #applySettings}. */
    public record Settings(boolean enabled, String destination, int lifespanSeconds, PortalSpawnerMode mode,
                           int intervalSeconds, int minIntervalSeconds, int maxIntervalSeconds, int minSignal) {
    }

    // -- settings
    private boolean enabled = false;
    /** {@code ""} = weighted random pool. */
    private String destination = "";
    private int lifespanSeconds = DEFAULT_LIFESPAN_SECONDS;
    private PortalSpawnerMode mode = PortalSpawnerMode.INTERVAL;
    private int intervalSeconds = DEFAULT_INTERVAL_SECONDS;
    private int minIntervalSeconds = DEFAULT_MIN_INTERVAL_SECONDS;
    private int maxIntervalSeconds = DEFAULT_MAX_INTERVAL_SECONDS;
    private int minSignal = MIN_SIGNAL;

    // -- runtime (persisted)
    @Nullable
    private UUID portalId;
    private long nextSpawnTick = UNSET;
    private long cycleIndex = 0;
    private boolean lastPowered = false;
    private boolean pendingPulse = false;

    // -- runtime (transient)
    private PortalSpawnerStatus status = PortalSpawnerStatus.DISABLED;
    /** Game tick the current status ends at (portal close / next spawn); only meaningful with a countdown. */
    private long statusEndTick = 0;
    /** False until the first check after load, which runs immediately instead of on the 20-tick cadence. */
    private boolean checkedSinceLoad = false;
    private boolean warnedBadDestination = false;

    public PortalSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORTAL_SPAWNER, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PortalSpawnerBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long now = level.getGameTime();
        if (be.checkedSinceLoad && Math.floorMod(now + pos.hashCode(), CHECK_INTERVAL_TICKS) != 0) {
            return;
        }
        be.checkedSinceLoad = true;
        be.checkSchedule(serverLevel, now);
    }

    // ------------------------------------------------------------------ schedule

    private void checkSchedule(ServerLevel level, long now) {
        if (!enabled) {
            status = PortalSpawnerStatus.DISABLED;
            return;
        }
        DungeonPortalBlockEntity alive = trackedPortal(level);
        if (alive != null) {
            long closesAt = alive.getSpawnTick() + alive.getEffectiveLifetimeTicks();
            if (nextSpawnTick == UNSET && mode != PortalSpawnerMode.REDSTONE) {
                // Settings changed while this portal was open: the new calendar starts at its close.
                nextSpawnTick = closesAt + rollCooldownTicks(cycleIndex++);
                setChanged();
            }
            setStatus(PortalSpawnerStatus.OPEN, closesAt);
            return;
        }
        closeStalePortal(level);

        if (mode == PortalSpawnerMode.REDSTONE) {
            tickRedstone(level, now);
            return;
        }

        long before = nextSpawnTick;
        if (nextSpawnTick == UNSET) {
            nextSpawnTick = now;
        }
        int guard = 0;
        while (nextSpawnTick + lifespanTicks() <= now) {
            // Whole cycles that ended while the chunk was unloaded.
            if (++guard > MAX_CATCH_UP_CYCLES) {
                nextSpawnTick = now;
                break;
            }
            nextSpawnTick += lifespanTicks() + rollCooldownTicks(cycleIndex++);
        }
        if (nextSpawnTick != before) {
            setChanged();
        }
        if (now < nextSpawnTick) {
            setStatus(PortalSpawnerStatus.COOLDOWN, nextSpawnTick);
            return;
        }
        long spawnAt = nextSpawnTick;
        if (trySpawn(level, spawnAt)) {
            nextSpawnTick = spawnAt + lifespanTicks() + rollCooldownTicks(cycleIndex++);
            setChanged();
            setStatus(PortalSpawnerStatus.OPEN, spawnAt + lifespanTicks());
        } else {
            setStatus(PortalSpawnerStatus.BLOCKED, 0);
        }
    }

    private void tickRedstone(ServerLevel level, long now) {
        if (!pendingPulse) {
            setStatus(PortalSpawnerStatus.WAITING_SIGNAL, 0);
            return;
        }
        if (trySpawn(level, now)) {
            pendingPulse = false;
            setChanged();
            setStatus(PortalSpawnerStatus.OPEN, now + lifespanTicks());
        } else {
            setStatus(PortalSpawnerStatus.BLOCKED, 0);
        }
    }

    /**
     * Called by the block on neighbour updates: a rising edge to {@code minSignal} queues one portal.
     * Only the edge itself triggers an immediate check - placing or removing our own portal also
     * fires neighbour updates, and those must not re-enter the schedule mid-spawn. A queued pulse
     * that found the spot occupied is retried by the regular tick while the signal holds.
     */
    public void onSignal(ServerLevel level, int signal) {
        boolean powered = signal >= minSignal;
        boolean rising = powered && !lastPowered;
        boolean changed = powered != lastPowered;
        if (rising) {
            pendingPulse = true;
        } else if (!powered) {
            // A pulse still waiting for a free spot is dropped once the signal falls.
            changed |= pendingPulse;
            pendingPulse = false;
        }
        lastPowered = powered;
        if (changed) {
            setChanged();
        }
        if (rising && enabled && mode == PortalSpawnerMode.REDSTONE) {
            checkSchedule(level, level.getGameTime());
        }
    }

    /** Seeds the redstone memory on placement so an already-powered spot does not count as an edge. */
    public void initSignal(int signal) {
        this.lastPowered = signal >= minSignal;
        setChanged();
    }

    private boolean trySpawn(ServerLevel level, long spawnTick) {
        BlockPos target = worldPosition.above();
        if (!level.getBlockState(target).canBeReplaced()) {
            return false;
        }
        UUID id = PortalSpawnManager.spawnManagedPortal(level, target,
                getBlockState().getValue(PortalSpawnerBlock.FACING), resolveDestination(),
                spawnTick, lifespanTicks(), true);
        if (id == null) {
            return false;
        }
        portalId = id;
        setChanged();
        return true;
    }

    /** The configured destination, or {@code null} (weighted random) when it is random or unusable. */
    @Nullable
    private DungeonConfig resolveDestination() {
        if (destination.isEmpty()) {
            return null;
        }
        DungeonConfig config = DungeonDimensions.getDungeonById(destination);
        boolean enabledInPool = config != null && PortalSpawnConfig.getDimensionConfig(config.getId())
                .map(DungeonDimensionEntry::isEnabled)
                .orElse(true);
        if (config == null || config.isSubDungeon() || !enabledInPool) {
            if (!warnedBadDestination) {
                warnedBadDestination = true;
                CobbleSafari.LOGGER.warn("Portal Spawner at {} targets unusable dungeon '{}', using a random one",
                        worldPosition, destination);
            }
            return null;
        }
        return config;
    }

    /** Our portal above, if it is still standing and still has lifetime left. */
    @Nullable
    private DungeonPortalBlockEntity trackedPortal(Level level) {
        if (portalId != null
                && level.getBlockEntity(worldPosition.above()) instanceof DungeonPortalBlockEntity portal
                && portalId.equals(portal.getPortalId())
                && portal.getRemainingLifetimeTicks() > 0) {
            return portal;
        }
        return null;
    }

    /** Our portal block left standing past its lifetime (unloaded chunk): close it the natural way. */
    private void closeStalePortal(ServerLevel level) {
        if (portalId == null) {
            return;
        }
        closeTrackedPortal(level);
    }

    /** Natural close of our portal, if it is the one above: its instance is detached, nobody is evicted. */
    private void closeTrackedPortal(ServerLevel level) {
        if (portalId == null) {
            return;
        }
        BlockPos above = worldPosition.above();
        if (level.getBlockEntity(above) instanceof DungeonPortalBlockEntity portal
                && portalId.equals(portal.getPortalId())) {
            PortalSpawnManager.closePortalNaturally(level, above);
        }
        portalId = null;
        setChanged();
    }

    /** Spawner broken: its portal closes with it (natural close). */
    public void onSpawnerRemoved(ServerLevel level) {
        closeTrackedPortal(level);
    }

    private int rollCooldownTicks(long cycle) {
        if (mode != PortalSpawnerMode.RANDOM_INTERVAL) {
            return intervalSeconds * 20;
        }
        int span = maxIntervalSeconds - minIntervalSeconds;
        if (span <= 0) {
            return minIntervalSeconds * 20;
        }
        // Seeded per cycle: the catch-up replays exactly the delays that would have been rolled live.
        RandomSource rng = RandomSource.create(worldPosition.asLong() ^ (cycle * CYCLE_SEED_MIX));
        return (minIntervalSeconds + rng.nextInt(span + 1)) * 20;
    }

    private int lifespanTicks() {
        return lifespanSeconds * 20;
    }

    private void setStatus(PortalSpawnerStatus status, long endTick) {
        this.status = status;
        this.statusEndTick = endTick;
    }

    // ------------------------------------------------------------------ settings / GUI

    /**
     * Applies settings saved from the GUI. Values are clamped here, the authority. Disabling closes the
     * open portal (natural close); a change of mode or timing restarts the calendar, the open portal
     * keeping its own destination and lifetime.
     */
    public void applySettings(ServerLevel level, Settings requested) {
        int newLifespan = clampSeconds(requested.lifespanSeconds(), MIN_LIFESPAN_SECONDS);
        int newInterval = clampSeconds(requested.intervalSeconds(), 0);
        int newMin = clampSeconds(requested.minIntervalSeconds(), 0);
        int newMax = clampSeconds(requested.maxIntervalSeconds(), 0);
        if (newMin > newMax) {
            int swap = newMin;
            newMin = newMax;
            newMax = swap;
        }
        int newSignal = Math.clamp(requested.minSignal(), MIN_SIGNAL, MAX_SIGNAL);
        PortalSpawnerMode newMode = requested.mode() == null ? PortalSpawnerMode.INTERVAL : requested.mode();

        boolean wasEnabled = this.enabled;
        boolean timingChanged = newMode != mode || newLifespan != lifespanSeconds || newInterval != intervalSeconds
                || newMin != minIntervalSeconds || newMax != maxIntervalSeconds;

        this.enabled = requested.enabled();
        this.destination = requested.destination() == null ? "" : requested.destination();
        this.lifespanSeconds = newLifespan;
        this.mode = newMode;
        this.intervalSeconds = newInterval;
        this.minIntervalSeconds = newMin;
        this.maxIntervalSeconds = newMax;
        this.minSignal = newSignal;
        this.warnedBadDestination = false;

        if (!enabled) {
            closeTrackedPortal(level);
            nextSpawnTick = UNSET;
            pendingPulse = false;
        } else if (!wasEnabled || timingChanged) {
            nextSpawnTick = UNSET;
            pendingPulse = false;
            if (mode == PortalSpawnerMode.REDSTONE) {
                // Re-read the signal so an already-powered spawner fires once when (re)armed.
                lastPowered = false;
                onSignal(level, level.getBestNeighborSignal(worldPosition));
            }
        }
        setChanged();
        checkSchedule(level, level.getGameTime());
    }

    /** The configured destination id; {@code ""} = weighted random pool. */
    public String getDestination() {
        return destination;
    }

    private static int clampSeconds(int value, int min) {
        return Math.clamp(value, min, MAX_SECONDS);
    }

    /** Snapshot for the config GUI; refreshes the status first so the countdown is current. */
    public OpenPortalSpawnerConfigPayload createOpenPayload(ServerLevel level) {
        long now = level.getGameTime();
        checkSchedule(level, now);
        List<String> destinations = new ArrayList<>(PortalSpawnManager.getEnabledDungeonIdsForCycle());
        if (!destination.isEmpty() && !destinations.contains(destination)) {
            // Keep an unlisted fixed destination selectable so saving never silently changes it.
            destinations.add(destination);
        }
        long remaining = status.hasCountdown() ? Math.max(0, statusEndTick - now) : 0;
        return new OpenPortalSpawnerConfigPayload(worldPosition, enabled, destination, lifespanSeconds,
                mode.ordinal(), intervalSeconds, minIntervalSeconds, maxIntervalSeconds, minSignal,
                destinations, status.ordinal(), remaining);
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(KEY_ENABLED, enabled);
        tag.putString(KEY_DESTINATION, destination);
        tag.putInt(KEY_LIFESPAN, lifespanSeconds);
        tag.putInt(KEY_MODE, mode.ordinal());
        tag.putInt(KEY_INTERVAL, intervalSeconds);
        tag.putInt(KEY_MIN_INTERVAL, minIntervalSeconds);
        tag.putInt(KEY_MAX_INTERVAL, maxIntervalSeconds);
        tag.putInt(KEY_MIN_SIGNAL, minSignal);
        if (portalId != null) {
            tag.putUUID(KEY_PORTAL_ID, portalId);
        }
        tag.putLong(KEY_NEXT_SPAWN, nextSpawnTick);
        tag.putLong(KEY_CYCLE, cycleIndex);
        tag.putBoolean(KEY_LAST_POWERED, lastPowered);
        tag.putBoolean(KEY_PENDING_PULSE, pendingPulse);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        enabled = tag.getBoolean(KEY_ENABLED);
        destination = tag.getString(KEY_DESTINATION);
        lifespanSeconds = tag.contains(KEY_LIFESPAN)
                ? clampSeconds(tag.getInt(KEY_LIFESPAN), MIN_LIFESPAN_SECONDS) : DEFAULT_LIFESPAN_SECONDS;
        mode = PortalSpawnerMode.byOrdinal(tag.getInt(KEY_MODE));
        intervalSeconds = tag.contains(KEY_INTERVAL)
                ? clampSeconds(tag.getInt(KEY_INTERVAL), 0) : DEFAULT_INTERVAL_SECONDS;
        minIntervalSeconds = tag.contains(KEY_MIN_INTERVAL)
                ? clampSeconds(tag.getInt(KEY_MIN_INTERVAL), 0) : DEFAULT_MIN_INTERVAL_SECONDS;
        maxIntervalSeconds = tag.contains(KEY_MAX_INTERVAL)
                ? clampSeconds(tag.getInt(KEY_MAX_INTERVAL), 0) : DEFAULT_MAX_INTERVAL_SECONDS;
        minSignal = tag.contains(KEY_MIN_SIGNAL)
                ? Math.clamp(tag.getInt(KEY_MIN_SIGNAL), MIN_SIGNAL, MAX_SIGNAL) : MIN_SIGNAL;
        portalId = tag.hasUUID(KEY_PORTAL_ID) ? tag.getUUID(KEY_PORTAL_ID) : null;
        nextSpawnTick = tag.contains(KEY_NEXT_SPAWN) ? tag.getLong(KEY_NEXT_SPAWN) : UNSET;
        cycleIndex = tag.getLong(KEY_CYCLE);
        lastPowered = tag.getBoolean(KEY_LAST_POWERED);
        pendingPulse = tag.getBoolean(KEY_PENDING_PULSE);
        checkedSinceLoad = false;
    }
}
