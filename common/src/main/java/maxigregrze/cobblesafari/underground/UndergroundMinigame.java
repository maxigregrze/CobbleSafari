package maxigregrze.cobblesafari.underground;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.underground.logic.PlacedTreasure;
import maxigregrze.cobblesafari.underground.logic.ShapeRegistry;
import maxigregrze.cobblesafari.underground.logic.TreasureDataLoader;
import maxigregrze.cobblesafari.underground.logic.TreasureDefinition;
import maxigregrze.cobblesafari.underground.logic.TreasureRegistry;
import maxigregrze.cobblesafari.underground.network.UndergroundPayloads;
import maxigregrze.cobblesafari.underground.screen.UndergroundScreenHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UndergroundMinigame {

    private static final Map<UUID, MiningSession> activeSessions = new HashMap<>();

    public static MenuType<UndergroundScreenHandler> MENU_TYPE;

    public static void registerCommon() {
        CobbleSafari.LOGGER.info("Registering Underground Minigame...");
        TreasureRegistry.init();
        CobbleSafari.LOGGER.info("Underground Minigame registered!");
    }

    public static void loadDatapacks(MinecraftServer server) {
        ShapeRegistry.load(server);
        TreasureDataLoader.load(server);
    }

    public static void startSession(ServerPlayer player) {
        UUID sessionId = UUID.randomUUID();
        long seed = player.level().getRandom().nextLong();

        MiningSession session = new MiningSession(sessionId, seed);
        activeSessions.put(sessionId, session);

        Services.PLATFORM.openUndergroundMenu(player, session);

        player.displayClientMessage(Component.translatable("gui.cobblesafari.underground.treasures_found",
                session.getGrid().getTreasureCount()), true);
    }

    public static MiningSession getSession(UUID sessionId) {
        return activeSessions.get(sessionId);
    }

    public static void onScreenClosed(UUID sessionId, Player player) {
        MiningSession session = activeSessions.remove(sessionId);

        if (session != null && player instanceof ServerPlayer serverPlayer) {
            List<PlacedTreasure> collected = session.getGrid().getRevealedTreasures();
            if (!collected.isEmpty()) {
                giveRewards(serverPlayer, collected);
                if (!session.isComplete()) {
                    player.displayClientMessage(Component.translatable("gui.cobblesafari.underground.partial_clear",
                            collected.size(), session.getGrid().getTreasureCount()), false);
                }
            }
        }
    }

    public static void giveRewards(ServerPlayer player, List<PlacedTreasure> treasures) {
        for (PlacedTreasure treasure : treasures) {
            TreasureDefinition def = treasure.getDefinition();
            if (def == null) {
                CobbleSafari.LOGGER.warn("[Underground] Skipping reward: treasure definition is null for session");
            } else if (def.getRewardItem() == null) {
                CobbleSafari.LOGGER.warn("[Underground] Skipping reward: item is null for treasure id '{}' (sync may have overwritten server registry)", def.getId());
            } else {
                int count = def.getRewardCount();
                ItemStack reward = new ItemStack(def.getRewardItem(), count);
                net.minecraft.resources.ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(def.getRewardItem());
                CobbleSafari.LOGGER.info("[Underground] Gave {} x{} to {}",
                        itemId, count, player.getName().getString());
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
            }
        }
    }

    public static void endSession(UUID sessionId) {
        activeSessions.remove(sessionId);
    }

    public static void syncRegistryToPlayer(ServerPlayer player) {
        List<TreasureDefinition> allTreasures = TreasureRegistry.getAllTreasures();
        List<UndergroundPayloads.TreasureEntryData> entries = new java.util.ArrayList<>();
        for (TreasureDefinition def : allTreasures) {
            entries.add(new UndergroundPayloads.TreasureEntryData(
                    def.getId(), def.getTextureId(), def.getWeight(),
                    def.getMinQty(), def.getMaxQty(), def.getShapeMatrix()
            ));
        }
        Services.PLATFORM.sendPayloadToPlayer(player,
                new UndergroundPayloads.TreasureRegistrySyncPayload(entries));
    }

    public static void syncRegistryToAllPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncRegistryToPlayer(player);
        }
    }
}
