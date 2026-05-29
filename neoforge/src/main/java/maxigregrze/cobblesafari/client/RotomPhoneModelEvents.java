package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.client.renderer.RotomPhoneDynamicBakedModel;
import maxigregrze.cobblesafari.client.renderer.RotomPhoneTextureResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = CobbleSafari.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RotomPhoneModelEvents {

    private static final ModelResourceLocation ROTOM_PHONE_INVENTORY = ModelResourceLocation.inventory(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "rotomphone"));

    private RotomPhoneModelEvents() {}

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation rl : RotomPhoneTextureResolver.allVariantModels(
                Minecraft.getInstance().getResourceManager())) {
            event.register(ModelResourceLocation.standalone(rl));
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModel original = event.getModels().get(ROTOM_PHONE_INVENTORY);
        if (original == null) return;
        event.getModels().put(ROTOM_PHONE_INVENTORY, new RotomPhoneDynamicBakedModel(original, RotomPhoneModelEvents::lookup));
    }

    private static BakedModel lookup(ResourceLocation rl) {
        ModelResourceLocation mrl = ModelResourceLocation.standalone(rl);
        return Minecraft.getInstance().getModelManager().getModel(mrl);
    }
}
