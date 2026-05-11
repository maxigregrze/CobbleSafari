package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class AuspiciousPokeballGoldBlock extends AuspiciousPokeballBlock {

    public static final MapCodec<AuspiciousPokeballGoldBlock> CODEC = simpleCodec(AuspiciousPokeballGoldBlock::new);

    public AuspiciousPokeballGoldBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof AuspiciousPokeballGoldBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        if (player.isCreative() && player.isShiftKeyDown()) {
            Services.PLATFORM.sendPayloadToPlayer(serverPlayer, blockEntity.createGoldOpenPayload());
            return InteractionResult.CONSUME;
        }

        if (blockEntity.hasClaimed(serverPlayer.getUUID())) {
            return InteractionResult.CONSUME;
        }
        if (!blockEntity.canPlayerAttemptClaim(serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        blockEntity.tryClaim(serverPlayer.getUUID());
        givePoolLoot(serverLevel, pos, serverPlayer, blockEntity);
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AuspiciousPokeballGoldBlockEntity(pos, state);
    }
}
