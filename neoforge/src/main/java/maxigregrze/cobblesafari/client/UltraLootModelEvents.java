package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.client.renderer.UltraLootModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Bakes the Ultra Space loot projectile models (action plan 192, section 7.4). */
@EventBusSubscriber(modid = CobbleSafari.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UltraLootModelEvents {

    private UltraLootModelEvents() {}

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation id : UltraLootModels.STANDALONE) {
            event.register(ModelResourceLocation.standalone(id));
        }
        // NeoForge keys extra models as "standalone" variants.
        UltraLootModels.setStandaloneLookup(id ->
                Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(id)));
    }
}
