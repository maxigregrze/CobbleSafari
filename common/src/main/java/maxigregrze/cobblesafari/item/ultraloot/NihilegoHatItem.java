package maxigregrze.cobblesafari.item.ultraloot;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Nihilhat (action plan 192, section 9): a wearable conduit. Every second worn, in water or rain and
 * with a non-empty food bar, it grants Conduit Power and Hunger III for three more seconds.
 * <p>On Peaceful, vanilla refills food and saturation every second, so Hunger costs nothing: the hat's
 * neurotoxin takes its place, a 4 s Nausea pulse every 20 s while the hat is active.</p>
 */
public class NihilegoHatItem extends ArmorItem {

    private static final int CHECK_INTERVAL = 20;
    private static final int EFFECT_TICKS = 60;
    private static final int HUNGER_AMPLIFIER = 2;
    private static final int NAUSEA_PERIOD = 400;
    private static final int NAUSEA_TICKS = 80;

    public NihilegoHatItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // Armor slots share their indices with the hotbar: compare the instance instead.
        if (level.isClientSide() || !(entity instanceof Player player)
                || player.getItemBySlot(EquipmentSlot.HEAD) != stack
                || player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        if (player.isInWaterOrRain() && player.getFoodData().getFoodLevel() > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, EFFECT_TICKS, 0, true, true));
            if (level.getDifficulty() != Difficulty.PEACEFUL) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, EFFECT_TICKS, HUNGER_AMPLIFIER, true, true));
            } else if (player.tickCount % NAUSEA_PERIOD < CHECK_INTERVAL) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, 0, true, true));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UltraLootTooltip.append(tooltip, "nihilego_hat");
    }
}
