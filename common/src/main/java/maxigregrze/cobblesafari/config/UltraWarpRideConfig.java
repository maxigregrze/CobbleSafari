package maxigregrze.cobblesafari.config;

/** Ultra Warp Ride gameplay values, in code rather than JSON; the client gets the subset it predicts with. */
public class UltraWarpRideConfig {

    
    // --- speed: v(E) = baseSpeed * 2^((E-50)/50) -> 16 / 32 / 64 ly/s at 0 / 50 / 100 % energy ---
    public double baseSpeed = 32.0;
    public double minSpeed = 16.0;
    public double maxSpeed = 64.0;

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

    // --- energy ---
    public double startEnergy = 50.0;
    public double decayPerSecond = 1.0;
    public double bonusEnergy = 5.0;
    public double malusEnergy = -10.0;

    // --- speed envelope ---
    /** Stall speed of a malus. Kept at minSpeed: a malus slows you to empty-bar speed, never below. */
    public double malusSpeed = 16.0;
    public int malusHoldMs = 400;
    public int rampMs = 600;

    // --- freezes ---
    public int openingGraceSeconds = 10;
    public int postHitFreezeSeconds = 3;

    // --- input ---
    public int inputCooldownMs = 25;
    // Drives the jump arc, the spin tween, the client cooldown and the server steering lock.
    public int jumpTransitMs = 240;
    public int laneRotationMs = 60;
    public int positiveToleranceMs = 80;

    // --- portals ---
    public int minPortalSpacingWaves = 4;
    /** How many waves a portal parks at the plane before fading (server take window + client hover). */
    public double portalHoldWaves = 2.0;

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

    public long inputCooldownTicks() {
        return Math.max(1L, Math.round(inputCooldownMs / 50.0));
    }

    public long jumpTransitTicks() {
        return Math.max(1L, Math.round(jumpTransitMs / 50.0));
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
