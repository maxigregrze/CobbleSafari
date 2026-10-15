package maxigregrze.cobblesafari.client.screen.warpride;

import maxigregrze.cobblesafari.warpride.network.WarpRidePayloads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The client's predictive mirror of the server simulation; it decides nothing. Discrete values snap
 * on reconciliation, continuous ones ({@code odometer}, {@code energy}, {@code speed}) are eased.
 */
public final class WarpRideClientState {

    /** How fast an eased quantity closes on the server value, per second. */
    private static final double EASE_RATE = 12.0;

    public static final int PHASE_AWAITING_READY = 0;
    public static final int PHASE_COUNTDOWN = 1;
    public static final int PHASE_RIDING = 2;
    public static final int PHASE_PORTAL_TAKEN = 3;
    public static final int PHASE_ENDED = 4;

    /**
     * @param kind   ordinal of {@code WaveObject.Kind}: 0 positive, 1 negative, 2 portal
     * @param lane   0..7, or -1 for a portal, which sits on the axis
     * @param rank   portals only: 0..3
     * @param typeId portals only: the destination id, which selects the tint
     */
    public record ClientObject(int kind, int lane, int rank, int typeId) {
        public boolean isPortal() {
            return kind == 2;
        }
    }

    public static final class ClientWave {
        public final int index;
        public final double trackPos;
        public final List<ClientObject> objects;
        /** Set when the client sees it cross, so the hit/miss visual can differ. */
        public boolean crossed;
        /** Lanes whose object was actually hit, so only *missed* ones fly past. */
        public final java.util.Set<Integer> consumedLanes = new java.util.HashSet<>();

        public final boolean hasPortal;
        /** Wall-clock instant the portal reached the plane, in nanoseconds; 0 until then. */
        public long portalCrossNanos;
        public long portalHoldNanos;
        /** Length of the fade-away after the hold, in nanoseconds. */
        public long portalFlyPastNanos;

        ClientWave(int index, double trackPos, List<ClientObject> objects) {
            this.index = index;
            this.trackPos = trackPos;
            this.objects = objects;
            boolean portal = false;
            for (ClientObject o : objects) {
                if (o.isPortal()) {
                    portal = true;
                    break;
                }
            }
            this.hasPortal = portal;
        }

        public double depth(double odometer) {
            return trackPos - odometer;
        }

        /** True while the portal is still hovering or fading, i.e. not yet culled. */
        public boolean portalActive(long now) {
            return hasPortal && portalCrossNanos != 0
                    && now < portalCrossNanos + portalHoldNanos + portalFlyPastNanos;
        }

        /** True only during the takeable hover window, before the fade begins. */
        public boolean portalHovering(long now) {
            return hasPortal && portalCrossNanos != 0
                    && now - portalCrossNanos < portalHoldNanos;
        }

        /** Pinned to the plane while hovering, then a synthetic slide toward the fifth plane. */
        public double portalDepth(long now, double fifthPlaneDepth) {
            long elapsed = now - portalCrossNanos;
            if (elapsed < portalHoldNanos) {
                return 0.0;
            }
            long fadeElapsed = elapsed - portalHoldNanos;
            double t = Math.min(1.0, (double) fadeElapsed / Math.max(1L, portalFlyPastNanos));
            return fifthPlaneDepth * t;
        }
    }

    public UUID sessionId;
    public double baseSpeed = 32.0;
    public double minSpeed = 16.0;
    public double maxSpeed = 64.0;
    public double waveSpacingLy = 16.0;
    /** How deep objects are drawn, independent of the cadence and of the ring count. */
    public double visibleDepthLy = 32.0;
    /** Pure decoration: nothing about the simulation depends on it. */
    public int ringCount = 4;
    public double projectionK = 9.0;
    public double startEnergy = 50.0;
    public int inputCooldownMs = 25;
    public int jumpTransitMs = 240;
    public double ringFourRadius = 400.0;
    public double fifthPlaneDepth = -8.0;
    public double flyPastMaxScale = 2.5;
    public int starCount = 200;
    public int canvasWidth = 1920;
    public int canvasHeight = 1080;
    public int lagBudgetTicks = 4;
    public int laneRotationMs = 60;
    public double portalHoldWaves = 2.0;
    public double driftSeconds = 3.0;
    /** Seconds the player and portal shrink while portal_wrap plays. */
    public double warpAnimSeconds = 0.8;
    public double fadeToBlackSeconds = 0.4;

    public int phase = PHASE_AWAITING_READY;
    public double odometer;
    public double energy = 50.0;
    public double speed = 32.0;
    public int lane;
    public boolean drifting;
    /** Whether energy decay is frozen (opening grace or post-hit); drives the bar tint. */
    public boolean frozen;

