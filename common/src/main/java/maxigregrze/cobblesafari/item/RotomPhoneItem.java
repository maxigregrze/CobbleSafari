package maxigregrze.cobblesafari.item;

import maxigregrze.cobblesafari.rotomphone.RotomPhoneSkinDefinition;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneSkinRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

public class RotomPhoneItem extends Item {

    public RotomPhoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            maxigregrze.cobblesafari.rotomphone.RotomPhoneServerHandler.openPhone(serverPlayer, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
        if (!isSafetyMode(stack)) return;

        boolean inHand = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!inHand) return;

        if (player.fallDistance > 1.0f && player.getDeltaMovement().y < -0.1) {
            if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false, true));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String name = getRotomName(stack);
        if (!name.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.cobblesafari.rotomphone.name", name)
                    .withStyle(Style.EMPTY.withColor(0x7AFFFF)));
        }
        if (isShiny(stack)) {
            tooltip.add(Component.translatable("tooltip.cobblesafari.rotomphone.shiny")
                    .withStyle(Style.EMPTY.withColor(0xFFFFC0)));
        }
        String skin = getCurrentSkin(stack);
        if (!skin.isEmpty()) {
            RotomPhoneSkinDefinition def = RotomPhoneSkinRegistry.getSkin(skin);
            String displayName = def != null ? def.getDisplayName() : skin;
            tooltip.add(Component.translatable("tooltip.cobblesafari.rotomphone.skin", displayName));
        }
        if (isSafetyMode(stack)) {
            tooltip.add(Component.translatable("tooltip.cobblesafari.rotomphone.safety_on")
                    .withStyle(Style.EMPTY.withColor(0x55FF55)));
        } else {
            tooltip.add(Component.translatable("tooltip.cobblesafari.rotomphone.safety_off")
                    .withStyle(Style.EMPTY.withColor(0xAAAAAA)));
        }
    }

    public static String getRotomName(ItemStack stack) {
        CompoundTag tag = getPhoneTag(stack);
        return tag.getString("name");
    }

    public static boolean isShiny(ItemStack stack) {
        CompoundTag tag = getPhoneTag(stack);
        return tag.getBoolean("shinyStatus");
    }

    public static String getCurrentSkin(ItemStack stack) {
        CompoundTag tag = getPhoneTag(stack);
        return tag.getString("currentSkin");
    }

    public static boolean isSafetyMode(ItemStack stack) {
        CompoundTag tag = getPhoneTag(stack);
        return tag.getBoolean("safetyMode");
    }

    public static void setRotomName(ItemStack stack, String name) {
        modifyPhoneTag(stack, tag -> tag.putString("name", name));
    }

    public static void setShiny(ItemStack stack, boolean shiny) {
        modifyPhoneTag(stack, tag -> tag.putBoolean("shinyStatus", shiny));
    }

    public static void setCurrentSkin(ItemStack stack, String skin) {
        modifyPhoneTag(stack, tag -> tag.putString("currentSkin", skin));
    }

    public static void setSafetyMode(ItemStack stack, boolean mode) {
        modifyPhoneTag(stack, tag -> tag.putBoolean("safetyMode", mode));
    }

    private static CompoundTag getPhoneTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return new CompoundTag();
        return data.copyTag();
    }

    private static void modifyPhoneTag(ItemStack stack, java.util.function.Consumer<CompoundTag> modifier) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        modifier.accept(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
