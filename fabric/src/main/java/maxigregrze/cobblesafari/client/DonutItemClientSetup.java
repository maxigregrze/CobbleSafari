package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.init.ModComponents;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.item.donut.DonutDebugClientAccess;
import maxigregrze.cobblesafari.item.donut.DonutFlavorComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

public final class DonutItemClientSetup {

    private static final float MAX_VARIANT_CODE = 35f;

    private DonutItemClientSetup() {}

    public static void registerItemProperties() {
        DonutDebugClientAccess.setShiftKeyDownSupplier(Screen::hasShiftDown);
        ItemProperties.register(
                ModItems.DONUT,
                ResourceLocation.fromNamespaceAndPath("cobblesafari", "donut_seasoned"),
                (stack, level, entity, seed) -> {
                    DonutFlavorComponent c = stack.get(ModComponents.DONUT_FLAVOR);
                    return c == null ? 0f : 1f;
                });
        ItemProperties.register(
                ModItems.DONUT,
                ResourceLocation.fromNamespaceAndPath("cobblesafari", "donut_variant"),
                (stack, level, entity, seed) -> {
                    DonutFlavorComponent c = stack.get(ModComponents.DONUT_FLAVOR);
                    if (c == null) {
                        return 0f;
                    }
                    int code = c.flavor().getIndex() * (DonutFlavorComponent.MAX_TIER + 1) + c.tier();
                    return code / MAX_VARIANT_CODE;
                });
    }
}
