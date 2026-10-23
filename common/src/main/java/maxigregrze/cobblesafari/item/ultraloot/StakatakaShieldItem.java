package maxigregrze.cobblesafari.item.ultraloot;

import maxigregrze.cobblesafari.init.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Stacked Shield (action plan 192, section 8). A raised, active shield cancels any vanilla-blockable
 * hit whatever its amount, for one durability point, then grants Absorption IV and Regeneration II
 * and rests for {@code 4 s × damage} - the more worn, the longer the rest. The rest is a game-time
 * deadline in a data component: one write per block, nothing to sync when it ends.
 * <p>The blocking itself is detected by {@code UltraLootDamageHandler}, ahead of vanilla's shield
 * logic. Enchantment value 0: only books (Unbreaking, Mending) apply.</p>
 */
public class StakatakaShieldItem extends Item {

    public static final int MAX_DURABILITY = 32;
    private static final int REST_TICKS_PER_DAMAGE = 80;
    private static final int BOOST_TICKS = 200;
    private static final int ABSORPTION_AMPLIFIER = 3;
    private static final int REGENERATION_AMPLIFIER = 1;

    public StakatakaShieldItem(Properties properties) {
        super(properties);
    }

    public static boolean isResting(ItemStack stack, Level level) {
        Long until = stack.get(ModComponents.SHIELD_RECHARGE_UNTIL);
        return until != null && level.getGameTime() < until;
    }

    /** Server side, once a hit has been cancelled: wear, boost, rest. */
    public static void onBlocked(Player player, ItemStack stack) {
        Level level = player.level();
        InteractionHand hand = player.getUsedItemHand();
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, BOOST_TICKS, ABSORPTION_AMPLIFIER));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, BOOST_TICKS, REGENERATION_AMPLIFIER));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS, 1.0f, 0.8f + level.getRandom().nextFloat() * 0.4f);
        if (!stack.isEmpty()) {
            int restTicks = REST_TICKS_PER_DAMAGE * stack.getDamageValue();
            stack.set(ModComponents.SHIELD_RECHARGE_UNTIL, level.getGameTime() + restTicks);
            // The cooldown is only the client's cue (overlay, no immediate re-raise while use is
            // held); the component stays the authority, as cooldowns do not survive a relog.
            player.getCooldowns().addCooldown(stack.getItem(), restTicks);
        }
        player.stopUsingItem();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isResting(stack, level)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UltraLootTooltip.append(tooltip, "stakataka_shield");
    }
}
