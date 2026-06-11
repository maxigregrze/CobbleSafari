package maxigregrze.cobblesafari.effect;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import kotlin.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RedShackledPlayerGuard {

    private static final int MESSAGE_COOLDOWN_TICKS = 40;
    private static final Map<UUID, Long> LAST_MESSAGE_TICK = new HashMap<>();

    private RedShackledPlayerGuard() {}

    public static InteractionResultHolder<ItemStack> onUseItem(Player player, Level world, InteractionHand hand) {
        if (!RedShackledEffects.isShackled(player)) {
            return null;
        }
        notifyBlocked(player);
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    public static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand) {
        if (!RedShackledEffects.isShackled(player)) {
            return InteractionResult.PASS;
        }
        notifyBlocked(player);
        return InteractionResult.FAIL;
    }

    public static InteractionResult onAttackBlock(Player player, Level world, InteractionHand hand,
                                                  BlockPos pos, Direction direction) {
        if (!RedShackledEffects.isShackled(player)) {
            return InteractionResult.PASS;
        }
        notifyBlocked(player);
        return InteractionResult.FAIL;
    }

    public static boolean onBlockBreakTry(ResourceKey<Level> dimension, Player player, BlockPos pos) {
        if (!RedShackledEffects.isShackled(player)) {
            return true;
        }
        notifyBlocked(player);
        return false;
    }

    public static void registerCobblemonEvents() {
        CobblemonEvents.POKEMON_SENT_PRE.subscribe(Priority.HIGHEST, event -> {
            UUID ownerId = event.getPokemon().getOwnerUUID();
            if (ownerId != null) {
                Player owner = event.getLevel().getPlayerByUUID(ownerId);
                if (owner != null && RedShackledEffects.isShackled(owner)) {
                    notifyBlocked(owner);
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
                if (owner != null && RedShackledEffects.isShackled(owner)) {
                    notifyBlocked(owner);
                    event.cancel();
                    return Unit.INSTANCE;
                }
            }
            return Unit.INSTANCE;
        });

        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGHEST, event -> {
            for (var actor : event.getBattle().getActors()) {
                if (actor instanceof PlayerBattleActor playerActor) {
                    ServerPlayer player = playerActor.getEntity();
                    if (player != null && RedShackledEffects.isShackled(player)) {
                        notifyBlocked(player);
                        event.cancel();
                        return Unit.INSTANCE;
                    }
                }
            }
            return Unit.INSTANCE;
        });
    }

    private static void notifyBlocked(Player player) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        long tick = player.level().getGameTime();
        Long last = LAST_MESSAGE_TICK.get(player.getUUID());
        if (last != null && tick - last < MESSAGE_COOLDOWN_TICKS) {
            return;
        }
        LAST_MESSAGE_TICK.put(player.getUUID(), tick);
        serverPlayer.sendSystemMessage(Component.translatable("cobblesafari.red_shackled.action_blocked"));
    }
}
