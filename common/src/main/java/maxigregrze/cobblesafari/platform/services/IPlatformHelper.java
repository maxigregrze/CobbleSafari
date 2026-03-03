package maxigregrze.cobblesafari.platform.services;

import maxigregrze.cobblesafari.underground.MiningSession;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    Path getConfigDir();

    void sendPayloadToPlayer(ServerPlayer player, CustomPacketPayload payload);

    void sendPayloadToServer(CustomPacketPayload payload);

    void openUndergroundMenu(ServerPlayer player, MiningSession session);

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
