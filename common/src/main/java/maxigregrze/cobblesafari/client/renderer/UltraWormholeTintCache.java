package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client-side cache of destination-tinted copies of the wormhole layer textures
 * (action plan 183, section 4.2.1).
 *
 * <p><b>Why a derived texture rather than a tint.</b> The wanted weighting is full colour on the
 * dark end, none at all on white. Minecraft's tint is a <em>product</em> applied to the vertex
 * colour, so it weights the other way round - full on white, nothing on black - and no arrangement
 * of {@code tintindex}, layers or indices changes the <em>function</em> being applied, only which
 * quads receive it. A per-pixel function needs somewhere to be written, and this is it.</p>
 *
 * <p>A core shader was the alternative and is refused for the reason {@code GreyscaleTextureCache}
 * already records: Iris replaces core shaders when a shaderpack is active, which would disable the
 * effect <em>silently</em> for a subset of players.</p>
 *
 * <p>Entries are built lazily, bounded by LRU eviction, and dropped wholesale on a resource
 * reload. Both eviction paths close the GL texture.</p>
 */
public final class UltraWormholeTintCache {

    /** Destinations times layers. Six destinations is 84 entries, ~775 KB at 48x48 RGBA. */
    private static final int MAX_ENTRIES = 128;

    /**
     * When true, the source's own value is preserved and only the <em>hue</em> fades toward white:
     * {@code out = g * lerp(T, white, g)}. That is the vanilla product with the tint bleeding out
     * instead of onto white, and it renders a darker, more muted vortex.
     *
     * <p>False - the shipped default - uses {@code out = lerp(T, white, g)}, which lifts the grey
     * band to the <em>full</em> destination colour. That is the brief as it was given ("tint the
     * dark parts heavily"), and on these assets the band is the only place colour can read at all,
     * the rest of the art being white. Flip after seeing both in game; nothing else changes.</p>
     */
    private static final boolean MULTIPLY_BY_VALUE = false;

    /** Access-ordered so the eldest entry is the least recently used. */
    private static final Map<Long, ResourceLocation> CACHE = new LinkedHashMap<>(16, 0.75f, true);

    /**
     * One vertical strip of every layer per tint (action plan 200, section 8), so the whole vortex
     * draws through a single render type. A handful of destinations, so no eviction.
     */
    private static final Map<Integer, ResourceLocation> STACKS = new java.util.HashMap<>();

    private UltraWormholeTintCache() {}

    /**
     * @param source the untinted layer texture
     * @param tintRgb the destination colour, or {@link WarpRideClientTints#NO_TINT}
     * @return a tinted copy, or {@code source} itself when there is no tint or the build failed -
     *         the portal then renders white rather than crashing the frame
     */
    public static ResourceLocation tinted(ResourceLocation source, int tintRgb) {
        if (tintRgb == WarpRideClientTints.NO_TINT) {
            return source;
        }
        long key = (((long) source.hashCode()) << 32) ^ (tintRgb & 0xFFFFFFFFL);
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        ResourceLocation built = build(source, tintRgb);
        if (built == null) {
            return source;
        }
        CACHE.put(key, built);
        evictWhileOversized();
        return built;
    }

