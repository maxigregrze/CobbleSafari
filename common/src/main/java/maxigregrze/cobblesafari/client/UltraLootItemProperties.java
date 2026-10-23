package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.item.ultraloot.UltraCanonItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Item model properties of the Ultra Space loot, registered by each loader (action plan 192). */
public final class UltraLootItemProperties {

    /** Id of the canons' unloaded-state property: their models switch to the {@code _empty} texture. */
    public static final String CANON_EMPTY = "empty";

    private UltraLootItemProperties() {}

    public static float canonEmpty(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return UltraCanonItem.isCharged(stack) ? 0.0f : 1.0f;
    }
}
