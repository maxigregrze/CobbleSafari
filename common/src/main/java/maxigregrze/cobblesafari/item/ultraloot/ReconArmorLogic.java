package maxigregrze.cobblesafari.item.ultraloot;

import maxigregrze.cobblesafari.config.MiscConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Elytra behaviour of the full Ultra Recon set (action plan 192, section 10.4), shared by the
 * Fabric elytra event and the NeoForge chestplate subclass.
 * <p>The client decides on its own to start gliding, so it needs the server's
 * {@code enableUltraReconElytra}: {@code UltraLootConfigSyncPayload} stores it here.</p>
 */
public final class ReconArmorLogic {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static volatile boolean clientElytraEnabled = true;

    private ReconArmorLogic() {}

    public static void setClientElytraEnabled(boolean enabled) {
        clientElytraEnabled = enabled;
    }

    public static boolean isClientElytraEnabled() {
        return clientElytraEnabled;
    }

    public static boolean isFullSet(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!(entity.getItemBySlot(slot).getItem() instanceof ReconArmorItem)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canGlide(LivingEntity entity) {
        boolean enabled = entity.level().isClientSide() ? clientElytraEnabled : MiscConfig.isUltraReconElytraEnabled();
        if (!enabled || !isFullSet(entity)) {
            return false;
        }
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getDamageValue() < chest.getMaxDamage() - 1;
    }

    /** Vanilla elytra wear: one point per second of flight, a glide game event every half second. */
    public static void flightTick(LivingEntity entity, int flightTicks) {
        if (entity.level().isClientSide()) {
            return;
        }
        int next = flightTicks + 1;
        if (next % 10 == 0) {
            if (next % 20 == 0) {
                entity.getItemBySlot(EquipmentSlot.CHEST).hurtAndBreak(1, entity, EquipmentSlot.CHEST);
            }
            entity.gameEvent(GameEvent.ELYTRA_GLIDE);
        }
    }
}
