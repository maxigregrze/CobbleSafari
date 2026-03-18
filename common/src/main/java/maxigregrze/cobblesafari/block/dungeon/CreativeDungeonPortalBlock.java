package maxigregrze.cobblesafari.block.dungeon;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CreativeDungeonPortalBlock extends DungeonPortalBlock {

    public static final MapCodec<CreativeDungeonPortalBlock> CODEC = simpleCodec(CreativeDungeonPortalBlock::new);
    private static final String MODE_RANDOM_KEY = "cobblesafari.portal.mode.random";
    private static final String MODE_FIXED_KEY = "cobblesafari.portal.mode.fixed";

    public CreativeDungeonPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DungeonPortalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player != null && !player.isCreative()) {
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (placer instanceof Player player && !player.isCreative()) {
            level.removeBlock(pos, false);
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DungeonPortalBlockEntity portalEntity) {
            portalEntity.setAutoRenewPortal(true);
            portalEntity.setRandomDestinationMode(true);
            PortalSpawnManager.registerCreativePortal(serverLevel, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide() && player.isShiftKeyDown() && player.isCreative()) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && player.isShiftKeyDown() && player.isCreative()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof DungeonPortalBlockEntity portalEntity
                    && level instanceof ServerLevel
                    && player instanceof ServerPlayer serverPlayer) {
                toggleDestinationMode(portalEntity, serverPlayer);
                return InteractionResult.CONSUME;
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    private void toggleDestinationMode(DungeonPortalBlockEntity portalEntity, ServerPlayer player) {
        List<String> available = PortalSpawnManager.getEnabledDungeonIdsForCycle();
        if (available.isEmpty()) {
            player.sendSystemMessage(Component.translatable(MODE_RANDOM_KEY));
            portalEntity.setRandomDestinationMode(true);
            return;
        }

        if (portalEntity.isRandomDestinationMode()) {
            String nextId = available.get(0);
            portalEntity.setFixedDungeonId(nextId);
            player.sendSystemMessage(Component.translatable(MODE_FIXED_KEY,
                    Component.translatable("dimension." + CobbleSafari.MOD_ID + "." + nextId)));
            return;
        }

        String currentFixed = portalEntity.getFixedDungeonId();
        int index = available.indexOf(currentFixed);
        if (index < 0 || index + 1 >= available.size()) {
            portalEntity.setRandomDestinationMode(true);
            player.sendSystemMessage(Component.translatable(MODE_RANDOM_KEY));
            return;
        }

        String nextId = available.get(index + 1);
        if (DungeonDimensions.getDungeonById(nextId) == null) {
            portalEntity.setRandomDestinationMode(true);
            player.sendSystemMessage(Component.translatable(MODE_RANDOM_KEY));
            return;
        }

        portalEntity.setFixedDungeonId(nextId);
        player.sendSystemMessage(Component.translatable(MODE_FIXED_KEY,
                Component.translatable("dimension." + CobbleSafari.MOD_ID + "." + nextId)));
    }
}
