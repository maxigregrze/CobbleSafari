package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.block.misc.UnionRoomColor;
import maxigregrze.cobblesafari.block.misc.UnionRoomColoredBlocks;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class UnionRoomColoredItemClientSetup {

    private static final ResourceLocation COLOR_PROPERTY = ResourceLocation.fromNamespaceAndPath(
            "cobblesafari", "union_room_color");
    private static final float COLOR_SCALE = UnionRoomColor.VALUES.length - 1;

    private UnionRoomColoredItemClientSetup() {}

    public static void registerItemProperties() {
        register(ModBlocks.UNION_ROOM_BRICKS.asItem());
        register(ModBlocks.UNION_ROOM_SPOTLIGHT.asItem());
    }

    private static void register(net.minecraft.world.item.Item item) {
        ItemProperties.register(item, COLOR_PROPERTY, UnionRoomColoredItemClientSetup::colorProperty);
    }

    private static float colorProperty(ItemStack stack, net.minecraft.world.level.Level level,
                                       net.minecraft.world.entity.LivingEntity entity, int seed) {
        return UnionRoomColoredBlocks.colorFromStack(stack).ordinal() / COLOR_SCALE;
    }
}
