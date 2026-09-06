package maxigregrze.cobblesafari.cstrader.logic;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One data component to stamp onto a trade result, as authored in a {@code cstrader} JSON:
 * {@code { "dataId": "cobblemon:tm_move", "dataValue": { "move": "thunderbolt" } }}.
 *
 * <p>{@code dataValue} is kept as raw JSON and only decoded when the offer is built, so a component
 * whose codec needs registries that are not ready at datapack-load time still works.
 */
public record CsTraderComponentSpec(ResourceLocation dataId, JsonElement dataValue) {

    /** Applies this component to the stack, logging and skipping rather than throwing on bad data. */
    public void applyTo(ItemStack stack) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(dataId);
        if (type == null) {
            CobbleSafari.LOGGER.warn("[CSTrader] Unknown data component '{}', skipped", dataId);
            return;
        }
        applyTyped(stack, type);
    }

    private <T> void applyTyped(ItemStack stack, DataComponentType<T> type) {
        Codec<T> codec = type.codec();
        if (codec == null) {
            CobbleSafari.LOGGER.warn("[CSTrader] Data component '{}' is transient and cannot be "
                    + "written to a trade result, skipped", dataId);
            return;
        }
        codec.parse(JsonOps.INSTANCE, dataValue)
                .resultOrPartial(error -> CobbleSafari.LOGGER.warn(
                        "[CSTrader] Could not read value for data component '{}': {}", dataId, error))
                .ifPresent(value -> stack.set(type, value));
    }
}
