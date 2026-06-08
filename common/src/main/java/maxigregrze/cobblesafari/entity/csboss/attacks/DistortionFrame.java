package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;

/**
 * Polar → world transform in the boss's rotating frame (plan 107 § 6.5 revised):
 * a relative position ({@code angleDeg} relative to the boss orientation, horizontal {@code radius},
 * vertical {@code localY}) is converted to world coordinates using the boss's current yaw.
 * Thus the entire distortion structure rotates with the boss.
 */
final class DistortionFrame {

    private DistortionFrame() {}

    /** @return world {@code {x, y, z}} for the given polar position around the boss. */
    static double[] world(CsBossEntity boss, double angleDeg, double radius, double localY) {
        double worldAngle = Math.toRadians(boss.getYRot()) + Math.toRadians(angleDeg);
        double x = boss.getX() + radius * Math.cos(worldAngle);
        double z = boss.getZ() + radius * Math.sin(worldAngle);
        double y = boss.getY() + localY;
        return new double[] {x, y, z};
    }
}
