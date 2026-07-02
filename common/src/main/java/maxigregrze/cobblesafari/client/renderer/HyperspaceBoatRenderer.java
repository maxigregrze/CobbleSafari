package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.WaterPatchModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import org.joml.Quaternionf;

/**
 * Renders the Hyperspace boat / chest boat. Reuses the vanilla oak boat (or chest boat) geometry and
 * the Hyperspace texture. Vanilla's {@code BoatRenderer} keys its model/texture map by {@code Boat.Type}
 * (no Hyperspace entry) and its accessor is a loader-specific addition, so this is a self-contained
 * {@link EntityRenderer} mirroring the vanilla render path with a single fixed model + texture.
 */
public class HyperspaceBoatRenderer extends EntityRenderer<Boat> {

    private final ListModel<Boat> model;
    private final ResourceLocation texture;

    public HyperspaceBoatRenderer(EntityRendererProvider.Context context, boolean chestBoat) {
        super(context);
        this.shadowRadius = 0.8F;
        ModelLayerLocation layer = chestBoat
                ? ModelLayers.createChestBoatModelName(Boat.Type.OAK)
                : ModelLayers.createBoatModelName(Boat.Type.OAK);
        ModelPart part = context.bakeLayer(layer);
        this.model = chestBoat ? new ChestBoatModel(part) : new BoatModel(part);
        this.texture = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID,
                "textures/entity/" + (chestBoat ? "hyperspace_chest_boat" : "hyperspace_boat") + ".png");
    }

    @Override
    public void render(Boat entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        float hurt = (float) entity.getHurtTime() - partialTicks;
        float damage = entity.getDamage() - partialTicks;
        if (damage < 0.0F) {
            damage = 0.0F;
        }
        if (hurt > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(
                    Mth.sin(hurt) * hurt * damage / 10.0F * (float) entity.getHurtDir()));
        }

        float bubble = entity.getBubbleAngle(partialTicks);
        if (!Mth.equal(bubble, 0.0F)) {
            poseStack.mulPose(new Quaternionf().setAngleAxis(
                    bubble * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        this.model.setupAnim(entity, partialTicks, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer consumer = buffer.getBuffer(this.model.renderType(this.texture));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        if (!entity.isUnderWater() && this.model instanceof WaterPatchModel waterPatchModel) {
            VertexConsumer waterConsumer = buffer.getBuffer(RenderType.waterMask());
            waterPatchModel.waterPatch().render(poseStack, waterConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Boat entity) {
        return this.texture;
    }
}
