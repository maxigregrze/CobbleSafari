package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.client.renderer.UltraLootModels;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;

/** Bakes the Ultra Space loot projectile models (action plan 192, section 7.4). */
public final class UltraLootModelLoadingPlugin {

    private UltraLootModelLoadingPlugin() {}

    public static void register() {
        ModelLoadingPlugin.register(ctx -> ctx.addModels(UltraLootModels.STANDALONE));
        // Fabric API keys extra models by their plain id on the model manager.
        UltraLootModels.setStandaloneLookup(id -> Minecraft.getInstance().getModelManager().getModel(id));
    }
}
