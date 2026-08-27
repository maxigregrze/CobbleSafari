package maxigregrze.cobblesafari.warpride.logic;

/**
 * The object-driven override on top of the energy-derived target speed. A positive posts a linear
 * ramp to the target; a negative drops to the stall speed, holds, then ramps back. Overlaps are
 * reachable at high energy and are resolved in {@link #onPositive} / {@link #onNegative}.
 */
public final class SpeedEnvelope {

    private enum Mode {
        /** Linear ramp from {@link #fromSpeed} to the live target. */
        RAMP,
        /** Held flat at {@link #holdSpeed}, then a RAMP. */
        HOLD_THEN_RAMP
    }

    private Mode mode;
    private long startTick;
    private long holdTicks;
    private long rampTicks;
    private double fromSpeed;
    private double holdSpeed;

    public SpeedEnvelope() {
        this.mode = null;
    }

    public boolean isActive() {
        return mode != null;
    }

    /** A positive never restarts a running envelope: both kinds already ramp to the live target. */
    public void onPositive(long now, double currentSpeed, long rampTicksIn) {
        if (mode == Mode.HOLD_THEN_RAMP || mode == Mode.RAMP) {
            return;
        }
        this.mode = Mode.RAMP;
        this.startTick = now;
        this.rampTicks = Math.max(1L, rampTicksIn);
        this.holdTicks = 0L;
        this.fromSpeed = currentSpeed;
    }

    /** A negative impact always <em>replaces</em> whatever was running. */
    public void onNegative(long now, double malusSpeed, long holdTicksIn, long rampTicksIn) {
        this.mode = Mode.HOLD_THEN_RAMP;
        this.startTick = now;
        this.holdSpeed = malusSpeed;
        this.holdTicks = Math.max(1L, holdTicksIn);
        this.rampTicks = Math.max(1L, rampTicksIn);
        this.fromSpeed = malusSpeed;
    }

    public void clear() {
        this.mode = null;
    }

    /** @return the speed for this tick, {@code target} being {@code v(E)}; self-clears when expired */
    public double apply(long now, double target) {
        if (mode == null) {
            return target;
        }
        long elapsed = now - startTick;
        if (mode == Mode.HOLD_THEN_RAMP) {
            if (elapsed < holdTicks) {
                return holdSpeed;
            }
            long rampElapsed = elapsed - holdTicks;
            if (rampElapsed >= rampTicks) {
                mode = null;
                return target;
            }
            double t = (double) rampElapsed / (double) rampTicks;
            return holdSpeed + (target - holdSpeed) * t;
        }
        if (elapsed >= rampTicks) {
            mode = null;
            return target;
        }
        double t = (double) elapsed / (double) rampTicks;
        return fromSpeed + (target - fromSpeed) * t;
    }
}
