package maxigregrze.cobblesafari.item;

import maxigregrze.cobblesafari.block.misc.LostItemBlock;
import maxigregrze.cobblesafari.block.misc.LostItemSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block item for the Lost Item block, one registered instance per {@link LostItemSkin}.
 *
 * <p>Two behaviours set it apart from a plain {@code BlockItem}:
 * <ul>
 *   <li>it stamps its skin onto the placed block state, so the creative item you pick is the look you
 *       get;</li>
 *   <li>it names itself per skin ({@code Lost Item - Classic}) instead of falling back to the block's
 *       own name. The block keeps its plain {@code block.cobblesafari.lost_item} name, which is what
 *       F3 and third-party tooltips show.</li>
 * </ul>
 */
public class LostItemBlockItem extends BlockItem {

    private final LostItemSkin skin;

    public LostItemBlockItem(Block block, Properties properties, LostItemSkin skin) {
        super(block, properties);
        this.skin = skin;
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(LostItemBlock.SKIN, this.skin);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.cobblesafari.lost_item." + this.skin.getSerializedName());
    }
}
