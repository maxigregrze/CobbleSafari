package maxigregrze.cobblesafari.item;

import maxigregrze.cobblesafari.item.ultraloot.StakatakaShieldItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * NeoForge's {@code LivingEntity#isBlocking} requires the {@code shield_block} ability, which the
 * common class cannot declare (action plan 192, section 8.1).
 */
public class NeoForgeStakatakaShieldItem extends StakatakaShieldItem {

    public NeoForgeStakatakaShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return ItemAbilities.DEFAULT_SHIELD_ACTIONS.contains(itemAbility);
    }
}
