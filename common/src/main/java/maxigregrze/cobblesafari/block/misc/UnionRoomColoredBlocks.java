package maxigregrze.cobblesafari.block.misc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class UnionRoomColoredBlocks {

    private UnionRoomColoredBlocks() {}

    public static ItemStack stackWithColor(Block block, UnionRoomColor color) {
        ItemStack stack = new ItemStack(block);
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("color", color.getSerializedName())));
        return stack;
    }

    public static UnionRoomColor colorFromStack(ItemStack stack) {
        BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
        if (props != null) {
            String value = props.properties().get("color");
            if (value != null) {
                for (UnionRoomColor color : UnionRoomColor.VALUES) {
                    if (color.getSerializedName().equals(value)) {
                        return color;
                    }
                }
            }
        }
        return UnionRoomColor.GREEN;
    }

    public static BlockState applyColorFromStack(BlockState state, ItemStack stack) {
        BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
        if (props != null) {
            return props.apply(state);
        }
        return state;
    }
}
