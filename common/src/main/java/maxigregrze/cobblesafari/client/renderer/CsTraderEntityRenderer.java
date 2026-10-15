package maxigregrze.cobblesafari.client.renderer;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.CsTraderEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CsTraderEntityRenderer extends MobRenderer<CsTraderEntity, VillagerModel<CsTraderEntity>> {

    /**
     * Dedicated layer with the vanilla villager geometry. Model-replacing packs (Fresh Animations via
     * EMF) swap models per layer, so baking {@code ModelLayers.VILLAGER} would hand our traders the
     * pack's villager model; this layer has no replacement and keeps the vanilla look (plan 188, C1).
     */
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "cstrader"), "main");

    static final String TEXTURE_FOLDER = "textures/entity/cstrader/";

    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, TEXTURE_FOLDER + "mob_hiker.png");

    public CsTraderEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(LAYER_LOCATION)), 0.5f);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(VillagerModel.createBodyModel(), 64, 64);
    }

    @Override
    public ResourceLocation getTextureLocation(CsTraderEntity entity) {
        return textureFor(entity.getTextureFile());
    }

    static ResourceLocation textureFor(String textureFile) {
        if (textureFile == null || textureFile.isBlank()) {
            return FALLBACK_TEXTURE;
        }
        return ResourceLocation.fromNamespaceAndPath(
                CobbleSafari.MOD_ID,
                TEXTURE_FOLDER + "mob_" + textureFile + ".png"
        );
    }
}
