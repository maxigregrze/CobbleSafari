package maxigregrze.cobblesafari.warpride;

/** Lifecycle of one Ultra Warp Ride session. Transitions are strictly forward. */
public enum WarpRidePhase {
    /** Screen open on the tutorial page; the simulation is not running yet. */
    AWAITING_READY,
    /**
     * The source minigame's intro (action plan 191, I1): the start tunnel and the camera orbit. The
     * odometer is held at 0, energy does not decay, nothing can be hit.
     */
    INTRO,
    RIDING,
    /** A portal was taken; the simulation is frozen and the outcome is fixed. */
    PORTAL_TAKEN,
    /** Terminal. The session is about to be removed from the manager. */
    ENDED
}
