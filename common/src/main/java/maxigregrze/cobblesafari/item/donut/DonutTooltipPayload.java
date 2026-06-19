package maxigregrze.cobblesafari.item.donut;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.init.ModComponents;
import maxigregrze.cobblesafari.power.ItemCategoryVariantRegistry;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record DonutTooltipPayload(
        Component flavorDescription,
        List<DonutBonus> bonuses,
        int calories,
        List<ResourceLocation> berryItemIds,
        boolean shiftDetail
) implements TooltipComponent {

    private static final int FLAVOR_LINE_HEIGHT = 10;

    public static int flavorLineHeight() {
        return FLAVOR_LINE_HEIGHT;
    }

    public static Optional<DonutTooltipPayload> fromStack(ItemStack stack) {
        DonutFlavorComponent c = stack.get(ModComponents.DONUT_FLAVOR);
        if (c == null) {
            return Optional.empty();
        }
        Component flavor = Component.translatable(
                "tooltip.cobblesafari.donut." + c.flavor().getSerializedName() + "." + c.tier()
        ).withStyle(ChatFormatting.GRAY);
        boolean shift = DonutDebugClientAccess.isShiftDown();
        return Optional.of(new DonutTooltipPayload(
                flavor,
                List.copyOf(c.bonuses()),
                c.calories(),
                List.copyOf(c.inputBerries()),
                shift
        ));
    }

    public static String bonusEffectIdPath(String powerId, int level, int typeIndex) {
        DonutPower power = DonutPowerRegistry.get(powerId);
        if (power == null || power.typeNbr() <= 1) {
            return powerId + "_" + level;
        }
        String suffix;
        if ("item".equals(powerId)) {
            int idx = Math.floorMod(typeIndex, ItemCategoryVariantRegistry.COUNT);
            suffix = ItemCategoryVariantRegistry.suffix(idx);
        } else {
            int idx = Math.floorMod(typeIndex, PowerVariantRegistry.VARIANT_COUNT);
            suffix = PowerVariantRegistry.suffix(idx);
        }
        return powerId + "_" + suffix + "_" + level;
    }

    public static Component bonusEffectDescription(DonutBonus bonus) {
        String path = bonusEffectIdPath(bonus.powerId(), bonus.level(), bonus.type());
        return Component.translatable("effect." + CobbleSafari.MOD_ID + "." + path);
    }

    public static ResourceLocation bonusTextureLocation(String powerId, int level, int typeIndex) {
        String fileName = bonusEffectIdPath(powerId, level, typeIndex);
        return ResourceLocation.fromNamespaceAndPath(
                "cobblesafari",
                "textures/tooltip/" + fileName + ".png"
        );
    }
}
