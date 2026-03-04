package maxigregrze.cobblesafari.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.Holder;

import java.util.List;

public class LuckyMiningHelmetItem extends ArmorItem {

    private static final String TOOLTIP_KEY = "tooltip.cobblesafari.lucky_mining_helmet";

    public LuckyMiningHelmetItem(Holder<ArmorMaterial> material, Item.Properties properties) {
        super(material, Type.HELMET, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(TOOLTIP_KEY).withStyle(ChatFormatting.GRAY));
    }

    public static void tickEffect(Player player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof LuckyMiningHelmetItem) {
            if (!player.hasEffect(MobEffects.LUCK)) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 40, 0, false, false));
            }
        }
    }
}
