package maxigregrze.cobblesafari.config;

/** Ultra Warp Ride gameplay values, in code rather than JSON; the client gets the subset it predicts with. */
public class UltraWarpRideConfig {

    // --- speed: the source's linear law, v(E) = minSpeed + (maxSpeed - minSpeed) * E / 100 ---
    // -> 15 / 36 / 57 ly/s at 0 / 50 / 100 energy (plan 191, section 3.2)
    public double minSpeed = 15.0;
    public double maxSpeed = 57.0;

    // --- track and projection ---
    public double waveSpacingLy = 16.0;
    /** How far the client draws (plan 189: one ring step past the original 32 ly). */
    public double visibleDepthLy = 40.0;
    /**
     * How far ahead the server generates. Deliberately decoupled from {@link #visibleDepthLy}: the
     * generator reads the odometer at generation time, so generating earlier would shift
     * difficulty and portal forcing - a mechanic change (plan 189, section 5.2).
     */
    public double generationDepthLy = 32.0;
    public int ringCount = 5;
    public double projectionK = 9.0;

    // --- energy: source 2.0 = 100 here (plan 191, section 2) ---
    public double startEnergy = 35.0;
    public double decayPerSecond = 1.25;
    public double bonusEnergy = 5.0;
    public double malusEnergy = -10.0;

    // --- energy ceiling: only rises, by ceilingStep each time a bonus overflows it (section 3.1) ---
    public double startCeiling = 50.0;
    public double ceilingStep = 1.0;
    public double maxCeiling = 100.0;

    // --- freezes ---
    public int openingGraceSeconds = 10;
    public int postHitFreezeSeconds = 3;

    // --- input: the three durations are their values at 50 energy, scaled by steeringFactor(E) ---
    public int inputCooldownMs = 25;
    // Drives the jump arc, the spin tween, the client cooldown and the server steering lock.
    public int jumpTransitMs = 240;
    public int laneRotationMs = 60;
    public int positiveToleranceMs = 80;
    /** The source's steering step, 0.02 + 0.015 * E_source, with E_source = E / 50 (section 3.3). */
    public double steeringBase = 0.02;
    public double steeringPerEnergy = 0.0003;

    // --- portals ---
    public int minPortalSpacingWaves = 4;
    /** How many waves a portal parks at the plane before fading (server take window + client hover). */
    public double portalHoldWaves = 2.0;

    // --- intro: the source's 197 frames at 30 fps; launch at frame 187 (plan 191, section 5) ---
    public int introTicks = 132;
    public int introLaunchTick = 125;

    // --- opening spawn grace ---
    /** Seconds of riding, after the countdown, kept object-free so a ride never starts on a malus. */
    public double startGraceSeconds = 3.0;

    // --- latency ---
    public int lagBudgetTicks = 4;

    // --- drift ---
    public int forcedPortalMaxRank = 3;
    public double driftSeconds = 3.0;

    // --- portal-entry warp ---
    /** Seconds the player and portal shrink to nothing while portal_wrap plays. */
    public double warpAnimSeconds = 0.8;
    public double fadeToBlackSeconds = 0.4;

    // --- presentation ---
    public int virtualCanvasWidth = 1920;
    public int virtualCanvasHeight = 1080;
    public double ringFourRadius = 480.0;
    public double fifthPlaneDepth = -8.0;
    public double flyPastMaxScale = 2.5;
    public int starCount = 200;

    // --- lifecycle ---
    public int sessionWatchdogSeconds = 60;

    private static final UltraWarpRideConfig INSTANCE = new UltraWarpRideConfig();

    public static UltraWarpRideConfig get() {
        return INSTANCE;
    }

    // --- derived helpers, so the same arithmetic is never written twice ---

    /** v(E), clamped to the energy range; shared by the server and the client prediction. */
    public static double speedFromEnergy(double energy, double minSpeed, double maxSpeed) {
        double clamped = Math.max(0.0, Math.min(100.0, energy));
        return minSpeed + (maxSpeed - minSpeed) * clamped / 100.0;
    }

    /**
     * Duration multiplier for steering at this energy: 1 at 50, 1.75 at 0, 0.7 at 100. The source
     * steers in steps of {@code base + perEnergy * E}; a bigger step is a shorter move here.
     */
    public static double steeringFactor(double energy, double base, double perEnergy) {
        double clamped = Math.max(0.0, Math.min(100.0, energy));
        return (base + perEnergy * 50.0) / Math.max(1.0E-6, base + perEnergy * clamped);
    }

    public double speedFromEnergy(double energy) {
        return speedFromEnergy(energy, minSpeed, maxSpeed);
    }

    public long inputCooldownTicks(double energy) {
        return Math.max(1L, Math.round(inputCooldownMs * steeringFactor(energy, steeringBase, steeringPerEnergy) / 50.0));
    }

    public long jumpTransitTicks(double energy) {
        return Math.max(1L, Math.round(jumpTransitMs * steeringFactor(energy, steeringBase, steeringPerEnergy) / 50.0));
    }

    public long openingGraceTicks() {
        return openingGraceSeconds * 20L;
    }

    public long postHitFreezeTicks() {
        return postHitFreezeSeconds * 20L;
    }

    /** Half-width of the positive-pickup window, expressed in ticks. */
    public long positiveToleranceTicks() {
        return Math.max(1L, Math.round(positiveToleranceMs / 50.0));
    }

    public double spawnDepth() {
        return generationDepthLy;
    }
}
