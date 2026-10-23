package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.client.renderer.LostItemModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Bakes the floating Lost Item aura model (action plan 193, section 7.3). */
@EventBusSubscriber(modid = CobbleSafari.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LostItemModelEvents {

    private LostItemModelEvents() {}

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation id : LostItemModels.STANDALONE) {
            event.register(ModelResourceLocation.standalone(id));
        }
        // NeoForge keys extra models as "standalone" variants.
        LostItemModels.setStandaloneLookup(id ->
                Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(id)));
    }
}
