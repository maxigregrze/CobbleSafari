package maxigregrze.cobblesafari.item.donut;

import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class SurpriseDonutItem extends Item {

    private final DonutMainFlavor flavor;
    private final int tier;

    public SurpriseDonutItem(Properties properties, DonutMainFlavor flavor, int tier) {
        super(properties);
        this.flavor = flavor;
        this.tier = tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        Style style = flavor == null ? Style.EMPTY : nameStyleForTier(tier);
        return Component.translatable("item.cobblesafari.donut_random").withStyle(style);
    }

    private static Style nameStyleForTier(int tier) {
        if (tier >= 4) {
            return Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);
        }
        if (tier >= 2) {
            return Style.EMPTY.withColor(ChatFormatting.AQUA);
        }
        return Style.EMPTY.withColor(ChatFormatting.YELLOW);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (flavor == null) {
            tooltip.add(Component.translatable("tooltip.cobblesafari.donut_random.blank")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        Component flavorName = Component.translatable(
                "tooltip.cobblesafari.donut_random.flavor." + flavor.getSerializedName());
        Style style = nameStyleForTier(tier);
        tooltip.add(Component.translatable("tooltip.cobblesafari.donut_random.contents", flavorName, tier)
                .withStyle(style));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        ItemStack result;
        if (flavor == null) {
            result = new ItemStack(ModItems.DONUT);
        } else if (flavor == DonutMainFlavor.MIX) {
            result = DonutFlavorLogic.createGeneratedMixStack(tier);
        } else {
            result = DonutFlavorLogic.createGeneratedStack(flavor, tier);
        }

        // createFilledResult consumes one surprise donut and returns the generated donut into the
        // same hand slot when the stack empties; otherwise it adds to the inventory (or drops it).
        // This replaces the consumed item in place and avoids losing the donut when the hotbar is full.
        ItemStack remainder = ItemUtils.createFilledResult(stack, player, result, false);
        return InteractionResultHolder.sidedSuccess(remainder, false);
    }
}
