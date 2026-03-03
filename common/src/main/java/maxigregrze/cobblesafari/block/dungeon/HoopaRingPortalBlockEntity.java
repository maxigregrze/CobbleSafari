package maxigregrze.cobblesafari.block.dungeon;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HoopaRingPortalBlockEntity extends BlockEntity {

    public HoopaRingPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOOPA_RING_PORTAL, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HoopaRingPortalBlockEntity blockEntity) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.getRandom().nextInt(40) == 0) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            serverLevel.playSound(null, x, y, z, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5f, serverLevel.getRandom().nextFloat() * 0.4f + 0.8f);
        }
    }
}
