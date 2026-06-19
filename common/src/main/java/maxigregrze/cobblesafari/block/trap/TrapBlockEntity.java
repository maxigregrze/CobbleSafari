package maxigregrze.cobblesafari.block.trap;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TrapBlockEntity extends BlockEntity {

    public TrapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAP, pos, state);
    }

    public boolean shouldHideWorldModelForLocalPlayer(Player player) {
        return !shouldRevealTo(player);
    }

    private static boolean shouldRevealTo(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        if (player.isCrouching()) {
            return true;
        }
        return isHoldingTrap(player);
    }

    private static boolean isHoldingTrap(Player player) {
        return player.getMainHandItem().is(ModItemTags.TRAPS)
                || player.getOffhandItem().is(ModItemTags.TRAPS);
    }
}
