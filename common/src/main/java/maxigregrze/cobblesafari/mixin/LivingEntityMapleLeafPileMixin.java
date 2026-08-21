package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.block.nature.MapleLeafPileBlock;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Landing in a deep drift of maple leaves hurts less.
 *
 * <p>This cannot be done with {@code Block#fallOn}. The leaf pile has no collision - you wade
 * through it rather than stand on it - so an entity landing in one actually rests on the block
 * underneath, and {@code Entity.getOnPosLegacy()} probes {@code floor(y - 0.2)}, which resolves past
 * the pile to that block. {@code fallOn} is therefore dispatched to the ground, never to the pile.
 * Buying the dispatch with a collision floor (the trick {@code BelltowerTrellisBlock} uses) is not
 * available either: the floor would have to be at least 0.2 high to clear the probe and at most 0.2
 * high to keep a 1.8-tall player under a two-high ceiling.</p>
 *
 * <p>{@code @ModifyVariable} on the argument rather than a cancellable {@code @Inject} on purpose:
 * only the distance is reduced, and the whole vanilla calculation downstream - Feather Falling,
 * Jump Boost, entity fall-damage immunity, the damage source - still runs untouched.</p>
 *
 * <p>Cost: nothing at all below {@link #NO_DAMAGE_BELOW}, which covers the overwhelming majority
 * of calls, and one block lookup above it. Note that {@code causeFallDamage} is <em>not</em> rare -
 * {@code Entity.checkFallDamage} calls it on every landing where {@code fallDistance > 0}, which
 * includes stepping down half a block - hence the guard.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMapleLeafPileMixin {

    /**
     * Below this, vanilla deals no fall damage at all: {@code calculateFallDamage} is
     * {@code ceil((distance - 3 - jumpBoost) * multiplier)}. Softening a distance that already
     * yields nothing cannot change any outcome - including for passengers, which
     * {@code Entity.causeFallDamage} forwards the same distance to - so the block lookup is skipped
     * outright. This is what keeps the mixin off the common path: {@code causeFallDamage} fires on
     * every landing with a non-zero fall distance, which includes stepping down half a block.
     */
    private static final float NO_DAMAGE_BELOW = 3.0F;

    @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float cobblesafari$softenOnMapleLeafPile(float fallDistance) {
        if (fallDistance <= NO_DAMAGE_BELOW) {
            return fallDistance;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        BlockState atFeet = self.level().getBlockState(self.blockPosition());
        if (!(atFeet.getBlock() instanceof MapleLeafPileBlock)) {
            return fallDistance;
        }
        return fallDistance * MapleLeafPileBlock.fallSofteningFactor(
                atFeet.getValue(BlockStateProperties.LAYERS));
    }
}
