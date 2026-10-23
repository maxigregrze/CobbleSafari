package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import maxigregrze.cobblesafari.client.renderer.UltraWarpDiveModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

/**
 * Tint and glint on the rider's model, driven by the ride's events (action plan 191, E1). Each one
 * stands in for one of the source minigame's aura effects:
 *
 * <table>
 *   <caption>Ride events and their effect on the rider</caption>
 *   <tr><th>Event</th><th>Source</th><th>Here</th></tr>
 *   <tr><td>launch</td><td>auradome_st</td><td>cyan-white glint, fading over 0.6 s</td></tr>
 *   <tr><td>bonus</td><td>auradome_up</td><td>white flash, 0.25 s</td></tr>
 *   <tr><td>bonus raising the ceiling</td><td>auradome_maxup</td><td>gold glint, 0.6 s</td></tr>
 *   <tr><td>malus</td><td>hurdle_hit</td><td>yellow electric glint + a yellow blink at 3 Hz,
 *       while the post-hit freeze lasts</td></tr>
 *   <tr><td>end of that freeze</td><td>auradome_down</td><td>the yellow glint fades over 0.3 s</td></tr>
 *   <tr><td>energy above 50</td><td>the fast aura</td><td>a faint, steady blue glint</td></tr>
 * </table>
 *
 * <p>The rider is drawn through {@link #buffers()}, its own buffer source: tint wraps each vertex
 * consumer, and the glint is layered on with {@link VertexMultiConsumer}, the way enchanted items
 * do it. That needs the glint buffers to be <b>fixed</b> - two shared buffers cannot be filled at
 * once - so the source holds them, plus the vanilla glint types the elytra layer asks for.</p>
 */
public final class WarpRideRiderEffects {

    private static final long LAUNCH_NANOS = 600_000_000L;
    private static final long FLASH_NANOS = 250_000_000L;
    private static final long GOLD_NANOS = 600_000_000L;
    /** The yellow lasts while decay is frozen, but at least this long and at most the freeze. */
    private static final long HAZARD_MIN_NANOS = 300_000_000L;
    private static final long HAZARD_MAX_NANOS = 3_000_000_000L;
    private static final long HAZARD_FADE_NANOS = 300_000_000L;
    /** 3 Hz: a full yellow / neutral cycle every third of a second, the photosensitivity ceiling. */
    private static final long BLINK_PERIOD_NANOS = 333_333_333L;

    private static final float[] LAUNCH_GLINT = {0.70f, 1.00f, 1.00f};
    private static final float[] GOLD_GLINT = {1.00f, 0.75f, 0.25f};
    private static final float[] HAZARD_GLINT = {1.00f, 0.85f, 0.20f};
    private static final float[] FAST_GLINT = {0.35f, 0.50f, 1.00f};
    private static final float FAST_GLINT_STRENGTH = 0.35f;
    private static final float[] HAZARD_TINT = {1.00f, 0.95f, 0.55f};
    private static final float FLASH_TINT = 1.6f;
    private static final double FAST_ENERGY = 50.0;

    /** The glint colour, read by the glint render types when they are drawn. */
    private static final float[] GLINT_RGB = new float[3];
    /** Over armour-layer types (armour, elytra), which are drawn nudged towards the eye. */
    private static final RenderType GLINT_ARMOR = UltraWarpDiveModels.tintedGlint(
            "cobblesafari_warpride_rider_glint_armor", RenderType.armorEntityGlint(), 0.16f, GLINT_RGB);
    /** Over everything else (skin, cape). */
    private static final RenderType GLINT_DIRECT = UltraWarpDiveModels.tintedGlint(
            "cobblesafari_warpride_rider_glint", RenderType.entityGlintDirect(), 0.16f, GLINT_RGB);

    private final MultiBufferSource.BufferSource source;
    private final MultiBufferSource wrapped = this::buffer;

    private long launchNanos;
    private long flashNanos;
    private long goldNanos;
    private long hazardStartNanos;
    private boolean hazardActive;
    private long hazardFadeNanos;

    private float tintR = 1.0f;
    private float tintG = 1.0f;
    private float tintB = 1.0f;
    private float glintStrength;

    /** Native memory: freed by {@link #close()} when the screen goes away. */
    private final java.util.List<ByteBufferBuilder> allocations = new java.util.ArrayList<>();

    public WarpRideRiderEffects() {
        SequencedMap<RenderType, ByteBufferBuilder> fixed = new LinkedHashMap<>();
        for (RenderType type : new RenderType[]{
                RenderType.armorEntityGlint(), RenderType.glint(), RenderType.glintTranslucent(),
                RenderType.entityGlint(), RenderType.entityGlintDirect(), GLINT_ARMOR, GLINT_DIRECT}) {
            ByteBufferBuilder buffer = new ByteBufferBuilder(type.bufferSize());
            allocations.add(buffer);
            fixed.put(type, buffer);
        }
        ByteBufferBuilder shared = new ByteBufferBuilder(1 << 18);
        allocations.add(shared);
        this.source = MultiBufferSource.immediateWithBuffers(fixed, shared);
    }

