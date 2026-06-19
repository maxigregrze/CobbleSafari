package maxigregrze.cobblesafari.item;

import maxigregrze.cobblesafari.block.teleporter.SurvivalTeleportPadBlock;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TinkhammerItem extends AxeItem {

    public TinkhammerItem(Item.Properties properties) {
        super(Tiers.DIAMOND, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SurvivalTeleportPadBlock) {
            BlockHitResult hit = new BlockHitResult(
                    context.getClickLocation(), context.getClickedFace(), pos, context.isInside());
            ItemInteractionResult result = SurvivalTeleportPadBlock.tinkhammerInteract(
                    context.getItemInHand(), state, level, pos, context.getPlayer(), hit);
            if (result != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return super.useOn(context);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModItems.TINKAGEAR);
    }
}
