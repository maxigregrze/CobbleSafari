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
        ClaimSounds.playPickup(serverPlayer);
        maxigregrze.cobblesafari.objectives.ObjectivesManager.onAuspiciousPokeballOpened(serverPlayer);
        // The Hyperspace ribbon only counts for the ball that appears in Hyperspace Lumiose (plan 201 § 8.3).
        if ("cobblesafari:dungeon_paris".equals(serverLevel.dimension().location().toString())) {
            maxigregrze.cobblesafari.mark.RibbonAwards.rollRibbon(
                    serverPlayer, maxigregrze.cobblesafari.mark.RibbonAwards.RIBBON_PARIS, 0.10);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AuspiciousPokeballGoldBlockEntity(pos, state);
    }
}
