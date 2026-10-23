package maxigregrze.cobblesafari.item.ultraloot;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Ultra Recon armor piece (action plan 192, section 10). The full set glides like an elytra when
 * {@code enableUltraReconElytra} is on - see {@link ReconArmorLogic}.
 */
public class ReconArmorItem extends ArmorItem {

    public ReconArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UltraLootTooltip.append(tooltip, "recon_armor", ReconArmorLogic.isClientElytraEnabled()
                ? "tooltip.cobblesafari.recon_armor.line2_elytra"
                : "tooltip.cobblesafari.recon_armor.line2");
    }
}
