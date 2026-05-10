package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.item.donut.DonutFlavorComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ModComponents {

    private ModComponents() {}

    public static DataComponentType<DonutFlavorComponent> DONUT_FLAVOR;

    public static void register() {
        CobbleSafari.LOGGER.info("Registering data components for " + CobbleSafari.MOD_ID);

        DONUT_FLAVOR = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "donut_flavor"),
                DataComponentType.<DonutFlavorComponent>builder()
                        .persistent(DonutFlavorComponent.CODEC)
                        .networkSynchronized(DonutFlavorComponent.STREAM_CODEC)
                        .cacheEncoding()
                        .build()
        );
    }
}
