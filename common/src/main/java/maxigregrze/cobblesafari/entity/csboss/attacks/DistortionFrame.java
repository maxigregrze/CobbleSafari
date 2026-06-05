package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;

/**
 * Transformation polaire → monde dans le repère tournant du boss (plan 107 § 6.5 révisé) :
 * une position relative ({@code angleDeg} relatif à l'orientation du boss, {@code radius} horizontal,
 * {@code localY} vertical) est convertie en coordonnées monde en tenant compte du yaw courant du boss.
 * Ainsi toute la structure de distorsion tourne avec le boss.
 */
final class DistortionFrame {

    private DistortionFrame() {}

    /** @return {@code {x, y, z}} monde pour la position polaire donnée autour du boss. */
    static double[] world(CsBossEntity boss, double angleDeg, double radius, double localY) {
        double worldAngle = Math.toRadians(boss.getYRot()) + Math.toRadians(angleDeg);
        double x = boss.getX() + radius * Math.cos(worldAngle);
        double z = boss.getZ() + radius * Math.sin(worldAngle);
        double y = boss.getY() + localY;
        return new double[] {x, y, z};
    }
}
