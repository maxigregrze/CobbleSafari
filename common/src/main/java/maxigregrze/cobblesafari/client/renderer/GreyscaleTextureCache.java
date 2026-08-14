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
 * Client-side cache of desaturated copies of Pokémon textures, used by the statue renderer.
 *
 * <p>The tint applied by {@code PosableModel.render} is a multiply, so a coloured texture cannot be
 * turned grey by tinting alone: the desaturation has to happen on the texture itself. A derived
 * texture is preferred over a custom core shader because Iris replaces core shaders when a
 * shaderpack is active, which would silently disable the effect.</p>
 *
 * <p>Entries are built lazily on first use, bounded to {@value #MAX_ENTRIES} by LRU eviction, and
 * dropped wholesale on a resource reload. Both eviction paths close the GL texture.</p>
 */
public final class GreyscaleTextureCache {

    private static final int MAX_ENTRIES = 64;

    /** Rec. 709 luminance weights. */
    private static final float LUMA_R = 0.2126f;
    private static final float LUMA_G = 0.7152f;
    private static final float LUMA_B = 0.0722f;

    /**
     * Output range, as a fraction of full white. The full 0-255 span is compressed into
     * [20 %, 80 %] so a statue never has crushed-black or blown-out-white patches: pure black
     * would swallow the tint entirely (anything × 0 is 0) and pure white would show it undimmed.
     */
    private static final float GREY_MIN = 0.20f;
    private static final float GREY_MAX = 0.80f;

    /** Access-ordered so the eldest entry is the least recently used. */
    private static final Map<ResourceLocation, ResourceLocation> CACHE =
            new LinkedHashMap<>(16, 0.75f, true);

    private GreyscaleTextureCache() {}

    /**
     * Returns a desaturated copy of {@code source}, or {@code source} itself when the conversion
     * fails (the statue then renders in colour instead of crashing the frame).
     */
    public static ResourceLocation grey(ResourceLocation source) {
        ResourceLocation cached = CACHE.get(source);
        if (cached != null) {
            return cached;
        }
        ResourceLocation built = build(source);
        if (built == null) {
            return source;
        }
        CACHE.put(source, built);
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
    }

    private static void evictWhileOversized() {
        if (CACHE.size() <= MAX_ENTRIES) {
            return;
        }
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        Iterator<Map.Entry<ResourceLocation, ResourceLocation>> it = CACHE.entrySet().iterator();
        while (CACHE.size() > MAX_ENTRIES && it.hasNext()) {
            Map.Entry<ResourceLocation, ResourceLocation> eldest = it.next();
            manager.release(eldest.getValue());
            it.remove();
        }
    }

    private static ResourceLocation build(ResourceLocation source) {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(source);
        if (resource.isEmpty()) {
            CobbleSafari.LOGGER.debug("[PokeStatue] no resource for texture '{}'", source);
            return null;
        }
        NativeImage image = null;
        try (InputStream stream = resource.get().open()) {
            image = NativeImage.read(stream);
            desaturate(image);
            // Ownership passes to the DynamicTexture, which closes the image when the texture is
            // released. Hence no try-with-resources here: closing it would free it under the GPU.
            ResourceLocation id = derivedId(source);
            minecraft.getTextureManager().register(id, new DynamicTexture(image));
            return id;
        } catch (Exception e) {
            if (image != null) {
                image.close();
            }
            CobbleSafari.LOGGER.debug("[PokeStatue] could not desaturate texture '{}'", source, e);
            return null;
        }
    }

    /**
     * Rewrites every pixel to its luminance, alpha untouched.
     *
     * <p>{@code NativeImage} packs pixels as <b>ABGR</b> in native byte order, not ARGB: red is the
     * low byte and blue the third one. Swapping them would apply the red weight to the blue channel.</p>
     */
    private static void desaturate(NativeImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                int r = abgr & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int b = (abgr >> 16) & 0xFF;
                int a = abgr >>> 24;
                float luma = (LUMA_R * r + LUMA_G * g + LUMA_B * b) / 255.0f;
                int grey = Math.clamp(
                        Math.round(255.0f * (GREY_MIN + luma * (GREY_MAX - GREY_MIN))), 0, 255);
                image.setPixelRGBA(x, y, (a << 24) | (grey << 16) | (grey << 8) | grey);
            }
        }
    }

    private static ResourceLocation derivedId(ResourceLocation source) {
        String path = "grey/" + source.getNamespace() + "/" + source.getPath();
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, path);
    }
}
