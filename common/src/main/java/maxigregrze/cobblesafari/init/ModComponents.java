package maxigregrze.cobblesafari.init;

import com.mojang.serialization.Codec;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.item.donut.DonutFlavorComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

public final class ModComponents {

    private ModComponents() {}

    public static DataComponentType<DonutFlavorComponent> DONUT_FLAVOR;
    /** Target skin id of a Rotom Phone skin-unlock disc (dynamic, set per stack). */
    public static DataComponentType<String> SKIN_UNLOCK_TARGET;

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

        SKIN_UNLOCK_TARGET = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "skin_unlock_target"),
                DataComponentType.<String>builder()
                        .persistent(Codec.STRING)
                        .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                        .cacheEncoding()
                        .build()
        );
    }
}
