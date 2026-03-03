package maxigregrze.cobblesafari.block.basepc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class BasePCBlockItem extends BlockItem {

    public BasePCBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (stack.get(DataComponents.CUSTOM_MODEL_DATA) != null) return;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        int rank = customData.copyTag().getInt("Rank");
        rank = Math.clamp(rank, 0, 5);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(rank));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        int rank = 0;
        int battery = 0;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            rank = tag.getInt("Rank");
            battery = tag.getInt("Battery");
        }

        int maxBattery = BasePCBlockEntity.getMaxBattery(rank);
        int percentage = maxBattery > 0 ? (battery * 100 / maxBattery) : 0;

        tooltip.add(Component.translatable("tooltip.cobblesafari.basepc.rank_battery",
                Component.translatable("tooltip.cobblesafari.basepc.rank." + getRankKey(rank)),
                percentage));
    }

    private static String getRankKey(int rank) {
        return switch (rank) {
            case 1 -> "bronze";
            case 2 -> "silver";
            case 3 -> "gold";
            case 4 -> "platinum";
            case 5 -> "creative";
            default -> "regular";
        };
    }
}