    private double targetOdometer;
    private double targetEnergy = 50.0;
    private double targetSpeed = 32.0;

    /** Continuous, unwrapped lane the tunnel is drawn at, so turns and jumps sweep, not snap. */
    public double displayLane;

    private double tweenFrom;
    private double tweenTo;
    private long tweenStartNanos;
    private long tweenDurationNanos;

    /** Peak roll of the rider on a sidestep, and how long the input keeps it leaning. */
    private static final double BANK_MAX_DEGREES = 22.0;
    private static final long BANK_HOLD_NANOS = 180_000_000L;
    /** How fast the roll closes on its target, per second: leans in and levels out smoothly. */
    private static final double BANK_RATE = 14.0;

    /** Presentation only: the rider's roll about the depth axis, +CCW (a left turn), in degrees. */
    private double bankDegrees;
    private int bankSide;
    private long bankHoldUntilNanos;

    /** Wall-clock instants, in nanoseconds, for the presentation-only timers. */
    public long inputCooldownEndNanos;
    public long jumpStartNanos;
    public long jumpEndNanos;
    public long driftStartNanos;

    private long lastServerTick;
    private long lastServerTickNanos;

    /** Destination tints, synced separately from any session. */
    private final Map<Integer, Integer> tints = new HashMap<>();

    private final List<ClientWave> waves = new ArrayList<>();

    public boolean hasResult;
    public int resultRank;
    public int resultTypeId;
    public int resultDistance;

    /** Wall-clock instant the shrink/wrap begins (after any remaining drift-to-center). */
    public long warpStartNanos;

    public void applyOpen(WarpRidePayloads.OpenPayload p) {
        this.sessionId = p.sessionId();
        this.baseSpeed = p.baseSpeed();
        this.minSpeed = p.minSpeed();
        this.maxSpeed = p.maxSpeed();
        this.waveSpacingLy = p.waveSpacingLy();
        this.visibleDepthLy = p.visibleDepthLy();
        this.ringCount = p.ringCount();
        this.projectionK = p.projectionK();
        this.startEnergy = p.startEnergy();
        this.inputCooldownMs = p.inputCooldownMs();
        this.jumpTransitMs = p.jumpTransitMs();
        this.ringFourRadius = p.ringFourRadius();
        this.fifthPlaneDepth = p.fifthPlaneDepth();
        this.flyPastMaxScale = p.flyPastMaxScale();
        this.starCount = p.starCount();
        this.canvasWidth = p.canvasWidth();
        this.canvasHeight = p.canvasHeight();
        this.lagBudgetTicks = p.lagBudgetTicks();
        this.laneRotationMs = p.laneRotationMs();
        this.portalHoldWaves = p.portalHoldWaves();
        this.driftSeconds = p.driftSeconds();
        this.warpAnimSeconds = p.warpAnimSeconds();
        this.fadeToBlackSeconds = p.fadeToBlackSeconds();
        this.displayLane = 0.0;
        this.tweenFrom = 0.0;
        this.tweenTo = 0.0;
        this.tweenStartNanos = 0L;
        this.tweenDurationNanos = 0L;

        this.energy = p.startEnergy();
        this.targetEnergy = p.startEnergy();
        this.speed = speedFromEnergy(p.startEnergy());
        this.targetSpeed = this.speed;
        this.phase = PHASE_AWAITING_READY;
        this.waves.clear();
    }

    public void applyState(WarpRidePayloads.StatePayload p) {
        if (sessionId == null || !sessionId.equals(p.sessionId())) {
            return;
        }
        this.targetOdometer = p.odometer();
        this.targetEnergy = p.energy();
        this.targetSpeed = p.speed();
        this.lane = p.lane();                 // snap
        if (p.drifting() && !this.drifting) {
            this.driftStartNanos = System.nanoTime();
        }
        this.drifting = p.drifting();         // snap
        this.frozen = p.frozen();             // snap
        this.phase = p.phase();               // snap
        // Only re-aim when the server disagrees with where we are heading, or every packet restarts it.
        if (Math.floorMod((int) Math.round(tweenTo), 8) != p.lane()) {
            aimAtLane(p.lane(), laneRotationMs);
        }
        this.lastServerTick = p.serverTick();
        this.lastServerTickNanos = System.nanoTime();

        long cooldownNanos = p.cooldownTicks() * 50_000_000L;
        this.inputCooldownEndNanos = cooldownNanos <= 0
                ? 0L
                : System.nanoTime() + cooldownNanos;
    }

