package maxigregrze.cobblesafari.item;

import maxigregrze.cobblesafari.item.ultraloot.ReconArmorItem;
import maxigregrze.cobblesafari.item.ultraloot.ReconArmorLogic;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge drives elytra flight through the chest item's {@code IItemExtension} methods; Fabric uses
 * its elytra event instead (action plan 192, section 2.1).
 */
public class NeoForgeReconChestplateItem extends ReconArmorItem {

    public NeoForgeReconChestplateItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return ReconArmorLogic.canGlide(entity);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        ReconArmorLogic.flightTick(entity, flightTicks);
        return true;
    }
}
