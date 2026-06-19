package maxigregrze.cobblesafari.csboss.attack;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Shape rasterization on a block grid: circles (outline/filled) on the horizontal
 * plane and spheres, expressed as relative <b>offsets</b> {@link BlockPos} to the center. Used by
 * ring attacks ({@code base_electric_3}, {@code base_rock_2}).
 */
public final class CsBossGridShapes {

    private CsBossGridShapes() {}

    /** Circle outline (1-block thickness) of radius {@code radius} on plane {@code y = 0}. */
    public static List<BlockPos> circleOutline(int radius) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (Math.abs(d - radius) <= 0.5) {
                    out.add(new BlockPos(dx, 0, dz));
                }
            }
        }
        return out;
    }

    /** Filled disk of radius {@code radius} on plane {@code y = 0}. */
    public static List<BlockPos> filledCircle(int radius) {
        List<BlockPos> out = new ArrayList<>();
        double r2 = (radius + 0.5) * (radius + 0.5);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((double) dx * dx + (double) dz * dz <= r2) {
                    out.add(new BlockPos(dx, 0, dz));
                }
            }
        }
        return out;
    }

    /** Sphere shell (1-block thickness) of radius {@code radius}. */
    public static List<BlockPos> sphereShell(int radius) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
                    if (Math.abs(d - radius) <= 0.5) {
                        out.add(new BlockPos(dx, dy, dz));
                    }
                }
            }
        }
        return out;
    }
}
