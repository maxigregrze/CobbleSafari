package maxigregrze.cobblesafari.event;

import maxigregrze.cobblesafari.item.ultraloot.StakatakaShieldItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side damage hook of the Ultra Space loot (action plan 192, section 8.3), called from the
 * loaders' existing cancellable damage events, which both fire before vanilla's shield logic.
 */
public final class UltraLootDamageHandler {

    private UltraLootDamageHandler() {}

    /** @return {@code false} to cancel the damage entirely */
    public static boolean onLivingHurt(LivingEntity entity, DamageSource source) {
        if (entity.level().isClientSide() || !(entity instanceof Player player) || !player.isBlocking()) {
            return true;
        }
        ItemStack used = player.getUseItem();
        if (!(used.getItem() instanceof StakatakaShieldItem) || !player.isDamageSourceBlocked(source)) {
            return true;
        }
        StakatakaShieldItem.onBlocked(player, used);
        return false;
    }
}