    public void applyWaves(WarpRidePayloads.WavesPayload p) {
        if (sessionId == null || !sessionId.equals(p.sessionId())) {
            return;
        }
        for (WarpRidePayloads.WaveData w : p.waves()) {
            List<ClientObject> objects = new ArrayList<>(w.objects().size());
            for (WarpRidePayloads.ObjectData o : w.objects()) {
                objects.add(new ClientObject(o.kind(), o.lane(), o.rank(), o.typeId()));
            }
            waves.add(new ClientWave(w.index(), w.trackPos(), objects));
        }
    }

    public void applyTints(List<WarpRidePayloads.TintData> list) {
        tints.clear();
        for (WarpRidePayloads.TintData t : list) {
            tints.put(t.typeId(), t.tint());
        }
    }

    public int tintFor(int typeId) {
        Integer tint = tints.get(typeId);
        return tint == null ? 0xFFFFFFFF : tint;
    }

    /** Advances the predicted state and eases it toward the last authoritative snapshot. */
    public void advance(double deltaSeconds) {
        // The countdown scrolls the track too; without integrating through it the intro reads as lag.
        if (phase == PHASE_RIDING || phase == PHASE_COUNTDOWN) {
            odometer += speed * deltaSeconds;
        }
        advanceTween();
        advanceBank(deltaSeconds);
        odometer = ease(odometer, targetOdometer, deltaSeconds);
        energy = ease(energy, targetEnergy, deltaSeconds);
        speed = ease(speed, targetSpeed, deltaSeconds);
        pruneWaves();
    }

    /**
     * Leans the rider into a sidestep: +1 for a left move (counter-clockwise, left wing down), -1
     * for a right one. Held while inputs keep coming, so a run of taps stays banked instead of
     * wobbling, then levels out on its own.
     */
    public void startBank(int side) {
        this.bankSide = side;
        this.bankHoldUntilNanos = System.nanoTime() + BANK_HOLD_NANOS;
    }

    public double bankDegrees() {
        return bankDegrees;
    }

    private void advanceBank(double deltaSeconds) {
        double target = System.nanoTime() < bankHoldUntilNanos ? bankSide * BANK_MAX_DEGREES : 0.0;
        bankDegrees += (target - bankDegrees) * (1.0 - Math.exp(-BANK_RATE * deltaSeconds));
    }

    private double ease(double current, double target, double deltaSeconds) {
        double factor = 1.0 - Math.exp(-EASE_RATE * deltaSeconds);
        return current + (target - current) * factor;
    }

    private void pruneWaves() {
        long now = System.nanoTime();
        Iterator<ClientWave> it = waves.iterator();
        while (it.hasNext()) {
            ClientWave wave = it.next();
            if (!wave.crossed && wave.depth(odometer) <= 0.0) {
                onWaveCrossed(wave, now);
            }
            // A portal wave lives on its own hover/fade timer, decoupled from the odometer.
            boolean keepForPortal = wave.hasPortal && wave.portalCrossNanos != 0 && wave.portalActive(now);
            if (!keepForPortal && wave.depth(odometer) < fifthPlaneDepth) {
                it.remove();
            }
        }
    }

    /** Fires the first frame a wave reaches the plane: predicts hits, starts a portal's hover. */
    private void onWaveCrossed(ClientWave wave, long now) {
        wave.crossed = true;
        for (ClientObject o : wave.objects) {
            if (!o.isPortal() && o.lane() == lane) {
                wave.consumedLanes.add(o.lane());
            }
        }
        if (wave.hasPortal && wave.portalCrossNanos == 0) {
            wave.portalCrossNanos = now;
            double secondsPerWave = waveSpacingLy / Math.max(1.0, speed);
            wave.portalHoldNanos = Math.round(portalHoldWaves * secondsPerWave * 1_000_000_000.0);
            wave.portalFlyPastNanos = Math.round(
                    Math.abs(fifthPlaneDepth) / Math.max(1.0, speed) * 1_000_000_000.0);
        }
    }

    public List<ClientWave> waves() {
        return waves;
    }

    /** True while a portal is parked at the plane and can be taken by a jump. */
    public boolean hasHoveringPortal() {
        long now = System.nanoTime();
        for (ClientWave wave : waves) {
            if (wave.portalHovering(now)) {
                return true;
            }
        }
        return false;
    }

    /** Marks one object as hit, so it vanishes at the plane instead of flying past. */
    public void consumeObject(int waveIndex, int lane) {
        for (ClientWave wave : waves) {
            if (wave.index == waveIndex) {
                wave.consumedLanes.add(lane);
                return;
            }
        }
    }

