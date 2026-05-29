package maxigregrze.cobblesafari.compat.accessories;

import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.item.RotomPhoneItem;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneSafetyTick;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class RotomPhoneAccessory implements Accessory {

    @Override
    public boolean canEquip(ItemStack stack, SlotReference reference) {
        return stack.is(ModItems.ROTOM_PHONE);
    }

    @Override
    public void tick(ItemStack stack, SlotReference reference) {
        LivingEntity entity = reference.entity();
        if (entity.level().isClientSide() || !(entity instanceof ServerPlayer player)) return;
        if (!RotomPhoneItem.isSafetyMode(stack)) return;
        RotomPhoneSafetyTick.tryApplyRotoFall(player);
    }
}
