package maxigregrze.cobblesafari.client.renderer;

import maxigregrze.cobblesafari.entity.HikerEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HikerEntityRenderer extends MobRenderer<HikerEntity, VillagerModel<HikerEntity>> {

    public HikerEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(CsTraderEntityRenderer.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(HikerEntity entity) {
        return CsTraderEntityRenderer.textureFor(entity.getTextureFile());
    }
}
