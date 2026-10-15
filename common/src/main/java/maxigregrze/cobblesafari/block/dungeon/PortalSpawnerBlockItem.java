package maxigregrze.cobblesafari.block.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The Portal Spawner item: named "Portal Spawner" in the inventory while the placed block reads
 * "Portal's Shadow" in Jade / WTHIT (action plan 188, section 12.1).
 */
public class PortalSpawnerBlockItem extends BlockItem {

    public PortalSpawnerBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public String getDescriptionId() {
        return "item.cobblesafari.portal_spawner";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.cobblesafari.portal_spawner").withStyle(ChatFormatting.GRAY));
    }
}
