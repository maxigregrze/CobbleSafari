package maxigregrze.cobblesafari.platform;

import maxigregrze.cobblesafari.platform.services.IPlatformHelper;
import maxigregrze.cobblesafari.underground.MiningSession;
import maxigregrze.cobblesafari.underground.screen.UndergroundOpenData;
import maxigregrze.cobblesafari.underground.screen.UndergroundScreenHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void sendPayloadToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendPayloadToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public void openUndergroundMenu(ServerPlayer player, MiningSession session) {
        UndergroundOpenData data = session.createOpenData();
        player.openMenu(
                new SimpleMenuProvider(
                        (syncId, inv, p) -> new UndergroundScreenHandler(syncId, inv,
                                data.sessionId(), data.treasureCount(), data.gridData(),
                                data.currentStability(), data.maxStability()),
                        Component.translatable("gui.cobblesafari.underground.title")
                ),
                buf -> UndergroundOpenData.STREAM_CODEC.encode(buf, data)
        );
    }
}
