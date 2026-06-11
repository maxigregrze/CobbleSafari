package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackRedChainEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Rend le projectile « chaîne rouge » ({@code distortion_5}) comme le projectile de l'objet Chaîne
 * Rouge : un disque horizontal (parallèle au sol) qui tourne lentement, texture
 * {@code attack_redchain.png}. Réutilise {@link RedChainBillboardHelper}.
 */
public class AttackRedChainEntityRenderer extends EntityRenderer<AttackRedChainEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_redchain.png");
    private static final float HALF_SIZE = 1.5f;
    private static final float SPIN_DEGREES_PER_TICK = 6.0f;

    public AttackRedChainEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackRedChainEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(AttackRedChainEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        ps.pushPose();
        var vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        float spin = (entity.tickCount + partialTicks) * SPIN_DEGREES_PER_TICK;
        RedChainBillboardHelper.renderHorizontalSpinningQuad(vc, ps, HALF_SIZE, spin, LightTexture.FULL_BRIGHT);
        ps.popPose();

        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
