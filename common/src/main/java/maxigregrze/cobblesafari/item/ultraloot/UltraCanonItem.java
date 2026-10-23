package maxigregrze.cobblesafari.item.ultraloot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Crossbow-like launcher shared by the Stellar Canon and the Origami Carrier (action plan 192,
 * section 7.1): hold use to charge (one ammo is taken when the charge completes and is released),
 * then use again to fire. Not a {@code CrossbowItem}: no arrows, fireworks, Multishot or Quick
 * Charge. The enchantment value stays 0, so only books (Unbreaking, Mending) can enchant it.
 */
public class UltraCanonItem extends Item {

    public static final int CHARGE_TICKS = 25;
    private static final int START_SOUND_TICK = 5;
    private static final int MID_SOUND_TICK = 13;

    private final Item ammo;
    private final BiFunction<Level, LivingEntity, Projectile> projectileFactory;
    private final float velocity;
    private final String tooltipKey;

    public UltraCanonItem(Properties properties, Item ammo, BiFunction<Level, LivingEntity, Projectile> projectileFactory,
                          float velocity, String tooltipKey) {
        super(properties);
        this.ammo = ammo;
        this.projectileFactory = projectileFactory;
        this.velocity = velocity;
        this.tooltipKey = tooltipKey;
    }

    public static boolean isCharged(ItemStack stack) {
        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
        return charged != null && !charged.isEmpty();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isCharged(stack)) {
            shoot(level, player, hand, stack);
            return InteractionResultHolder.consume(stack);
        }
        if (player.getAbilities().instabuild || !findAmmo(player).isEmpty()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide()) {
            return;
        }
        int elapsed = getUseDuration(stack, user) - remainingUseDuration;
        if (elapsed == START_SOUND_TICK) {
            playSound(level, user, SoundEvents.CROSSBOW_LOADING_START.value(), 0.5f);
        } else if (elapsed == MID_SOUND_TICK) {
            playSound(level, user, SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), 0.5f);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        int elapsed = getUseDuration(stack, user) - timeLeft;
        if (elapsed < CHARGE_TICKS || isCharged(stack)) {
            return;
        }
        ItemStack loaded = takeAmmo(user);
        if (loaded.isEmpty()) {
            return;
        }
        stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(loaded));
        playSound(level, user, SoundEvents.CROSSBOW_LOADING_END.value(), 1.0f);
    }

    private void shoot(Level level, Player player, InteractionHand hand, ItemStack stack) {
        stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        if (!level.isClientSide()) {
            Projectile projectile = projectileFactory.apply(level, player);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, 1.0f);
            level.addFreshEntity(projectile);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        playSound(level, player, SoundEvents.CROSSBOW_SHOOT, 1.0f);
    }

    private ItemStack findAmmo(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(ammo)) {
                return held;
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.is(ammo)) {
                return slot;
            }
        }
        return ItemStack.EMPTY;
    }

    /** One ammo, removed from the inventory unless in creative (which loads a free one). */
    private ItemStack takeAmmo(LivingEntity user) {
        if (!(user instanceof Player player)) {
            return ItemStack.EMPTY;
        }
        if (player.getAbilities().instabuild) {
            return new ItemStack(ammo);
        }
        ItemStack source = findAmmo(player);
        return source.isEmpty() ? ItemStack.EMPTY : source.split(1);
    }

    private static void playSound(Level level, LivingEntity user, SoundEvent sound, float volume) {
        level.playSound(null, user.getX(), user.getY(), user.getZ(), sound, SoundSource.PLAYERS, volume,
                1.0f / (level.getRandom().nextFloat() * 0.5f + 1.0f) + 0.2f);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CHARGE_TICKS + 3;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CROSSBOW;
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UltraLootTooltip.append(tooltip, tooltipKey);
        if (isCharged(stack)) {
            tooltip.add(Component.translatable("tooltip.cobblesafari.canon.charged", ammo.getDescription())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
