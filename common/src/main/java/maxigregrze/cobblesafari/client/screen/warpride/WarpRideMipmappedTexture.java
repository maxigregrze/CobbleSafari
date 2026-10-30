package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * A GUI texture with a real mipmap chain and trilinear filtering (action plan 200, section 4.1).
 *
 * <p>A {@code SimpleTexture} uploads level 0 only, so a sprite drawn far from its native size is
 * resampled by plain bilinear filtering: soft when magnified, shimmering when minified. The pickup
 * rings are scaled every frame between 80 % and 125 % on a canvas that is itself fitted to the
 * window, so both cases happen. With the chain, a source exported at a higher resolution than the
 * ring ever covers on screen stays sharp on large windows and clean on small ones.</p>
 */
public final class WarpRideMipmappedTexture extends AbstractTexture {

    private static final int MAX_LEVELS = 4;
    /** No level below this many px on its short side: tiny levels only average the art away. */
    private static final int MIN_LEVEL_SIZE = 32;

    private final ResourceLocation location;

    public WarpRideMipmappedTexture(ResourceLocation location) {
        this.location = location;
    }

    @Override
    public void load(ResourceManager manager) throws IOException {
        NativeImage image;
        try (InputStream in = manager.open(location)) {
            image = NativeImage.read(in);
        }
        int levels = mipLevels(image.getWidth(), image.getHeight());
        NativeImage[] chain = MipmapGenerator.generateMipLevels(new NativeImage[]{image}, levels);
        if (RenderSystem.isOnRenderThreadOrInit()) {
            upload(chain, levels);
        } else {
            RenderSystem.recordRenderCall(() -> upload(chain, levels));
        }
    }

    /** Halvings that keep both sides even and at least {@link #MIN_LEVEL_SIZE}. */
    private static int mipLevels(int width, int height) {
        int levels = 0;
        while (levels < MAX_LEVELS && width % 2 == 0 && height % 2 == 0
                && Math.min(width, height) / 2 >= MIN_LEVEL_SIZE) {
            width /= 2;
            height /= 2;
            levels++;
        }
        return levels;
    }

    private void upload(NativeImage[] chain, int levels) {
        TextureUtil.prepareImage(getId(), levels, chain[0].getWidth(), chain[0].getHeight());
        for (int level = 0; level <= levels; level++) {
            // blur + mipmap: GL_LINEAR_MIPMAP_LINEAR / GL_LINEAR. autoClose frees each level.
            chain[level].upload(level, 0, 0, 0, 0, chain[level].getWidth(), chain[level].getHeight(),
                    true, true, levels > 0, true);
        }
    }
}
