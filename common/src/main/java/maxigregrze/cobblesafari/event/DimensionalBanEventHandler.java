package maxigregrze.cobblesafari.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import kotlin.Unit;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.manager.BannedItemsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

public class DimensionalBanEventHandler {

    private DimensionalBanEventHandler() {}

    public static InteractionResultHolder<ItemStack> onUseItem(Player player, Level world, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        if (BannedItemsManager.isItemBanned(world.dimension(), stack.getItem())) {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("cobblesafari.ban.item_banned")
                );
            }
            return InteractionResultHolder.fail(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    public static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }

        if (player.isCreative()) {
            return InteractionResult.PASS;
        }

        if (!BannedItemsManager.isBlockPlacingAllowed(world.dimension())) {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("cobblesafari.ban.block_placing_banned")
                );
                resyncBlock(serverPlayer, hitResult);
            }
            return InteractionResult.FAIL;
        }

        Block block = blockItem.getBlock();
        if (BannedItemsManager.isBlockBanned(world.dimension(), block)) {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("cobblesafari.ban.block_banned")
                );
                resyncBlock(serverPlayer, hitResult);
            }
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    private static void resyncBlock(ServerPlayer player, BlockHitResult hitResult) {
        BlockPos target = hitResult.getBlockPos();
        BlockPos placed = target.relative(hitResult.getDirection());
        player.connection.send(new ClientboundBlockUpdatePacket(player.level(), target));
        player.connection.send(new ClientboundBlockUpdatePacket(player.level(), placed));
    }

    public static InteractionResult onAttackBlock(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        if (player.isCreative()) {
            return InteractionResult.PASS;
        }

        if (!BannedItemsManager.isBlockBreakingAllowed(world.dimension())) {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("cobblesafari.ban.block_breaking_banned")
                );
                serverPlayer.connection.send(
                        new ClientboundBlockUpdatePacket(serverPlayer.level(), pos)
                );
            }
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    public static boolean onBlockBreakTry(ResourceKey<Level> dimension, Player player, BlockPos pos) {
        if (player.isCreative()) {
            return true;
        }
        if (!BannedItemsManager.isBlockBreakingAllowed(dimension)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(
                        new ClientboundBlockUpdatePacket(serverPlayer.level(), pos)
                );
            }
            CobbleSafari.LOGGER.warn(
                    "Block break blocked in dimension {} at {} for player {} (item: {})",
                    dimension.location(), pos, player.getName().getString(),
                    player.getMainHandItem().getItem().getDescriptionId()
            );
            return false;
        }
        return true;
    }

    public static void registerCobblemonEvents() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGHEST, event -> {
            for (var actor : event.getBattle().getActors()) {
                for (var pokemon : actor.getPokemonList()) {
                    var entity = pokemon.getEntity();
                    if (entity != null) {
                        Level level = entity.level();
                        if (!BannedItemsManager.isBattleAllowed(level.dimension())) {
                            event.cancel();
                            return Unit.INSTANCE;
                        }
                    }
                }
            }
            return Unit.INSTANCE;
        });

        CobblemonEvents.POKEMON_SENT_PRE.subscribe(Priority.HIGHEST, event -> {
            var level = event.getLevel();
            if (level != null) {
                if (!BannedItemsManager.isBattleAllowed(level.dimension())) {
                    event.cancel();
                    return Unit.INSTANCE;
                }
            }
            return Unit.INSTANCE;
        });

        CobblemonEvents.POKEROD_CAST_PRE.subscribe(Priority.HIGHEST, event -> {
            var bobber = event.getBobber();
            if (bobber != null) {
                var owner = bobber.getPlayerOwner();
                if (owner instanceof ServerPlayer serverPlayer) {
                    if (!BannedItemsManager.isBattleAllowed(serverPlayer.level().dimension())) {
                        serverPlayer.sendSystemMessage(
                                Component.translatable("cobblesafari.ban.fishing_banned")
                        );
                        event.cancel();
                        return Unit.INSTANCE;
                    }
                }
            }
            return Unit.INSTANCE;
        });

        CobbleSafari.LOGGER.info("CobbleSafari >> Dimensional ban event handlers registered!");
    }
}
