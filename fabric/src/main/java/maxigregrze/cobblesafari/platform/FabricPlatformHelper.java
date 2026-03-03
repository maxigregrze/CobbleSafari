package maxigregrze.cobblesafari.platform;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.services.IPlatformHelper;
import maxigregrze.cobblesafari.underground.MiningSession;
import maxigregrze.cobblesafari.underground.screen.UndergroundOpenData;
import maxigregrze.cobblesafari.underground.screen.UndergroundScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void sendPayloadToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendPayloadToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    @Override
    public void openUndergroundMenu(ServerPlayer player, MiningSession session) {
        player.openMenu(new ExtendedScreenHandlerFactory<UndergroundOpenData>() {
            @Override
            public UndergroundOpenData getScreenOpeningData(ServerPlayer p) {
                return session.createOpenData();
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.cobblesafari.underground.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                UndergroundOpenData data = session.createOpenData();
                return new UndergroundScreenHandler(syncId, inv,
                        data.sessionId(), data.treasureCount(), data.gridData(),
                        data.currentStability(), data.maxStability());
            }
        });
    }
}
