package maxigregrze.cobblesafari.block.incubator;

import maxigregrze.cobblesafari.incubator.CobbreedingCompat;
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

public class IncubatorBlockItem extends BlockItem {

    private static final String KEY_INPUT_ITEM = "InputItem";
    private static final String KEY_STORED_EGG_SPECIES_NAME = "StoredEggSpeciesName";
    private static final String KEY_TICKS_REMAINING = "TicksRemaining";

    public IncubatorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            if (stack.get(DataComponents.CUSTOM_MODEL_DATA) != null) {
                stack.remove(DataComponents.CUSTOM_MODEL_DATA);
            }
            return;
        }

        CompoundTag tag = customData.copyTag();
        boolean hasEgg = tag.contains(KEY_INPUT_ITEM) || (tag.getBoolean("IsCobbreedingEgg") && (tag.contains(KEY_STORED_EGG_SPECIES_NAME) || tag.contains(KEY_TICKS_REMAINING)));
        if (!hasEgg) {
            return;
        }

        int ticksRemaining = tag.getInt(KEY_TICKS_REMAINING);
        int modelData;
        if (ticksRemaining <= 0) {
            modelData = 2;
        } else {
            modelData = 1;
        }
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelData));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;

        CompoundTag tag = customData.copyTag();
        boolean hasInput = tag.contains(KEY_INPUT_ITEM);
        boolean isCobbreedingEgg = tag.getBoolean("IsCobbreedingEgg");
        int ticksRemaining = tag.getInt(KEY_TICKS_REMAINING);

        if (!hasInput && !(isCobbreedingEgg && tag.contains(KEY_STORED_EGG_SPECIES_NAME))) {
            return;
        }

        String itemName;
        if (isCobbreedingEgg && !CobbreedingCompat.isCobbreedingLoaded()) {
            String speciesName = tag.getString(KEY_STORED_EGG_SPECIES_NAME);
            if (speciesName != null && !speciesName.isEmpty()) {
                String formatted = speciesName.substring(0, 1).toUpperCase() + speciesName.substring(1);
                itemName = Component.translatable("cobblesafari.incubator.strange_egg", formatted).getString();
            } else {
                itemName = Component.translatable("cobblesafari.incubator.strange_egg", "Unknown").getString();
            }
        } else if (isCobbreedingEgg && CobbreedingCompat.isCobbreedingLoaded() && hasInput) {
            ItemStack storedEgg = ItemStack.parse(context.registries(), tag.getCompound(KEY_INPUT_ITEM))
                    .orElse(ItemStack.EMPTY);
            String speciesName = CobbreedingCompat.getEggName(storedEgg);
            if (speciesName != null && !speciesName.isEmpty()) {
                itemName = speciesName.substring(0, 1).toUpperCase() + speciesName.substring(1) + " Egg";
            } else {
                itemName = "Unknown Egg";
            }
        } else if (hasInput) {
            ItemStack storedEgg = ItemStack.parse(context.registries(), tag.getCompound(KEY_INPUT_ITEM))
                    .orElse(ItemStack.EMPTY);
            if (storedEgg.isEmpty()) return;
            itemName = storedEgg.getHoverName().getString();
        } else {
            return;
        }

        if (ticksRemaining <= 0) {
            tooltip.add(Component.translatable("tooltip.cobblesafari.incubator.ready", itemName));
        } else {
            int totalSeconds = ticksRemaining / 20;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            String timeStr = String.format("%02d:%02d", minutes, seconds);
            tooltip.add(Component.translatable("tooltip.cobblesafari.incubator.progress", timeStr, itemName));
        }
    }
}
