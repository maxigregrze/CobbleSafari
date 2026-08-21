package maxigregrze.cobblesafari.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Scaffolding-style placement for the suspension bridge: clicking any face of an existing bridge
 * relocates the placement to the first free cell past the end of its chain, so a whole span can be
 * extended by repeatedly clicking the piece next to you.
 *
 * <p>Going through {@link BlockItem#updatePlacementContext} rather than the block's
 * {@code useItemOn} is deliberate - it is the hook Mojang provides for exactly this, and it leaves
 * the rest of {@link BlockItem#place} intact: area protection, {@code mayUseItemAt}, the
 * item-placed statistic and each loader's place event all still run.</p>
 *
 * <p>Because the relocation always advances along the clicked bridge's own direction, a placement
 * that would turn the chain cannot be expressed at all.</p>
 */
public class BelltowerBridgeBlockItem extends BlockItem {

    public BelltowerBridgeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        if (!state.is(this.getBlock())) {
            return context;                      // ordinary placement against an anchor
        }
        Direction facing = state.getValue(BelltowerBridgeBlock.FACING);
        BlockPos.MutableBlockPos cursor = clicked.mutable();
        for (int i = 0; i < BelltowerBridgeBlock.MAX_LENGTH; i++) {
            cursor.move(facing);
            BlockState next = level.getBlockState(cursor);
            if (next.is(this.getBlock())) {
                continue;                        // walk along the existing run
            }
            return next.canBeReplaced(context)
                    ? BlockPlaceContext.at(context, cursor.immutable(), facing)
                    : null;
        }
        return null;                             // chain already at the cap
    }
}
