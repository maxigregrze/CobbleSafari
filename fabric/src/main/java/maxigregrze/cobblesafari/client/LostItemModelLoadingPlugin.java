package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.client.renderer.LostItemModels;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;

/** Bakes the floating Lost Item aura model (action plan 193, section 7.3). */
public final class LostItemModelLoadingPlugin {

    private LostItemModelLoadingPlugin() {}

    public static void register() {
        ModelLoadingPlugin.register(ctx -> ctx.addModels(LostItemModels.STANDALONE));
        // Fabric API keys extra models by their plain id on the model manager.
        LostItemModels.setStandaloneLookup(id -> Minecraft.getInstance().getModelManager().getModel(id));
    }
}
