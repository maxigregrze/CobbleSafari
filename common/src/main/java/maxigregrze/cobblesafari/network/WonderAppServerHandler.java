package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.config.WonderTradeSettings;
import maxigregrze.cobblesafari.data.WonderTradeSavedData;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.wondertrade.WonderTradeEventDefinition;
import maxigregrze.cobblesafari.wondertrade.WonderTradeEventRegistry;
import maxigregrze.cobblesafari.wondertrade.WonderTradeService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WonderAppServerHandler {

    private WonderAppServerHandler() {}

    public static void handle(ServerPlayer player, WonderAppPayload payload) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        switch (payload.actionType()) {
            case WonderAppPayload.ACTION_REQUEST_STATE -> sendSnapshot(
                    player, WonderAppResultPayload.SUB_BEGIN, new CompoundTag(), new CompoundTag(), "");
            case WonderAppPayload.ACTION_TRADE -> {
                int slot = payload.slot();
                if (slot < 0 || slot > 5) {
                    sendSnapshot(player, WonderAppResultPayload.SUB_ERROR, new CompoundTag(), new CompoundTag(),
                            "gui.cobblesafari.rotomphone.wonder.error.slot");
                    return;
                }
                doTradeAndRespond(player, slot);
            }
            default -> {
                // Unknown action type; ignore
            }
        }
    }

    private static void doTradeAndRespond(ServerPlayer player, int slot) {
        WonderTradeService.TradeResultDetailed r = WonderTradeService.tryTrade(player, slot);
        if (r.result() != WonderTradeService.TradeResult.SUCCESS) {
            String key = switch (r.result()) {
                case EMPTY_SLOT -> "gui.cobblesafari.rotomphone.wonder.error.empty";
                case POOL_EMPTY -> "gui.cobblesafari.rotomphone.wonder.error.pool";
                case NO_CREDITS -> "gui.cobblesafari.rotomphone.wonder.error.credits";
                default -> "gui.cobblesafari.rotomphone.wonder.error.generic";
            };
            sendSnapshot(player, WonderAppResultPayload.SUB_ERROR, new CompoundTag(), new CompoundTag(), key);
            return;
        }
        sendSnapshot(player, WonderAppResultPayload.SUB_TRADE, r.offeredNbt(), r.receivedNbt(), "");
    }

    private static void sendSnapshot(
            ServerPlayer player,
            int subscreen,
            CompoundTag offeredNbt,
            CompoundTag receivedNbt,
            String errorKey) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        WonderTradeSettings cfg = WonderTradeSettings.get();
        int tickets = cfg.isUnlimitedDailyTrades()
                ? -1
                : WonderTradeService.getRemainingCredits(player);

        WonderTradeSavedData data = WonderTradeSavedData.get(server);
        boolean hasEvent = data.hasActiveEvent();
        String eventName = "";
        String customBannerName = "";
        int eventDaysLeft = 0;
        List<WonderAppResultPayload.EventPoolEntry> poolEntries = List.of();

        if (hasEvent) {
            Optional<WonderTradeEventDefinition> evOpt = WonderTradeEventRegistry.get(data.getActiveEventId());
            if (evOpt.isPresent()) {
                WonderTradeEventDefinition ev = evOpt.get();
                eventName = ev.getEventName();
                if (ev.isHasCustomBanner()) {
                    customBannerName = ev.getCustomBannerName();
                }
                eventDaysLeft = WonderTradeService.getEventDaysLeftForGui(server);
                int weightSum = 0;
                for (WonderTradeSettings.WeightedPoolEntry w : ev.getEventPools()) {
                    if (w.weight > 0) {
                        weightSum += w.weight;
                    }
                }
                List<WonderAppResultPayload.EventPoolEntry> built = new ArrayList<>();
                for (WonderTradeSettings.WeightedPoolEntry w : ev.getEventPools()) {
                    if (w.weight > 0) {
                        built.add(new WonderAppResultPayload.EventPoolEntry(w.groupId, w.weight));
                    }
                }
                poolEntries = built;
            } else {
                hasEvent = false;
            }
        }

        Services.PLATFORM.sendPayloadToPlayer(player, new WonderAppResultPayload(
                subscreen,
                tickets,
                WonderTradeService.getNextResetEpochSeconds(),
                hasEvent,
                eventName,
                customBannerName,
                eventDaysLeft,
                poolEntries,
                offeredNbt,
                receivedNbt,
                errorKey == null ? "" : errorKey));
    }
}
