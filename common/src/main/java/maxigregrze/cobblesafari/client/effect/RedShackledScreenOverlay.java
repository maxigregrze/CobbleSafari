package maxigregrze.cobblesafari.client.effect;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.effect.RedShackledEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Red vignette matching vanilla nausea overlay (distortion-off path): additive ONE/ONE blend,
 * nausea texture stretched to screen size, transparent centre preserved via texture alpha.
 */
public final class RedShackledScreenOverlay {

    private static final ResourceLocation NAUSEA_OVERLAY =
            ResourceLocation.withDefaultNamespace("textures/misc/nausea.png");

    private RedShackledScreenOverlay() {}

    public static void renderVanillaStyle(GuiGraphics graphics, float scalar) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !RedShackledEffects.isShackled(minecraft.player)) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float scale = Mth.lerp(scalar, 2.0F, 1.0F);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(width / 2.0F, height / 2.0F, 0.0F);
        pose.scale(scale, scale, scale);
        pose.translate(-width / 2.0F, -height / 2.0F, 0.0F);

        float red = 0.45F * scalar;
        float green = 0.08F * scalar;
        float blue = 0.08F * scalar;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE
        );

        graphics.setColor(red, green, blue, 1.0F);
        graphics.blit(NAUSEA_OVERLAY, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        pose.popPose();
    }
}
