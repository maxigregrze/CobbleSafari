package maxigregrze.cobblesafari.entity.projectile;

import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Origami Carrier paper plane (action plan 192, section 7.3). No vanilla gravity: the heading keeps
 * the launch yaw while the pitch follows a glide / dive cycle - slow shallow glides that lose speed,
 * short steep dives that gain it back - until it touches something and sticks.
 * <p>Pitch here is Minecraft's: positive points down.</p>
 */
public class PaperPlaneEntity extends StickyProjectileEntity {

    // Speeds halved after the first in-game test (plan 192 log).
    public static final float LAUNCH_SPEED = 0.5f;

    private static final float GLIDE_PITCH_MIN = 1.0f;
    private static final float GLIDE_PITCH_MAX = 3.0f;
    private static final int GLIDE_TICKS_MIN = 40;
    private static final int GLIDE_TICKS_MAX = 80;
    private static final float DIVE_PITCH = 70.0f;
    private static final int DIVE_TICKS = 10;

    private static final float LAUNCH_PITCH_STEP = 10.0f;
    private static final float GLIDE_PITCH_STEP = 15.0f;
    private static final float DIVE_PITCH_STEP = 20.0f;

    private static final double GLIDE_SPEED_DECAY = 0.97;
    private static final double GLIDE_SPEED_FLOOR = 0.175;
    private static final double DIVE_ACCELERATION = 0.03;
    private static final double DIVE_SPEED_CAP = 0.55;

    private static final int STUCK_LIFETIME_TICKS = 1200;

    private enum Phase { LAUNCH, GLIDE, DIVE }

    private Phase phase;
    private float heading;
    private float pitch;
    private float glidePitch;
    private int phaseTicks;
    private double speed;

    public PaperPlaneEntity(EntityType<? extends PaperPlaneEntity> type, Level level) {
        super(type, level);
    }

    public PaperPlaneEntity(Level level, LivingEntity shooter) {
        this(ModEntities.PAPER_PLANE, level);
        initFromShooter(shooter);
    }

    @Override
    protected Vec3 nextFlightMotion(Vec3 motion) {
        if (phase == null) {
            startFromMotion(motion);
        }
        switch (phase) {
            case LAUNCH -> {
                pitch = approach(pitch, glidePitch, LAUNCH_PITCH_STEP);
                if (pitch == glidePitch) {
                    startGlide();
                }
            }
            case GLIDE -> {
                pitch = approach(pitch, glidePitch, GLIDE_PITCH_STEP);
                speed = Math.max(GLIDE_SPEED_FLOOR, speed * GLIDE_SPEED_DECAY);
                if (--phaseTicks <= 0) {
                    phase = Phase.DIVE;
                    phaseTicks = DIVE_TICKS;
                }
            }
            case DIVE -> {
                pitch = approach(pitch, DIVE_PITCH, DIVE_PITCH_STEP);
                speed = Math.min(DIVE_SPEED_CAP, speed + DIVE_ACCELERATION);
                if (--phaseTicks <= 0) {
                    startGlide();
                }
            }
        }
        return Vec3.directionFromRotation(pitch, heading).scale(speed);
    }

    /** First flight tick (or after a reload): read heading, pitch and speed off the velocity. */
    private void startFromMotion(Vec3 motion) {
        speed = Math.max(GLIDE_SPEED_FLOOR, motion.length());
        heading = (float) (Mth.atan2(-motion.x, motion.z) * Mth.RAD_TO_DEG);
        pitch = (float) (Mth.atan2(-motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG);
        glidePitch = rollGlidePitch();
        phase = Phase.LAUNCH;
    }

    private void startGlide() {
        phase = Phase.GLIDE;
        glidePitch = rollGlidePitch();
        phaseTicks = Mth.nextInt(random, GLIDE_TICKS_MIN, GLIDE_TICKS_MAX);
    }

    private float rollGlidePitch() {
        return Mth.nextFloat(random, GLIDE_PITCH_MIN, GLIDE_PITCH_MAX);
    }

    private static float approach(float current, float target, float step) {
        if (Math.abs(target - current) <= step) {
            return target;
        }
        return current + Math.copySign(step, target - current);
    }

    @Override
    protected int stuckLifetimeTicks() {
        return STUCK_LIFETIME_TICKS;
    }
}
