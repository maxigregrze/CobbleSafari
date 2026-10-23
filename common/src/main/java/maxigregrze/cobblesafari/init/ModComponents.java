package maxigregrze.cobblesafari.init;

import com.mojang.serialization.Codec;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.item.donut.DonutFlavorComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;

public final class ModComponents {

    private ModComponents() {}

    public static DataComponentType<DonutFlavorComponent> DONUT_FLAVOR;
    /** Target skin id of a Rotom Phone skin-unlock disc (dynamic, set per stack). */
    public static DataComponentType<String> SKIN_UNLOCK_TARGET;
    /** Game time until which a Stacked Shield rests after a block (action plan 192, section 8.2). */
    public static DataComponentType<Long> SHIELD_RECHARGE_UNTIL;
    /** Configuration copied from a Lost Item block into a Lost Item Template (action plan 193, section 5.1). */
    public static DataComponentType<CustomData> LOST_ITEM_TEMPLATE;

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

        SHIELD_RECHARGE_UNTIL = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "shield_recharge_until"),
                DataComponentType.<Long>builder()
                        .persistent(Codec.LONG)
                        .networkSynchronized(ByteBufCodecs.VAR_LONG)
                        .build()
        );

        LOST_ITEM_TEMPLATE = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "lost_item_template"),
                DataComponentType.<CustomData>builder()
                        .persistent(CustomData.CODEC)
                        .networkSynchronized(CustomData.STREAM_CODEC)
                        .build()
        );
    }
}
