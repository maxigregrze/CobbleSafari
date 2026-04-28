package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.client.renderer.RotomPhoneDynamicBakedModel;
import maxigregrze.cobblesafari.client.renderer.RotomPhoneTextureResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public final class RotomPhoneModelLoadingPlugin {

    private static final ModelResourceLocation ROTOM_PHONE_INVENTORY = ModelResourceLocation.inventory(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "rotomphone"));

    private RotomPhoneModelLoadingPlugin() {}

    public static void register() {
        ModelLoadingPlugin.register(ctx -> {
            for (ResourceLocation rl : RotomPhoneTextureResolver.allVariantModels(
                    Minecraft.getInstance().getResourceManager())) {
                ctx.addModels(rl);
            }

            ctx.modifyModelAfterBake().register((original, context) -> {
                if (ROTOM_PHONE_INVENTORY.equals(context.topLevelId())) {
                    if (original == null) return original;
                    return new RotomPhoneDynamicBakedModel(original, RotomPhoneModelLoadingPlugin::lookup);
                }
                return original;
            });
        });
    }

    private static BakedModel lookup(ResourceLocation rl) {
        return Minecraft.getInstance().getModelManager().getModel(rl);
    }
}