    /** Drops every derived texture. Wired to a client resource-reload listener on both loaders. */
    public static void clear() {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation id : CACHE.values()) {
            manager.release(id);
        }
        CACHE.clear();
        for (ResourceLocation id : STACKS.values()) {
            manager.release(id);
        }
        STACKS.clear();
    }

    /**
     * The layers stacked top to bottom in one texture, frame {@code i} being {@code frames[i]},
     * tinted like {@link #tinted} (untinted for {@link WarpRideClientTints#NO_TINT}).
     *
     * <p>Fifteen layers with fifteen textures are fifteen render types, and their compositing then
     * depends on the order those are flushed in - which a pipeline batching by render type (Iris) or
     * two portals seen from opposite sides break, letting a near layer's faint pixels stamp depth
     * over the layers behind it. One texture is one render type, whose quads the buffer sorts by
     * distance on upload.</p>
     *
     * @return the strip, or null when a layer is missing or the build failed
     */
    public static ResourceLocation stack(ResourceLocation[] frames, int tintRgb) {
        ResourceLocation cached = STACKS.get(tintRgb);
        if (cached != null) {
            return cached;
        }
        ResourceLocation built = buildStack(frames, tintRgb);
        if (built != null) {
            STACKS.put(tintRgb, built);
        }
        return built;
    }

    private static ResourceLocation buildStack(ResourceLocation[] frames, int tintRgb) {
        Minecraft minecraft = Minecraft.getInstance();
        NativeImage strip = null;
        try {
            for (int i = 0; i < frames.length; i++) {
                Optional<Resource> resource = minecraft.getResourceManager().getResource(frames[i]);
                if (resource.isEmpty()) {
                    throw new java.io.FileNotFoundException(frames[i].toString());
                }
                try (InputStream stream = resource.get().open(); NativeImage layer = NativeImage.read(stream)) {
                    int size = layer.getWidth();
                    if (strip == null) {
                        strip = new NativeImage(size, size * frames.length, true);
                    }
                    if (layer.getHeight() != size || size != strip.getWidth()) {
                        throw new IllegalStateException("layer " + frames[i] + " is not " + size + "x" + size);
                    }
                    layer.copyRect(strip, 0, 0, 0, i * size, size, size, false, false);
                }
            }
            if (tintRgb != WarpRideClientTints.NO_TINT) {
                applyTint(strip, tintRgb);
            }
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID,
                    String.format("warptint/stack/%08x", tintRgb));
            // Ownership passes to the DynamicTexture, as in build().
            minecraft.getTextureManager().register(id, new DynamicTexture(strip));
            return id;
        } catch (Exception e) {
            if (strip != null) {
                strip.close();
            }
            CobbleSafari.LOGGER.debug("[UltraWormhole] could not build the layer strip", e);
            return null;
        }
    }

    private static void evictWhileOversized() {
        if (CACHE.size() <= MAX_ENTRIES) {
            return;
        }
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        Iterator<Map.Entry<Long, ResourceLocation>> it = CACHE.entrySet().iterator();
        while (CACHE.size() > MAX_ENTRIES && it.hasNext()) {
            Map.Entry<Long, ResourceLocation> eldest = it.next();
            manager.release(eldest.getValue());
            it.remove();
        }
    }

    private static ResourceLocation build(ResourceLocation source, int tintRgb) {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(source);
        if (resource.isEmpty()) {
            CobbleSafari.LOGGER.debug("[UltraWormhole] no resource for texture '{}'", source);
            return null;
        }
        NativeImage image = null;
        try (InputStream stream = resource.get().open()) {
            image = NativeImage.read(stream);
            applyTint(image, tintRgb);
            // Ownership passes to the DynamicTexture, which closes the image when the texture is
            // released. Hence no try-with-resources: closing it would free it under the GPU.
            ResourceLocation id = derivedId(source, tintRgb);
            minecraft.getTextureManager().register(id, new DynamicTexture(image));
            return id;
        } catch (Exception e) {
            if (image != null) {
                image.close();
            }
            CobbleSafari.LOGGER.debug("[UltraWormhole] could not tint texture '{}'", source, e);
            return null;
        }
    }

    /**
     * Weighted toward the dark end: full destination colour on the darkest pixels, none at all on
     * white. This is a lerp toward white driven by luminance - deliberately <b>not</b> the multiply
     * that vertex-colour tinting performs, which weights the opposite way.
     *
     * <p>The fourteen layer textures are pure greyscale (measured saturation at most 0.01), so a
     * pixel's value <em>is</em> its luminance and any channel gives it - no conversion needed.
     * They are also 65-96 % transparent, and those pixels are skipped outright.</p>
     *
     * <p>{@code NativeImage} packs pixels as <b>ABGR</b> in native byte order, not ARGB: red is the
     * low byte and blue the third - the trap {@code GreyscaleTextureCache} documents.</p>
     */
    private static void applyTint(NativeImage image, int tintRgb) {
        int tr = (tintRgb >> 16) & 0xFF;
        int tg = (tintRgb >> 8) & 0xFF;
        int tb = tintRgb & 0xFF;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                int a = abgr >>> 24;
                if (a == 0) {
                    continue;
                }
                float g = (abgr & 0xFF) / 255.0f;
                float scale = MULTIPLY_BY_VALUE ? g : 1.0f;
                int r = clamp(scale * (tr + (255 - tr) * g));
                int gg = clamp(scale * (tg + (255 - tg) * g));
                int b = clamp(scale * (tb + (255 - tb) * g));
                image.setPixelRGBA(x, y, (a << 24) | (b << 16) | (gg << 8) | r);
            }
        }
    }

    private static int clamp(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    private static ResourceLocation derivedId(ResourceLocation source, int tintRgb) {
        String path = String.format("warptint/%06x/%s/%s",
                tintRgb & 0xFFFFFF, source.getNamespace(), source.getPath());
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, path);
    }
}
