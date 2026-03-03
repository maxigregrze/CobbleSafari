package maxigregrze.cobblesafari.safari;

import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class MudBallCauldronHandler {

    private MudBallCauldronHandler() {}

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (!heldStack.is(Items.DIRT)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) > 0) {
            if (!level.isClientSide()) {
                if (!player.getAbilities().instabuild) {
                    heldStack.shrink(1);
                }

                int waterLevel = state.getValue(LayeredCauldronBlock.LEVEL);
                if (waterLevel == 1) {
                    level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                } else {
                    level.setBlockAndUpdate(pos, state.setValue(LayeredCauldronBlock.LEVEL, waterLevel - 1));
                }

                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);

                ItemStack mudBalls = new ItemStack(ModItems.MUD_BALL, 16);
                if (!player.getInventory().add(mudBalls)) {
                    player.drop(mudBalls, false);
                }

                player.awardStat(Stats.ITEM_USED.get(Items.DIRT));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
    }
}
