package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class WhirlwindBlockEntity extends BlockEntity {

    public WhirlwindBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WHIRLWIND, pos, state);
    }

    protected WhirlwindBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean shouldHideShapeForSurvivalPlayer(Player player) {
        return !player.isCreative();
    }

    public boolean shouldHideWorldModelForLocalPlayer(Player player) {
        return false;
    }
}
