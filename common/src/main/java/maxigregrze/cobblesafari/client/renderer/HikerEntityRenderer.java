package maxigregrze.cobblesafari.client.renderer;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.HikerEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HikerEntityRenderer extends MobRenderer<HikerEntity, VillagerModel<HikerEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/mob_hiker.png");

    public HikerEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(HikerEntity entity) {
        return TEXTURE;
    }
}
