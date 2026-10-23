package maxigregrze.cobblesafari.item.ultraloot;

import maxigregrze.cobblesafari.entity.projectile.NeedleDartEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Needle Dart (action plan 192, section 4): an arrow in {@code #minecraft:arrows}, so every bow,
 * crossbow or modded weapon reading that tag fires it; dispensers go through {@link #asProjectile}.
 */
public class NeedleDartItem extends ArrowItem {

    public NeedleDartItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new NeedleDartEntity(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        NeedleDartEntity dart = new NeedleDartEntity(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
        dart.pickup = AbstractArrow.Pickup.ALLOWED;
        return dart;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UltraLootTooltip.append(tooltip, "poipole_arrow");
    }
}