    /** Releases the buffers. The instance must not be drawn with afterwards. */
    public void close() {
        for (ByteBufferBuilder buffer : allocations) {
            buffer.close();
        }
        allocations.clear();
    }

    public void onLaunch() {
        launchNanos = System.nanoTime();
    }

    public void onPositive(boolean ceilingRaised) {
        if (ceilingRaised) {
            goldNanos = System.nanoTime();
        } else {
            flashNanos = System.nanoTime();
        }
    }

    public void onNegative() {
        hazardStartNanos = System.nanoTime();
        hazardActive = true;
    }

    /** Works out this frame's tint and glint. The hazard's end is read from the decay freeze. */
    public void update(long now, WarpRideClientState state) {
        if (hazardActive) {
            long elapsed = now - hazardStartNanos;
            if ((!state.frozen && elapsed > HAZARD_MIN_NANOS) || elapsed > HAZARD_MAX_NANOS) {
                hazardActive = false;
                hazardFadeNanos = now;
            }
        }

        tintR = 1.0f;
        tintG = 1.0f;
        tintB = 1.0f;
        glintStrength = 0.0f;

        if (hazardActive) {
            setGlint(HAZARD_GLINT, 1.0f);
            if ((now - hazardStartNanos) % BLINK_PERIOD_NANOS < BLINK_PERIOD_NANOS / 2) {
                setTint(HAZARD_TINT[0], HAZARD_TINT[1], HAZARD_TINT[2]);
            }
            return;
        }
        float hazardFade = fading(now, hazardFadeNanos, HAZARD_FADE_NANOS);
        float gold = fading(now, goldNanos, GOLD_NANOS);
        float launch = fading(now, launchNanos, LAUNCH_NANOS);
        if (hazardFade > 0.0f) {
            setGlint(HAZARD_GLINT, hazardFade);
        } else if (gold > 0.0f) {
            setGlint(GOLD_GLINT, gold);
        } else if (launch > 0.0f) {
            setGlint(LAUNCH_GLINT, launch);
        } else if (state.energy > FAST_ENERGY) {
            setGlint(FAST_GLINT, FAST_GLINT_STRENGTH);
        }
        float flash = fading(now, flashNanos, FLASH_NANOS);
        if (flash > 0.0f) {
            float t = 1.0f + (FLASH_TINT - 1.0f) * flash;
            setTint(t, t, t);
        }
    }

    /** 1 at {@code start}, falling linearly to 0 after {@code length}; 0 if never started. */
    private static float fading(long now, long start, long length) {
        if (start == 0L || now - start >= length) {
            return 0.0f;
        }
        return 1.0f - (now - start) / (float) length;
    }

    private void setGlint(float[] rgb, float strength) {
        glintStrength = strength;
        GLINT_RGB[0] = rgb[0] * strength;
        GLINT_RGB[1] = rgb[1] * strength;
        GLINT_RGB[2] = rgb[2] * strength;
    }

    private void setTint(float r, float g, float b) {
        tintR = r;
        tintG = g;
        tintB = b;
    }

    /** The buffers the rider is drawn into. */
    public MultiBufferSource buffers() {
        return wrapped;
    }

    /** Draws the rider: its geometry first, then the fixed glint buffers over it. */
    public void flush() {
        source.endBatch();
    }

    private VertexConsumer buffer(RenderType type) {
        VertexConsumer base = source.getBuffer(type);
        if (tintR != 1.0f || tintG != 1.0f || tintB != 1.0f) {
            base = new Tinted(base, tintR, tintG, tintB);
        }
        if (glintStrength <= 0.0f || isGlint(type)) {
            return base;
        }
        // Armour layers are drawn nudged towards the eye; their glint must share that nudge.
        RenderType glint = type.toString().contains("armor") ? GLINT_ARMOR : GLINT_DIRECT;
        return VertexMultiConsumer.create(source.getBuffer(glint), base);
    }

    private static boolean isGlint(RenderType type) {
        return type.toString().contains("glint");
    }

    /** Multiplies every vertex colour by a tint; a tint above 1 brightens, clamped at white. */
    private record Tinted(VertexConsumer delegate, float r, float g, float b) implements VertexConsumer {

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(Math.min(255, Math.round(red * r)), Math.min(255, Math.round(green * g)),
                    Math.min(255, Math.round(blue * b)), alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
