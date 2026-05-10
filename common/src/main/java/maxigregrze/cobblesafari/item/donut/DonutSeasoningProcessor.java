package maxigregrze.cobblesafari.item.donut;

import com.cobblemon.mod.common.api.cooking.Flavour;
import com.cobblemon.mod.common.api.cooking.Seasonings;
import com.cobblemon.mod.common.item.crafting.SeasoningProcessor;
import maxigregrze.cobblesafari.init.ModComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DonutSeasoningProcessor implements SeasoningProcessor {

    public static final String TYPE = "cobblesafari:donut_flavour";
    public static final DonutSeasoningProcessor INSTANCE = new DonutSeasoningProcessor();

    private DonutSeasoningProcessor() {}

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void apply(ItemStack result, List<ItemStack> seasoning) {
        Map<Flavour, Integer> totals = new EnumMap<>(Flavour.class);
        List<ResourceLocation> inputBerries = new ArrayList<>();
        for (ItemStack stack : seasoning) {
            if (!stack.isEmpty() && inputBerries.size() < 3) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (key != null) {
                    inputBerries.add(key);
                }
            }
            Map<Flavour, Integer> flavours = Seasonings.INSTANCE.getFlavoursFromItemStack(stack);
            if (flavours == null || flavours.isEmpty()) {
                continue;
            }
            for (Map.Entry<Flavour, Integer> entry : flavours.entrySet()) {
                totals.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        DonutFlavorComponent component = DonutFlavorLogic.fromCooking(totals, List.copyOf(inputBerries));
        if (component != null) {
            result.set(ModComponents.DONUT_FLAVOR, component);
        }
    }

    @Override
    public boolean consumesItem(ItemStack seasoning) {
        return Seasonings.INSTANCE.hasFlavors(seasoning);
    }

    public static void register() {
        SeasoningProcessor.Companion.getProcessors().put(TYPE, INSTANCE);
    }
}
