package maxigregrze.cobblesafari.warpride;

import maxigregrze.cobblesafari.config.UltraWarpRideConfig;
import maxigregrze.cobblesafari.warpride.logic.Wave;
import maxigregrze.cobblesafari.warpride.logic.WaveGenerator;
import maxigregrze.cobblesafari.warpride.logic.WaveObject;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * One rider's ride. <b>Server-only</b>: distance, type and tier are computed here from this
 * object's own odometer and RNG, and no client packet has ever carried any of them.
 */
public final class WarpRideSession {

    /** Enough slots to look back over the whole lag budget with room to spare. */
    private static final int LANE_HISTORY_SIZE = 16;


    private final UUID sessionId;
    private final UUID ownerId;
    private final RandomSource random;
    private final UltraWarpRideConfig cfg;
    private final WaveGenerator generator;

    private WarpRidePhase phase = WarpRidePhase.AWAITING_READY;

    private double odometer;
    private double energy;
    /** Energy cannot exceed this; it only rises, when a bonus overflows it (plan 191, section 3.1). */
    private double energyCeiling;

    /**
     * Parent-dungeon credit remaining, in ticks, pushed in from outside every tick. A plain int is
     * the whole of the coupling: the simulation stays ignorant of the dungeon system, and the seam
     * lives in {@code WarpRideManager}, which may read {@code TimerManager} (plan 183, 8.2 bis).
     * {@link #NO_PARENT} for a parentless (debug) ride, which never panics.
     */
    private int parentRemainingTicks = NO_PARENT;
    private int panicTriggerTicks;
    private double panicDecayPerSecond;
    private boolean panicking;
    private double speed;
    private int lane;
    private boolean drifting;

    private final int[] laneHistory = new int[LANE_HISTORY_SIZE];

    private long introStartTick;
    private boolean launchAnnounced;
    private long inputCooldownUntil;
    private long decayFrozenUntil;
    private long lastActivityTick;

    private final List<Wave> waves = new ArrayList<>();
    private int nextWaveIndex;
    private int wavesSinceLastPortal = Integer.MAX_VALUE;
    private boolean forcePortalNext;

    /** No wave carries objects until its track position reaches here. */
    private final double noObjectBeforeTrackPos;

    /** Wave index of the portal currently parked at the plane, or -1 when none is held. */
    private int heldPortalWaveIndex = -1;
    private int heldPortalRank;
    private int heldPortalTypeId;
    private long portalHoldEndTick;
    /** Highest portal wave index already put through a hold, so one is never held twice. */
    private int lastPortalWaveIndex = -1;
    /** Set by a jump onto a hovering portal; consumed in {@link #updatePortalHold(long)}. */
    private boolean pendingPortalTake;

    /** Tick at which the drifting rider is 3/4 of the way to the axis; portals are only taken then. */
    private long driftThreeQuarterTick;
    /** The drift guarantee portal is planted once, at the 3/4 mark. */
    private boolean forcedDriftPortal;

    /** Waves generated this tick, drained by the manager and pushed to the client. */
    private final List<Wave> pendingWaves = new ArrayList<>();

    /** Sentinel: this ride is not nested in a dungeon run, so no parent clock applies. */
    public static final int NO_PARENT = -1;

    /**
     * One impact resolved this tick; {@code energyAfter} is the authoritative energy after it, and
     * {@code ceilingRaised} marks a bonus that pushed the energy ceiling up.
     */
    public record ImpactEvent(int waveIndex, int lane, boolean positive, double energyAfter,
                              boolean ceilingRaised) {}

    private final List<ImpactEvent> pendingImpacts = new ArrayList<>();


    private final Vec3 anchor;
    private boolean bodyRestored;

    /** The rider wore the full Ultra Recon set when the ride opened (action plan 200, section 6). */
    private final boolean reconBoost;
    /** The tutorial slideshow was shown, so the ready that ends it marks it as seen. */
    private final boolean tutorialShown;

    private int outcomeRank = -1;
    private int outcomeTypeId = -1;
    private int outcomeDistance = -1;

