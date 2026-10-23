package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The view of the ride's perspective passes (action plan 191, C1). Presentation only: nothing here
 * can reach the simulation.
 *
 * <p>World units are canvas pixels, the chase eye sits at the origin looking down -Z - the view the
 * ride has always had, which this class returns unchanged outside the intro and the feedback.</p>
 *
 * <ul>
 *   <li><b>Intro</b>: the source minigame's four-phase camera ({@code Camera::Beginning}), its
 *   Solgaleo keyframes scaled by {@link #INTRO_PX_PER_UNIT}, retargeted on the rider's centre so
 *   the rider stays centred (Decision 6.2), and its eased cylindrical orbit kept verbatim.</li>
 *   <li><b>Feedback</b>: the source's chase-camera dolly (push in on a bonus, pull back on a hit)
 *   and roll shake on a hit, both attenuated (Decision 6.4).</li>
 * </ul>
 */
public final class WarpRideCamera {

    /** Frames of the source's intro phases, at its 30 fps. */
    public static final double PHASE_1_END = 70.0;
    public static final double PHASE_2_END = 85.0;
    public static final double PHASE_3_END = 187.0;
    public static final double INTRO_END = 197.0;

    /** Source chase distance (1250 units) onto ours (the 771 px eye-to-rider focal). */
    private static final float INTRO_PX_PER_UNIT = WarpRideScene3D.FOCAL_PX / 1250.0f;

    // Keyframes relative to the rider's centre, world axes: the source's +Z (forward) is our -Z.
    private static final Vector3f KEY0_EYE = key(409.2f, 104.5f, 747.5f);
    private static final Vector3f KEY1_EYE = key(409.2f, 104.5f, 231.3f);
    private static final Vector3f KEY2_EYE = key(0.0f, 249.4f, -4007.1f);
    private static final Vector3f KEY2_TARGET = key(0.0f, 97.7f, 558.1f);
    /** Keys 0 and 1 look straight at the rider: the source aimed slightly off to one side. */
    private static final Vector3f RIDER_TARGET = new Vector3f();

    // --- feedback (attenuated: half the converted source dolly, 6 instead of 15 degrees) ---
    private static final double BONUS_DOLLY = -500.0 * INTRO_PX_PER_UNIT * 0.5;
    private static final double HIT_DOLLY = 400.0 * INTRO_PX_PER_UNIT * 0.5;
    private static final double DOLLY_APPROACH_PER_FRAME = 0.15;
    private static final double DOLLY_DECAY_PER_FRAME = 0.95;
    private static final double ROLL_DEGREES = 6.0;
    private static final long ROLL_NANOS = 333_000_000L;      // 10 source frames
    private static final double ROLL_PERIOD_SECONDS = 4.0 / 30.0;

    /** Camera offset along Z: negative is nearer the rider. */
    private double dolly;
    private double dollyTarget;
    private long rollStartNanos;

    private static Vector3f key(float x, float y, float forward) {
        return new Vector3f(x * INTRO_PX_PER_UNIT, y * INTRO_PX_PER_UNIT, -forward * INTRO_PX_PER_UNIT);
    }

    public void onPositive() {
        dollyTarget = BONUS_DOLLY;
    }

    public void onNegative() {
        dollyTarget = HIT_DOLLY;
        rollStartNanos = System.nanoTime();
    }

    /** The source's per-frame dolly dynamics, carried over to real time. */
    public void advance(double deltaSeconds) {
        double frames = deltaSeconds * 30.0;
        dolly += (dollyTarget - dolly) * (1.0 - Math.pow(1.0 - DOLLY_APPROACH_PER_FRAME, frames));
        dollyTarget *= Math.pow(DOLLY_DECAY_PER_FRAME, frames);
    }

    /** Roll of the hit shake, in degrees: a decaying wobble, then nothing. */
    public double rollDegrees(long now) {
        if (rollStartNanos == 0L) {
            return 0.0;
        }
        long elapsed = now - rollStartNanos;
        if (elapsed >= ROLL_NANOS) {
            return 0.0;
        }
        double t = elapsed / 1.0e9;
        double envelope = 1.0 - elapsed / (double) ROLL_NANOS;
        return ROLL_DEGREES * envelope * Math.sin(2.0 * Math.PI * t / ROLL_PERIOD_SECONDS);
    }

    /** The chase view with the feedback applied: roll about the view axis, then the dolly. */
    public Matrix4f rideView(long now) {
        return new Matrix4f()
                .rotateZ((float) Math.toRadians(rollDegrees(now)))
                .translate(0.0f, 0.0f, (float) -dolly);
    }

    /**
     * The same feedback for 2D drawn over the 3D scene (portal rank strips, the aura): exact at the
     * rider's plane. GUI Y points down, so the roll turns the other way.
     */
    public void apply2D(PoseStack pose, long now) {
        double scale = WarpRideScene3D.FOCAL_PX / Math.max(1.0, WarpRideScene3D.FOCAL_PX + dolly);
        pose.translate(WarpRideRenderer.CENTER_X, WarpRideRenderer.CENTER_Y, 0.0f);
        pose.mulPose(Axis.ZP.rotationDegrees((float) -rollDegrees(now)));
        pose.scale((float) scale, (float) scale, 1.0f);
        pose.translate(-WarpRideRenderer.CENTER_X, -WarpRideRenderer.CENTER_Y, 0.0f);
    }

    /**
     * The intro view at {@code frame} (source frames, 30 per second), or {@code null} once the
     * intro is over and the chase view applies.
     *
     * @param riderCenter the rider's centre in world units
     */
    public static Matrix4f introView(double frame, Vector3f riderCenter) {
        if (frame >= INTRO_END) {
            return null;
        }
        Vector3f eye = new Vector3f();
        Vector3f target = new Vector3f();
        if (frame < PHASE_1_END) {
            double u = Math.max(0.0, frame) / PHASE_1_END;
            KEY0_EYE.lerp(KEY1_EYE, (float) u, eye);
            target.set(RIDER_TARGET);
        } else if (frame < PHASE_2_END) {
            eye.set(KEY1_EYE);
            target.set(RIDER_TARGET);
        } else if (frame < PHASE_3_END) {
            double t = Math.min(1.0, (frame - PHASE_2_END) / (PHASE_3_END - PHASE_2_END - 1.0));
            orbit(KEY1_EYE, KEY2_EYE, t, eye);
            orbit(RIDER_TARGET, KEY2_TARGET, t, target);
        } else {
            // Phase 4: a straight blend onto the chase camera, so the hand-over is pixel-exact.
            double u = (frame - PHASE_3_END) / (INTRO_END - PHASE_3_END);
            Vector3f chaseEye = new Vector3f(riderCenter).negate();
            Vector3f chaseTarget = new Vector3f(0.0f, -riderCenter.y, 0.0f);
            KEY2_EYE.lerp(chaseEye, (float) u, eye);
            KEY2_TARGET.lerp(chaseTarget, (float) u, target);
        }
        eye.add(riderCenter);
        target.add(riderCenter);
        return new Matrix4f().lookAt(eye, target, new Vector3f(0.0f, 1.0f, 0.0f));
    }

    /**
     * The source's phase-3 orbit: around the rider's vertical axis, angle eased by
     * {@code (1 - e^-5t) / (1 - e^-5)}, radius by {@code (1 - e^-4t) / (1 - e^-4)}, height linear.
     * A point on the axis takes the other end's angle.
     */
    private static void orbit(Vector3f from, Vector3f to, double t, Vector3f out) {
        double r1 = Math.hypot(from.x, from.z);
        double r2 = Math.hypot(to.x, to.z);
        double a2 = Math.atan2(to.x, to.z);
        double a1 = r1 < 1.0e-3 ? a2 : Math.atan2(from.x, from.z);
        if (r2 < 1.0e-3) {
            a2 = a1;
        }
        double delta = a2 - a1;
        while (delta > Math.PI) {
            delta -= 2.0 * Math.PI;
        }
        while (delta < -Math.PI) {
            delta += 2.0 * Math.PI;
        }
        double angleEase = (1.0 - Math.exp(-5.0 * t)) / (1.0 - Math.exp(-5.0));
        double radiusEase = (1.0 - Math.exp(-4.0 * t)) / (1.0 - Math.exp(-4.0));
        double angle = a1 + delta * angleEase;
        double radius = r1 + (r2 - r1) * radiusEase;
        out.set((float) (Math.sin(angle) * radius),
                (float) (from.y + (to.y - from.y) * t),
                (float) (Math.cos(angle) * radius));
    }
}