    /** Re-aims the tunnel the <b>shortest</b> way round, so lane 7 to 0 sweeps one step forward. */
    public void aimAtLane(int newLane, int durationMs) {
        double base = Math.floor(displayLane / 8.0) * 8.0;
        double best = base + newLane;
        for (double candidate : new double[]{base + newLane - 8.0, base + newLane, base + newLane + 8.0}) {
            if (Math.abs(candidate - displayLane) < Math.abs(best - displayLane)) {
                best = candidate;
            }
        }
        startTween(best, durationMs);
    }

    /** A jump sweeps a full 180°, in a random direction so every jump does not look identical. */
    public void aimAtJump(boolean clockwise, int durationMs) {
        startTween(displayLane + (clockwise ? 4.0 : -4.0), durationMs);
    }

    private void startTween(double target, int durationMs) {
        this.tweenFrom = displayLane;
        this.tweenTo = target;
        this.tweenStartNanos = System.nanoTime();
        this.tweenDurationNanos = Math.max(1L, durationMs * 1_000_000L);
    }

    private void advanceTween() {
        if (tweenDurationNanos <= 0L) {
            return;
        }
        long elapsed = System.nanoTime() - tweenStartNanos;
        if (elapsed >= tweenDurationNanos) {
            displayLane = tweenTo;
            tweenDurationNanos = 0L;
            return;
        }
        double t = elapsed / (double) tweenDurationNanos;
        // smoothstep: no visible acceleration jolt at either end of the swing
        double eased = t * t * (3.0 - 2.0 * t);
        displayLane = tweenFrom + (tweenTo - tweenFrom) * eased;
    }

    public double speedFromEnergy(double e) {
        double clamped = Math.max(0.0, Math.min(100.0, e));
        return baseSpeed * Math.pow(2.0, (clamped - 50.0) / 50.0);
    }

    public long estimatedServerTick() {
        if (lastServerTickNanos == 0L) {
            return 0L;
        }
        long elapsed = System.nanoTime() - lastServerTickNanos;
        return lastServerTick + elapsed / 50_000_000L;
    }

    public boolean isOnCooldown() {
        return System.nanoTime() < inputCooldownEndNanos;
    }

    public void startLocalCooldown(int durationMs) {
        this.inputCooldownEndNanos = System.nanoTime() + durationMs * 1_000_000L;
    }

    public void startLocalJump() {
        long now = System.nanoTime();
        this.jumpStartNanos = now;
        this.jumpEndNanos = now + jumpTransitMs * 1_000_000L;
    }

    public boolean isJumping() {
        long now = System.nanoTime();
        return now >= jumpStartNanos && now < jumpEndNanos;
    }

    /** 0..1 through the jump transit, or -1 when not jumping. */
    public double jumpProgress() {
        if (!isJumping()) {
            return -1.0;
        }
        long span = jumpEndNanos - jumpStartNanos;
        if (span <= 0L) {
            return -1.0;
        }
        return (System.nanoTime() - jumpStartNanos) / (double) span;
    }

    /** Freezes the world; on a drift take the shrink waits until the rider reaches the axis. */
    public void onPortalTaken(int rank, int typeId, int distance) {
        this.hasResult = true;
        this.resultRank = rank;
        this.resultTypeId = typeId;
        this.resultDistance = distance;
        this.phase = PHASE_PORTAL_TAKEN;
        long now = System.nanoTime();
        if (drifting) {
            long driftEnd = driftStartNanos + (long) (driftSeconds * 1_000_000_000.0);
            this.warpStartNanos = Math.max(now, driftEnd);
        } else {
            this.warpStartNanos = now;
        }
    }

    public long warpAnimNanos() {
        return (long) (warpAnimSeconds * 1_000_000_000.0);
    }

    public long fadeNanos() {
        return (long) (fadeToBlackSeconds * 1_000_000_000.0);
    }

    /** World frozen but the rider is still drifting to the axis (drift take only). */
    public boolean warpInDriftWait(long now) {
        return phase == PHASE_PORTAL_TAKEN && now < warpStartNanos;
    }

    /** 0..1 through the shrink, -1 before it starts (drift wait), &gt;1 once done. */
    public double warpProgress(long now) {
        if (phase != PHASE_PORTAL_TAKEN || now < warpStartNanos) {
            return -1.0;
        }
        return (double) (now - warpStartNanos) / Math.max(1L, warpAnimNanos());
    }

    /** 0..1 through the fade to black after the shrink, -1 before it starts, &gt;1 once black. */
    public double fadeProgress(long now) {
        long fadeStart = warpStartNanos + warpAnimNanos();
        if (phase != PHASE_PORTAL_TAKEN || now < fadeStart) {
            return -1.0;
        }
        return (double) (now - fadeStart) / Math.max(1L, fadeNanos());
    }
}
