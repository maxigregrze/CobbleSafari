package maxigregrze.cobblesafari.compat.accessories;

import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.init.ModItems;

public class AccessoriesClientCompat {
    private AccessoriesClientCompat() {}

    public static void init() {
        AccessoriesRendererRegistry.registerRenderer(ModItems.ROTOM_PHONE, RotomPhoneAccessoryRenderer::new);
        AccessoriesRendererRegistry.registerRenderer(ModItems.ROTOM_EARPIECE, RotomEarpieceAccessoryRenderer::new);
        CobbleSafari.LOGGER.info("Registered Rotom Phone + Rotie Earpiece accessory renderers");
    }
}