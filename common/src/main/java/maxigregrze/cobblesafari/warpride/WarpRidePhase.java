package maxigregrze.cobblesafari.warpride;

/** Lifecycle of one Ultra Warp Ride session. Transitions are strictly forward. */
public enum WarpRidePhase {
    /** Screen open on the tutorial page; the simulation is not running yet. */
    AWAITING_READY,
    /** 3 / 2 / 1 / GO. Waves already scroll, nothing can be hit. */
    COUNTDOWN,
    RIDING,
    /** A portal was taken; the simulation is frozen and the outcome is fixed. */
    PORTAL_TAKEN,
    /** Terminal. The session is about to be removed from the manager. */
    ENDED
}
