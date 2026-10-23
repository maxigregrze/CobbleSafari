package maxigregrze.cobblesafari.item.ultraloot;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Glutton armor piece (action plan 192, section 11): eating while wearing it grants Gluttonous. */
public class GlutonArmorItem extends ArmorItem {

    public GlutonArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UltraLootTooltip.append(tooltip, "gluton_armor");
    }
}
