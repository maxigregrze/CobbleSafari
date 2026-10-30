package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.network.HyperspaceSkyMapPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;

/**
 * Client side of the Hyperspace sky map (action plan 197, section 6.3): the texture the sky shader
 * samples in its disc. It belongs to the level it was received in, and is dropped as soon as the
 * player is somewhere else.
 */
public final class HyperspaceSkyMapTexture {

    private static final ResourceLocation DYNAMIC_ID =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dynamic/hyperspace_sky_map");
    private static final ResourceLocation FALLBACK_ID =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/environment/hyperspace_sky_fallback.png");

    private enum Mode { NONE, MAP, STATIC }

    private static Mode mode = Mode.NONE;
    private static DynamicTexture texture;
    private static ClientLevel ownerLevel;
    /** Whether the fallback image exists; looked up once per static payload, not per frame. */
    private static boolean fallbackPresent;

    private HyperspaceSkyMapTexture() {}

    public static void accept(HyperspaceSkyMapPayload payload) {
        ownerLevel = Minecraft.getInstance().level;
        switch (payload.kind()) {
            case HyperspaceSkyMapPayload.KIND_MAP -> {
                upload(payload.colors());
                mode = Mode.MAP;
            }
            case HyperspaceSkyMapPayload.KIND_STATIC -> {
                release();
                fallbackPresent = Minecraft.getInstance().getResourceManager().getResource(FALLBACK_ID).isPresent();
                mode = Mode.STATIC;
            }
            default -> {
                release();
                mode = Mode.NONE;
            }
        }
    }

    /**
     * Binds the map to {@code unit} for the sky shader and returns its {@code MapMode}: 1 when a map
     * is bound, 0 when the disc should show its haze only.
     */
    static int bind(int unit) {
        if (ownerLevel != Minecraft.getInstance().level) {
            release();
            mode = Mode.NONE;
            return 0;
        }
        if (mode == Mode.MAP && texture != null) {
            RenderSystem.setShaderTexture(unit, DYNAMIC_ID);
            return 1;
        }
        if (mode == Mode.STATIC && fallbackPresent) {
            RenderSystem.setShaderTexture(unit, FALLBACK_ID);
            return 1;
        }
        return 0;
    }

    private static void upload(byte[] colors) {
        int size = HyperspaceSkyMapPayload.MAP_SIZE;
        if (texture == null) {
            texture = new DynamicTexture(size, size, true);
            Minecraft.getInstance().getTextureManager().register(DYNAMIC_ID, texture);
        }
        NativeImage pixels = texture.getPixels();
        if (pixels == null) {
            return;
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Packed id 0 is MapColor.NONE, which comes out fully transparent: "no data".
                pixels.setPixelRGBA(x, y, MapColor.getColorFromPackedId(colors[y * size + x] & 0xFF));
            }
        }
        texture.upload();
        // Linear filtering (decision 6.3): the disc spreads 256 pixels over about a thousand.
        texture.setFilter(true, false);
    }

    private static void release() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(DYNAMIC_ID);
            texture = null;
        }
    }
}
