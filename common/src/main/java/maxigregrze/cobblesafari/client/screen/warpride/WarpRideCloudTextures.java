package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * The two cloud textures of the tube (action plan 191, V1 revised). Both are white with the cloud
 * in the alpha channel; the vertex colour tints them.
 *
 * <ul>
 *   <li>wall: seamless both ways, wrapped once around the tube and repeated along it;</li>
 *   <li>ring: a cloudy annulus, drawn flat as a ring the rider flies through.</li>
 * </ul>
 *
 * <p>The <b>authored</b> PNGs ({@link #AUTHORED_WALL}, {@link #AUTHORED_RING}) are used whenever
 * the resource pack has them. Until they exist, a noise texture <b>generated in code</b> stands in,
 * so the ride never shows the missing-texture checkerboard. The choice is made again each time a
 * ride screen opens, so dropping the PNGs in and reloading resources is enough.</p>
 */
public final class WarpRideCloudTextures {

    public static final ResourceLocation AUTHORED_WALL =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/warpride/cloud_wall.png");
    public static final ResourceLocation AUTHORED_RING =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/warpride/cloud_ring.png");

    private static final ResourceLocation GENERATED_WALL =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "warpride/generated/cloud_wall");
    private static final ResourceLocation GENERATED_RING =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "warpride/generated/cloud_ring");

    private static ResourceLocation wall = GENERATED_WALL;
    private static ResourceLocation ring = GENERATED_RING;

    /** The wall texture in use, authored or generated. */
    public static ResourceLocation wall() {
        return wall;
    }

    /** The ring texture in use, authored or generated. */
    public static ResourceLocation ring() {
        return ring;
    }

    private static final int WALL_SIZE = 256;
    private static final int RING_SIZE = 512;
    private static final int SEED = 191;

    /**
     * Where the ring's cloud band is centred, as a fraction of the texture's half-width. The ring
     * quad is sized so that this radius lands on the lane floor; an authored ring must match it.
     */
    public static final float RING_RADIUS = 0.8f;
    private static final double RING_WIDTH = 0.07;

    private static boolean wallGenerated;
    private static boolean ringGenerated;

    private WarpRideCloudTextures() {}

    /** Picks authored or generated for each texture; called on every ride screen. Render thread only. */
    public static void ensure() {
        wall = resolve(AUTHORED_WALL, GENERATED_WALL, true);
        ring = resolve(AUTHORED_RING, GENERATED_RING, false);
    }

    private static ResourceLocation resolve(ResourceLocation authored, ResourceLocation generated, boolean isWall) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourceManager().getResource(authored).isPresent()) {
            configure(mc.getTextureManager().getTexture(authored));
            return authored;
        }
        if (isWall && !wallGenerated) {
            wallGenerated = true;
            register(generated, buildWall());
        } else if (!isWall && !ringGenerated) {
            ringGenerated = true;
            register(generated, buildRing());
        }
        return generated;
    }

    private static void register(ResourceLocation id, NativeImage image) {
        DynamicTexture texture = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        configure(texture);
    }

    /** Smooth filtering, and repeat: the wall's V coordinate runs past 1 along the track. */
    private static void configure(net.minecraft.client.renderer.texture.AbstractTexture texture) {
        texture.setFilter(true, false);
        texture.bind();
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
    }

    /** Periodic fBm on a torus, so the texture tiles around the tube and along it. */
    private static NativeImage buildWall() {
        NativeImage image = new NativeImage(WALL_SIZE, WALL_SIZE, false);
        for (int y = 0; y < WALL_SIZE; y++) {
            for (int x = 0; x < WALL_SIZE; x++) {
                double sum = 0.0;
                double amplitude = 0.5;
                double norm = 0.0;
                int frequency = 1;
                for (int octave = 0; octave < 5; octave++) {
                    int period = 8 * frequency;
                    sum += amplitude * wrappedNoise(x / (double) WALL_SIZE * period,
                            y / (double) WALL_SIZE * period, period);
                    norm += amplitude;
                    amplitude *= 0.5;
                    frequency *= 2;
                }
                double cloud = smooth(clamp01((sum / norm - 0.42) / 0.3));
                image.setPixelRGBA(x, y, whiteWithAlpha(cloud));
            }
        }
        return image;
    }

    /**
     * A Gaussian band at {@link #RING_RADIUS}, broken up by 3D fBm sampled on a circle - so the
     * pattern has no seam where the angle wraps.
     */
    private static NativeImage buildRing() {
        NativeImage image = new NativeImage(RING_SIZE, RING_SIZE, false);
        for (int y = 0; y < RING_SIZE; y++) {
            for (int x = 0; x < RING_SIZE; x++) {
                double u = (x + 0.5) / RING_SIZE * 2.0 - 1.0;
                double v = (y + 0.5) / RING_SIZE * 2.0 - 1.0;
                double r = Math.hypot(u, v);
                double q = (r - RING_RADIUS) / RING_WIDTH;
                double mask = Math.exp(-q * q * 2.2);
                double alpha = 0.0;
                if (mask > 0.003) {
                    double theta = Math.atan2(v, u);
                    double n = fbm3(Math.cos(theta) * 2.6 + 5.0, Math.sin(theta) * 2.6 + 5.0, r * 4.5, 5);
                    alpha = mask * smooth(clamp01((n - 0.42) / 0.3));
                }
                image.setPixelRGBA(x, y, whiteWithAlpha(alpha));
            }
        }
        return image;
    }

    private static int whiteWithAlpha(double alpha) {
        int a = (int) Math.round(clamp01(alpha) * 255.0);
        return (a << 24) | 0x00FFFFFF;
    }

    private static double fbm3(double x, double y, double z, int octaves) {
        double sum = 0.0;
        double amplitude = 0.5;
        double frequency = 1.0;
        double norm = 0.0;
        for (int i = 0; i < octaves; i++) {
            sum += amplitude * noise3(x * frequency, y * frequency, z * frequency);
            norm += amplitude;
            amplitude *= 0.5;
            frequency *= 2.03;
        }
        return sum / norm;
    }

    private static double noise3(double x, double y, double z) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        double fz = smooth(z - z0);
        double a = lerp(lerp(hash(x0, y0, z0), hash(x0 + 1, y0, z0), fx),
                lerp(hash(x0, y0 + 1, z0), hash(x0 + 1, y0 + 1, z0), fx), fy);
        double b = lerp(lerp(hash(x0, y0, z0 + 1), hash(x0 + 1, y0, z0 + 1), fx),
                lerp(hash(x0, y0 + 1, z0 + 1), hash(x0 + 1, y0 + 1, z0 + 1), fx), fy);
        return lerp(a, b, fz);
    }

    /** 2D value noise whose lattice wraps every {@code period} cells on both axes. */
    private static double wrappedNoise(double x, double y, int period) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        int xa = Math.floorMod(x0, period);
        int xb = Math.floorMod(x0 + 1, period);
        int ya = Math.floorMod(y0, period);
        int yb = Math.floorMod(y0 + 1, period);
        return lerp(lerp(hash(xa, ya, 7), hash(xb, ya, 7), fx), lerp(hash(xa, yb, 7), hash(xb, yb, 7), fx), fy);
    }

    private static double hash(int x, int y, int z) {
        int h = x * 374_761_393 + y * 668_265_263 + z * 2_147_483_647 + SEED * 1_013_904_223;
        h = (h ^ (h >>> 13)) * 1_274_126_177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
