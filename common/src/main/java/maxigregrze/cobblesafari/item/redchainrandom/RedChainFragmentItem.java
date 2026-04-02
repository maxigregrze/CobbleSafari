package maxigregrze.cobblesafari.item.redchainrandom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RedChainFragmentItem extends Item {
    public RedChainFragmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.RED);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cobblesafari.redchain_fragment.line1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cobblesafari.redchain_fragment.line2").withStyle(ChatFormatting.GRAY));
    }
}
