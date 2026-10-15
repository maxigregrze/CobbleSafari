package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.client.renderer.UltraWarpDiveModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Bakes the Ultra Warp Dive models no blockstate references (action plan 189, section 3.5). */
@EventBusSubscriber(modid = CobbleSafari.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UltraWarpDiveModelEvents {

    private UltraWarpDiveModelEvents() {}

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation id : UltraWarpDiveModels.STANDALONE) {
            event.register(ModelResourceLocation.standalone(id));
        }
        // NeoForge keys extra models as "standalone" variants.
        UltraWarpDiveModels.setStandaloneLookup(id ->
                Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(id)));
    }
}
