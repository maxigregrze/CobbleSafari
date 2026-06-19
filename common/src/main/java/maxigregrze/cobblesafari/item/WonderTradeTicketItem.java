package maxigregrze.cobblesafari.item;

import maxigregrze.cobblesafari.config.WonderTradeSettings;
import maxigregrze.cobblesafari.wondertrade.WonderTradeService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WonderTradeTicketItem extends Item {

    public WonderTradeTicketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        if (WonderTradeSettings.get().isUnlimitedDailyTrades()) {
            player.sendSystemMessage(Component.translatable("cobblesafari.command.wondertrade.ticket_unlimited_mode"));
            return InteractionResultHolder.fail(stack);
        }

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return InteractionResultHolder.fail(stack);
        }

        WonderTradeService.addPlayerTickets(server, player.getUUID(), 1);
        int after = WonderTradeService.getEffectiveTicketCount(server, player.getUUID());
        player.sendSystemMessage(Component.translatable("cobblesafari.item.ticket_wondertrade.used", after));
        stack.consume(1, player);

        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
