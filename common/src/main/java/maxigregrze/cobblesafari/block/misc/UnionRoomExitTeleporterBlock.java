package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.unionroom.UnionRoomManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

public class UnionRoomExitTeleporterBlock extends UnionRoomGlobeBlock {

    public static final MapCodec<UnionRoomExitTeleporterBlock> CODEC = simpleCodec(UnionRoomExitTeleporterBlock::new);

    public UnionRoomExitTeleporterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends UnionRoomGlobeBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos lowerPos = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                ? pos.below()
                : pos;

        BlockEntity be = level.getBlockEntity(lowerPos);
        if (!(be instanceof UnionRoomExitTeleporterBlockEntity teleporterBe)) {
            return InteractionResult.PASS;
        }

        int instanceId = teleporterBe.getInstanceId();

        if (serverPlayer.isShiftKeyDown()) {
            UnionRoomManager.handlePlayerExit(serverPlayer, instanceId);
        } else {
            UnionRoomManager.showRoomCode(serverPlayer, instanceId);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            return new UnionRoomExitTeleporterBlockEntity(pos, state);
        }
        return new UnionRoomGlobeUpperBlockEntity(pos, state);
    }
}
