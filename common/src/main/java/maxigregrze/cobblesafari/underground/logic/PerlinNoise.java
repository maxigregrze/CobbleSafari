package maxigregrze.cobblesafari.underground.logic;

import java.util.Random;

/**
 * 2D Perlin noise with seed for deterministic generation.
 * Used to create smooth "blobs" of wall tiers (tier 6 regions surrounded by tier 5, etc.).
 */
public final class PerlinNoise {

    private static final int PERM_SIZE = 256;
    private final int[] perm = new int[PERM_SIZE * 2];

    /** Gradient vectors for 2D (4 cardinal + 4 diagonal). */
    private static final double[][] GRAD2 = {
        { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
        { 0.707, 0.707 }, { -0.707, 0.707 }, { 0.707, -0.707 }, { -0.707, -0.707 }
    };

    public PerlinNoise(long seed) {
        Random r = new Random(seed);
        int[] p = new int[PERM_SIZE];
        for (int i = 0; i < PERM_SIZE; i++) {
            p[i] = i;
        }
        for (int i = PERM_SIZE - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        for (int i = 0; i < PERM_SIZE * 2; i++) {
            perm[i] = p[i & (PERM_SIZE - 1)];
        }
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private static double dot2(int gi, double x, double y) {
        return GRAD2[gi][0] * x + GRAD2[gi][1] * y;
    }

    /**
     * 2D Perlin noise. Returns a value in approximately [-1, 1].
     * Caller can remap to [0, 1] for tier selection.
     */
    public double noise2d(double x, double y) {
        int xi = (int) Math.floor(x) & (PERM_SIZE - 1);
        int yi = (int) Math.floor(y) & (PERM_SIZE - 1);
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);

        double u = fade(xf);
        double v = fade(yf);

        int aa = perm[perm[xi] + yi];
        int ab = perm[perm[xi] + yi + 1];
        int ba = perm[perm[xi + 1] + yi];
        int bb = perm[perm[xi + 1] + yi + 1];

        double x0 = xf;
        double x1 = xf - 1;
        double y0 = yf;
        double y1 = yf - 1;

        double n00 = dot2(aa & 7, x0, y0);
        double n10 = dot2(ba & 7, x1, y0);
        double n01 = dot2(ab & 7, x0, y1);
        double n11 = dot2(bb & 7, x1, y1);

        return lerp(lerp(n00, n10, u), lerp(n01, n11, u), v);
    }

    /**
     * Returns value in [0, 1] by rescaling and clamping.
     * Perlin raw output is roughly in [-1, 1].
     */
    public double noise2dNormalized(double x, double y) {
        double n = noise2d(x, y);
        return Math.clamp((n + 1) * 0.5, 0.0, 1.0);
    }
}
