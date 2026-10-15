package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.client.renderer.UltraWarpDiveModels;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;

/** Bakes the Ultra Warp Dive models no blockstate references (action plan 189, section 3.5). */
public final class UltraWarpDiveModelLoadingPlugin {

    private UltraWarpDiveModelLoadingPlugin() {}

    public static void register() {
        ModelLoadingPlugin.register(ctx -> ctx.addModels(UltraWarpDiveModels.STANDALONE));
        // Fabric API keys extra models by their plain id on the model manager.
        UltraWarpDiveModels.setStandaloneLookup(id -> Minecraft.getInstance().getModelManager().getModel(id));
    }
}
