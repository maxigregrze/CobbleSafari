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
    private static final ModelResourceLocation ROTOM_EARPIECE_INVENTORY = ModelResourceLocation.inventory(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "rotom_earpiece"));

    private RotomPhoneModelEvents() {}

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation rl : RotomPhoneTextureResolver.allVariantModels(
                Minecraft.getInstance().getResourceManager())) {
            event.register(ModelResourceLocation.standalone(rl));
        }
        for (ResourceLocation rl : RotomPhoneTextureResolver.allEarpieceVariantModels(
                Minecraft.getInstance().getResourceManager())) {
            event.register(ModelResourceLocation.standalone(rl));
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModel phone = event.getModels().get(ROTOM_PHONE_INVENTORY);
        if (phone != null) {
            event.getModels().put(ROTOM_PHONE_INVENTORY, new RotomPhoneDynamicBakedModel(
                    phone, RotomPhoneModelEvents::lookup, RotomPhoneTextureResolver::modelForStack));
        }
        BakedModel earpiece = event.getModels().get(ROTOM_EARPIECE_INVENTORY);
        if (earpiece != null) {
            event.getModels().put(ROTOM_EARPIECE_INVENTORY, new RotomPhoneDynamicBakedModel(
                    earpiece, RotomPhoneModelEvents::lookup, RotomPhoneTextureResolver::earpieceModelForStack));
        }
    }

    private static BakedModel lookup(ResourceLocation rl) {
        ModelResourceLocation mrl = ModelResourceLocation.standalone(rl);
        return Minecraft.getInstance().getModelManager().getModel(mrl);
    }
}
