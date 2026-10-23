package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.item.ultraloot.GluttonArmorLogic;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gluttonous trigger (action plan 192, section 2.2). Fabric API has no "finished eating" event, so a
 * read-only {@code TAIL} injection on {@code LivingEntity#eat} - which {@code Player#eat} calls - is
 * the single mixin of the Ultra Space loot. It changes nothing and cancels nothing.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGluttonousEatMixin {

    @Inject(
            method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("TAIL")
    )
    private void cobblesafari$gluttonousOnEat(Level level, ItemStack food, FoodProperties foodProperties,
                                              CallbackInfoReturnable<ItemStack> cir) {
        if (!level.isClientSide() && (Object) this instanceof Player player) {
            GluttonArmorLogic.onEat(player);
        }
    }
}
