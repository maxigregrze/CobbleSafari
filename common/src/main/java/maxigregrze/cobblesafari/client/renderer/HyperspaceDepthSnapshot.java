package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/**
 * A copy of the main target's depth, readable as a texture (action plan 197, section 4.1).
 *
 * <p>The copy is a depth blit, which requires both sides to share one format. The format is read
 * from the main framebuffer's attachments rather than assumed: NeoForge can give the main target a
 * stencil buffer, and the driver picks the precision of the vanilla unsized depth texture.</p>
 *
 * <p>Copies run in the middle of the Iris pipeline, so every framebuffer and texture binding this
 * class touches is put back as it was found.</p>
 */
final class HyperspaceDepthSnapshot {

    private int framebuffer = -1;
    private int texture = -1;
    private int width;
    private int height;
    private int internalFormat;

    int textureId() {
        return texture;
    }

    void copyFrom(RenderTarget main) {
        int previousRead = GlStateManager._getInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GlStateManager._getInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        int format = readDepthFormat();
        if (texture == -1 || width != main.width || height != main.height || internalFormat != format) {
            allocate(main.width, main.height, format);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
        GlStateManager._glBlitFrameBuffer(0, 0, width, height, 0, 0, width, height,
                GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
    }

    /** Sized internal format of the depth attached to the bound read framebuffer. */
    private static int readDepthFormat() {
        boolean stencil = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_READ_FRAMEBUFFER,
                GL30.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE) != GL11.GL_NONE;
        boolean floating = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_READ_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE) == GL11.GL_FLOAT;
        if (stencil) {
            return floating ? GL30.GL_DEPTH32F_STENCIL8 : GL30.GL_DEPTH24_STENCIL8;
        }
        if (floating) {
            return GL30.GL_DEPTH_COMPONENT32F;
        }
        int bits = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_READ_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE);
        return switch (bits) {
            case 16 -> GL14.GL_DEPTH_COMPONENT16;
            case 32 -> GL14.GL_DEPTH_COMPONENT32;
            default -> GL14.GL_DEPTH_COMPONENT24;
        };
    }

    /** Leaves the new framebuffer bound for drawing; {@link #copyFrom} restores the bindings. */
    private void allocate(int newWidth, int newHeight, int format) {
        release();
        width = newWidth;
        height = newHeight;
        internalFormat = format;

        boolean stencil = format == GL30.GL_DEPTH24_STENCIL8 || format == GL30.GL_DEPTH32F_STENCIL8;
        int pixelFormat = stencil ? GL30.GL_DEPTH_STENCIL : GL11.GL_DEPTH_COMPONENT;
        int pixelType;
        if (format == GL30.GL_DEPTH24_STENCIL8) {
            pixelType = GL30.GL_UNSIGNED_INT_24_8;
        } else if (format == GL30.GL_DEPTH32F_STENCIL8) {
            pixelType = GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV;
        } else {
            pixelType = GL11.GL_FLOAT;
        }

        int previousTexture = GlStateManager._getInteger(GL11.GL_TEXTURE_BINDING_2D);
        texture = GlStateManager._genTexture();
        GlStateManager._bindTexture(texture);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
        GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, format, width, height, 0, pixelFormat, pixelType, null);
        GlStateManager._bindTexture(previousTexture);

        framebuffer = GlStateManager.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
        GlStateManager._glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER,
                stencil ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D, texture, 0);
        GL11.glDrawBuffer(GL11.GL_NONE);
    }

    private void release() {
        if (framebuffer != -1) {
            GlStateManager._glDeleteFramebuffers(framebuffer);
            framebuffer = -1;
        }
        if (texture != -1) {
            GlStateManager._deleteTexture(texture);
            texture = -1;
        }
    }
}