    @SuppressWarnings("java:S107")
    public WarpRideSession(UUID sessionId, UUID ownerId, long seed, UltraWarpRideConfig cfg,
                           Vec3 anchor, long nowTick, boolean reconBoost, boolean tutorialShown) {
        this.sessionId = sessionId;
        this.ownerId = ownerId;
        this.random = RandomSource.create(seed);
        this.cfg = cfg;
        this.generator = new WaveGenerator(random, cfg);
        this.anchor = anchor;
        this.reconBoost = reconBoost;
        this.tutorialShown = tutorialShown;
        this.energy = cfg.startEnergy;
        this.energyCeiling = Math.max(cfg.startEnergy, cfg.startCeiling);
        this.speed = speedFromEnergy(cfg.startEnergy);
        this.lane = 0;
        this.lastActivityTick = nowTick;
        java.util.Arrays.fill(laneHistory, 0);

        // Deterministic: the odometer holds at 0 through the intro, and the start speed is constant
        // through the opening grace, so this many light-years cover its first seconds.
        double startSpeed = speedFromEnergy(cfg.startEnergy);
        this.noObjectBeforeTrackPos = startSpeed * cfg.startGraceSeconds;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public WarpRidePhase getPhase() {
        return phase;
    }

    public double getOdometer() {
        return odometer;
    }

    public double getEnergy() {
        return energy;
    }

    public double getSpeed() {
        return speed;
    }

    public int getLane() {
        return lane;
    }

    public boolean isDrifting() {
        return drifting;
    }

    /** Whether energy decay is currently frozen (the opening grace or a post-hit freeze). */
    public boolean isFrozen(long now) {
        return phase == WarpRidePhase.RIDING && now < decayFrozenUntil;
    }

    public Vec3 getAnchor() {
        return anchor;
    }

    public long getLastActivityTick() {
        return lastActivityTick;
    }

    public boolean isBodyRestored() {
        return bodyRestored;
    }

    public void markBodyRestored() {
        this.bodyRestored = true;
    }

    public boolean hasReconBoost() {
        return reconBoost;
    }

    public boolean isTutorialShown() {
        return tutorialShown;
    }

    public int getOutcomeRank() {
        return outcomeRank;
    }

    public int getOutcomeTypeId() {
        return outcomeTypeId;
    }

    public int getOutcomeDistance() {
        return outcomeDistance;
    }

    /**
     * Feeds the parent clock in. Returns true on the tick the panic decay first fires, so the
     * caller can send the one warning the rider must not miss - an energy bar draining ten times
     * faster with no explanation reads as a bug.
     */
    public boolean setParentRemainingTicks(int remaining, int triggerTicks, double decayPerSecond) {
        this.parentRemainingTicks = remaining;
        this.panicTriggerTicks = triggerTicks;
        this.panicDecayPerSecond = decayPerSecond;
        if (remaining == NO_PARENT || panicking) {
            return false;
        }
        if (remaining <= triggerTicks) {
            panicking = true;
            return true;
        }
        return false;
    }

    public int getParentRemainingTicks() {
        return parentRemainingTicks;
    }

    public boolean isPanicking() {
        return panicking;
    }

    public int cooldownTicksLeft(long now) {
        return (int) Math.max(0L, inputCooldownUntil - now);
    }

    public List<ImpactEvent> drainPendingImpacts() {
        if (pendingImpacts.isEmpty()) {
            return List.of();
        }
        List<ImpactEvent> out = List.copyOf(pendingImpacts);
        pendingImpacts.clear();
        return out;
    }

    /** @return true exactly once, on the intro tick that launches the rider (sound, aura burst) */
    public boolean pollLaunch(long now) {
        if (phase != WarpRidePhase.INTRO || launchAnnounced || now - introStartTick < cfg.introLaunchTick) {
            return false;
        }
        launchAnnounced = true;
        return true;
    }

    public List<Wave> drainPendingWaves() {
        if (pendingWaves.isEmpty()) {
            return List.of();
        }
        List<Wave> out = List.copyOf(pendingWaves);
        pendingWaves.clear();
        return out;
    }

    /** The tutorial was dismissed. Accepted once, and only from {@code AWAITING_READY}. */
    public boolean onReady(long now) {
        if (phase != WarpRidePhase.AWAITING_READY) {
            return false;
        }
        phase = WarpRidePhase.INTRO;
        introStartTick = now;
        lastActivityTick = now;
        // Seed the track ahead, so the first riding tick already has its waves on screen.
        ensureWaveLead();
        return true;
    }

    public void markEnded() {
        this.phase = WarpRidePhase.ENDED;
    }

    /** Applies a steering intent, backdated only inside the lag budget; true if anything changed. */
    public boolean onInput(int input, long clientTick, long now) {
        lastActivityTick = now;
        if (phase != WarpRidePhase.RIDING || drifting) {
            return false;
        }

        // A jump onto a parked portal ignores the steering cooldown; the take lands in updatePortalHold().
        if (input == 2 && heldPortalWaveIndex != -1) {
            pendingPortalTake = true;
            return true;
        }

        long effective = now;
        long delta = now - clientTick;
        if (delta >= 0 && delta <= cfg.lagBudgetTicks) {
            effective = clientTick;
        }

        if (effective < inputCooldownUntil) {
            return false;
        }

        int base = laneAt(effective);
        int newLane;
        switch (input) {
            case 0 -> newLane = Math.floorMod(base + 1, WaveGenerator.LANE_COUNT);   // keyLeft
            case 1 -> newLane = Math.floorMod(base - 1, WaveGenerator.LANE_COUNT);   // keyRight
            case 2 -> newLane = base ^ 4;                                            // keyUp
            default -> {
                return false;
            }
        }

        // A turn is nearly free; a jump costs its transit. Both stretch at low energy (section 3.3).
        inputCooldownUntil = effective
                + (input == 2 ? cfg.jumpTransitTicks(energy) : cfg.inputCooldownTicks(energy));
        this.lane = newLane;
        rewriteLaneHistory(effective, now, newLane);
        return true;
    }

    private int laneAt(long tick) {
        return laneHistory[(int) Math.floorMod(tick, LANE_HISTORY_SIZE)];
    }

    private void rewriteLaneHistory(long from, long to, int value) {
        for (long t = from; t <= to; t++) {
            laneHistory[(int) Math.floorMod(t, LANE_HISTORY_SIZE)] = value;
        }
    }

    public void tick(long now) {
        switch (phase) {
            case INTRO -> {
                // The odometer holds at 0 and nothing decays: the ride's distance starts at launch.
                lastActivityTick = now;
                if (now - introStartTick >= cfg.introTicks) {
                    phase = WarpRidePhase.RIDING;
                    decayFrozenUntil = now + cfg.openingGraceTicks();
                    lastActivityTick = now;
                }
            }
            case RIDING -> {
                // A rider holding a lane sends no input for minutes; the watchdog is for a stalled ride.
                lastActivityTick = now;
                tickRiding(now);
            }
            default -> {
                // AWAITING_READY, PORTAL_TAKEN and ENDED do not advance the simulation.
            }
        }
    }

    private void tickRiding(long now) {
        laneHistory[(int) Math.floorMod(now, LANE_HISTORY_SIZE)] = lane;

        if (now >= decayFrozenUntil) {
            // The panic decay ends the ride with the game's own machinery: a bigger number in the
            // one subtraction that was already running. From any energy at all, 10/s empties the
            // bar within 10 s, the rider drifts, the drift guarantee plants a portal, and the ride
            // finishes the way every ride finishes. No planted portal, no forced take, no new state.
            double perSecond = panicking ? panicDecayPerSecond : cfg.decayPerSecond;
            energy = Math.max(0.0, energy - perSecond / 20.0);
        }

        // As in the source, speed follows energy and nothing else: no stall, no ramp.
        speed = speedFromEnergy(energy);
        odometer += speed / 20.0;

        ensureWaveLead();
        markCrossings(now);
        updatePortalHold(now);
        if (phase != WarpRidePhase.RIDING) {
            return;   // a portal was taken this tick
        }
        maybeForceDriftPortal(now);
        resolveDueImpacts(now);
        pruneWaves();

        if (!drifting && energy <= 0.0) {
            enterDrift(now);
        }
    }

    private double speedFromEnergy(double e) {
        return cfg.speedFromEnergy(e);
    }

    /** Keeps at least the visible set plus two of lead, so nothing ever pops in at ring 1. */
    private void ensureWaveLead() {
        double horizon = odometer + cfg.spawnDepth() + 2.0 * cfg.waveSpacingLy;
        int guard = 0;
        while (nextWaveIndex * cfg.waveSpacingLy <= horizon && guard < 32) {
            guard++;
            double trackPos = nextWaveIndex * cfg.waveSpacingLy;
            Wave wave;
            if (trackPos < noObjectBeforeTrackPos) {
                // Opening grace: the track scrolls but this wave carries nothing, so no ride starts on a malus.
                wave = new Wave(nextWaveIndex, trackPos, List.of());
            } else {
                wave = generator.generate(nextWaveIndex, odometer, wavesSinceLastPortal, forcePortalNext);
            }
            if (wave.hasPortal()) {
                wavesSinceLastPortal = 0;
                forcePortalNext = false;
            } else {
                wavesSinceLastPortal = wavesSinceLastPortal == Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : wavesSinceLastPortal + 1;
            }
            waves.add(wave);
            pendingWaves.add(wave);
            nextWaveIndex++;
        }
    }

    private void markCrossings(long now) {
        for (Wave wave : waves) {
            if (!wave.hasCrossed() && wave.depth(odometer) <= 0.0) {
                wave.markCrossed(now);
            }
        }
    }

    /** The server waits rather than rewinding: resolved late, against the lane history at crossing. */
    private void resolveDueImpacts(long now) {
        for (Wave wave : waves) {
            if (wave.isResolved() || !wave.hasCrossed()) {
                continue;
            }
            if (now < wave.crossTick() + cfg.lagBudgetTicks) {
                continue;
            }
            wave.markResolved();
            resolveWave(wave, now);
            if (phase != WarpRidePhase.RIDING) {
                return;
            }
        }
    }

    private void resolveWave(Wave wave, long now) {
        long cross = wave.crossTick();

        for (WaveObject object : wave.objects()) {
            if (object.isPortal()) {
                // Portals are not resolved here; they park at the plane, handled in updatePortalHold().
                continue;
            }
            if (drifting) {
                // On the axis: lane objects can no longer be hit, positives or negatives alike.
                continue;
            }
            if (object.kind() == WaveObject.Kind.NEGATIVE) {
                if (laneAt(cross) == object.lane()) {
                    applyNegative(now, wave.index(), object.lane());
                }
            } else if (matchedWithinTolerance(cross, object.lane())) {
                applyPositive(now, wave.index(), object.lane());
            }
        }
    }

    /** Positives match anywhere in the tolerance window; negatives use the exact crossing tick. */
    private boolean matchedWithinTolerance(long cross, int lane) {
        long tolerance = cfg.positiveToleranceTicks();
        for (long t = cross - tolerance; t <= cross + tolerance; t++) {
            if (laneAt(t) == lane) {
                return true;
            }
        }
        return false;
    }

    /** A portal parks at the plane for {@code portalHoldWaves}, then is taken, or released and faded. */
    private void updatePortalHold(long now) {
        // While drifting a portal is only "hit" once the rider is 3/4 of the way to the axis.
        boolean driftTake = drifting && now >= driftThreeQuarterTick;
        if (heldPortalWaveIndex != -1) {
            if (driftTake || pendingPortalTake) {
                takePortalOutcome(heldPortalRank, heldPortalTypeId, now);
                return;
            }
            if (now >= portalHoldEndTick) {
                heldPortalWaveIndex = -1;   // released untaken; the fade is presentation-only
            }
            return;
        }
        for (Wave wave : waves) {
            if (wave.index() <= lastPortalWaveIndex || !wave.hasCrossed() || !wave.hasPortal()) {
                continue;
            }
            WaveObject portal = wave.portal();
            heldPortalWaveIndex = wave.index();
            heldPortalRank = portal.rank();
            heldPortalTypeId = portal.typeId();
            lastPortalWaveIndex = wave.index();
            portalHoldEndTick = wave.crossTick() + portalHoldTicks();
            if (driftTake || pendingPortalTake) {
                takePortalOutcome(heldPortalRank, heldPortalTypeId, now);
            }
            return;
        }
    }

    /** At the 3/4-of-drift mark, plants the guarantee portal if none is held or in flight. */
    private void maybeForceDriftPortal(long now) {
        if (!drifting || forcedDriftPortal || now < driftThreeQuarterTick) {
            return;
        }
        forcedDriftPortal = true;
        if (heldPortalWaveIndex == -1 && !portalInFlight()) {
            forcePortalNext = true;
        }
    }

    /** The portal-hold worth of real time at the current speed: shorter when fast, longer when slow. */
    private long portalHoldTicks() {
        double secondsPerWave = cfg.waveSpacingLy / Math.max(1.0, speed);
        return Math.max(1L, Math.round(cfg.portalHoldWaves * secondsPerWave * 20.0));
    }

    /**
     * A bonus that would overflow the ceiling raises the ceiling by one step instead, so climbing
     * past the start ceiling is slow, and a long run can still reach the top (section 3.1).
     */
    private void applyPositive(long now, int waveIndex, int lane) {
        energy += cfg.bonusEnergy * (reconBoost ? cfg.reconBonusFactor : 1.0);
        boolean ceilingRaised = false;
        if (energy > energyCeiling) {
            double raised = Math.min(cfg.maxCeiling, energyCeiling + cfg.ceilingStep);
            ceilingRaised = raised > energyCeiling;
            energyCeiling = raised;
        }
        energy = Math.min(energy, energyCeiling);
        freezeDecay(now);
        pendingImpacts.add(new ImpactEvent(waveIndex, lane, true, energy, ceilingRaised));
    }

    private void applyNegative(long now, int waveIndex, int lane) {
        energy = Math.max(0.0, energy + cfg.malusEnergy * (reconBoost ? cfg.reconMalusFactor : 1.0));
        freezeDecay(now);
        pendingImpacts.add(new ImpactEvent(waveIndex, lane, false, energy, false));
    }

    /** {@code max()}, never an assignment: an impact must never shorten a freeze already in flight. */
    private void freezeDecay(long now) {
        decayFrozenUntil = Math.max(decayFrozenUntil, now + cfg.postHitFreezeTicks());
    }

    private void takePortalOutcome(int rank, int typeId, long now) {
        outcomeRank = rank;
        outcomeTypeId = typeId;
        outcomeDistance = (int) Math.floor(odometer);
        phase = WarpRidePhase.PORTAL_TAKEN;
        lastActivityTick = now;
        heldPortalWaveIndex = -1;
        pendingPortalTake = false;
    }

    /** Zero energy. A soft fail: the ride continues, the rider loses agency over its outcome. */
    private void enterDrift(long now) {
        drifting = true;
        lastActivityTick = now;
        driftThreeQuarterTick = now + Math.round(0.75 * cfg.driftSeconds * 20.0);
        forcedDriftPortal = false;
        // Not planted here: the forced portal appears at the 3/4 mark, where a drifting rider takes it.
    }

    private boolean portalInFlight() {
        for (Wave wave : waves) {
            if (!wave.isResolved() && wave.hasPortal()) {
                return true;
            }
        }
        return false;
    }

    private void pruneWaves() {
        double cullDepth = cfg.fifthPlaneDepth;
        Iterator<Wave> it = waves.iterator();
        while (it.hasNext()) {
            Wave wave = it.next();
            if (wave.index() == heldPortalWaveIndex) {
                continue;   // never cull the portal parked at the plane
            }
            if (wave.isResolved() && wave.depth(odometer) < cullDepth) {
                it.remove();
            }
        }
    }
}
