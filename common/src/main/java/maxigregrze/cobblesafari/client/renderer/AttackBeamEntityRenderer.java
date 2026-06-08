package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackBeamEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Renders {@link AttackBeamEntity} as a vanilla beacon beam aimed along the entity's view direction
 * (yaw/pitch). Reusable for any directional "beacon beam" effect.
 */
public class AttackBeamEntityRenderer extends EntityRenderer<AttackBeamEntity> {

    public AttackBeamEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackBeamEntity entity) {
        return BeaconRenderer.BEAM_LOCATION;
    }

    @Override
    public boolean shouldRender(AttackBeamEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                               double camX, double camY, double camZ) {
        return true; // the beam is long; never cull it on its small origin box
    }

    @Override
    public void render(AttackBeamEntity beam, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 dir = beam.getViewVector(partialTicks).normalize();
        long gameTime = beam.level().getGameTime();

        ps.pushPose();
        // The beacon beam is drawn along +Y; rotate so +Y maps onto the beam's aim direction.
        ps.mulPose(new Quaternionf().rotateTo(0.0f, 1.0f, 0.0f, (float) dir.x, (float) dir.y, (float) dir.z));
        BeaconRenderer.renderBeaconBeam(ps, buffer, BeaconRenderer.BEAM_LOCATION, partialTicks, 1.0f,
                gameTime, 0, beam.getLength(), beam.getColor(), 0.35f, 0.45f);
        ps.popPose();

        super.render(beam, yaw, partialTicks, ps, buffer, packedLight);
    }
}
