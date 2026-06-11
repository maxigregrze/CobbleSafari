package maxigregrze.cobblesafari.item.redchain;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RedChainCoreItem extends RedChainNamedItem {

    public RedChainCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cobblesafari.red_chain_core.line1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cobblesafari.red_chain_core.line2").withStyle(ChatFormatting.GRAY));
    }
}
