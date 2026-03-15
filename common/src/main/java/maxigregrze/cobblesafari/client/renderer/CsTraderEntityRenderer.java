package maxigregrze.cobblesafari.client.renderer;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.CsTraderEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CsTraderEntityRenderer extends MobRenderer<CsTraderEntity, VillagerModel<CsTraderEntity>> {

    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/mob_hiker.png");

    public CsTraderEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(CsTraderEntity entity) {
        String textureFile = entity.getTextureFile();
        if (textureFile == null || textureFile.isBlank()) {
            return FALLBACK_TEXTURE;
        }
        return ResourceLocation.fromNamespaceAndPath(
                CobbleSafari.MOD_ID,
                "textures/entity/mob_" + textureFile + ".png"
        );
    }
}
