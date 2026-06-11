package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.projectile.ThrownRedChainEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ThrownRedChainEntityRenderer extends EntityRenderer<ThrownRedChainEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_redchain.png");
    private static final float HALF_SIZE = 1.5f;
    private static final float SPIN_DEGREES_PER_TICK = 6.0f;

    public ThrownRedChainEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownRedChainEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(ThrownRedChainEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));

        var vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        float spin = (entity.tickCount + partialTicks) * SPIN_DEGREES_PER_TICK;
        RedChainBillboardHelper.renderHorizontalSpinningQuad(vc, ps, HALF_SIZE, spin, LightTexture.FULL_BRIGHT);
        ps.popPose();

        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
