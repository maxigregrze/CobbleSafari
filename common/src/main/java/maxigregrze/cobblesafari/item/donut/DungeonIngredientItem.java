package maxigregrze.cobblesafari.item.donut;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DungeonIngredientItem extends Item {

    public DungeonIngredientItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        tooltip.add(Component.translatable("tooltip.cobblesafari." + key.getPath()).withStyle(ChatFormatting.GRAY));
    }
}
