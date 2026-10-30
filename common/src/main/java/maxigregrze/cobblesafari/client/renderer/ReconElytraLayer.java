package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.client.screen.warpride.WarpRideRiderProxy;
import maxigregrze.cobblesafari.item.ultraloot.ReconArmorLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Wings of the full Ultra Recon set (action plan 192, section 10.4), drawn only while gliding so
 * they never cross the backpack on the ground. Vanilla's {@code ElytraLayer} only draws for
 * {@code Items.ELYTRA}, hence this layer, added to player renderers by each loader.
 */
public class ReconElytraLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    /** The set's own wings; used as soon as the file exists in a loaded resource pack. */
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/recon_elytra.png");
    private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/elytra.png");

    private final ElytraModel<T> elytraModel;
    private final ResourceLocation texture;

    public ReconElytraLayer(RenderLayerParent<T, M> parent, EntityModelSet modelSet) {
        super(parent);
        this.elytraModel = new ElytraModel<>(modelSet.bakeLayer(ModelLayers.ELYTRA));
        // Layers are rebuilt on every resource reload, so this check follows texture packs too.
        this.texture = Minecraft.getInstance().getResourceManager().getResource(TEXTURE).isPresent()
                ? TEXTURE : FALLBACK_TEXTURE;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        // The warp ride's rider shows the wings whenever the set is complete, gliding allowed or not
        // (action plan 200, decision 2.5): it is a pose, not a flight.
        boolean show = entity instanceof WarpRideRiderProxy
                ? ReconArmorLogic.isFullSet(entity)
                : ReconArmorLogic.canGlide(entity);
        if (!entity.isFallFlying() || !show) {
            return;
        }
        pose.pushPose();
        pose.translate(0.0f, 0.0f, 0.125f);
        getParentModel().copyPropertiesTo(elytraModel);
        elytraModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        boolean foil = entity.getItemBySlot(EquipmentSlot.CHEST).hasFoil();
        VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(buffers, RenderType.armorCutoutNoCull(texture), foil);
        elytraModel.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
