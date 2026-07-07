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

    /**
     * Resolves a directory inside the mod's own jar/resources (e.g. {@code "data/cobblesafari/..."})
     * to a {@link Path} that can be walked at mod-init time, or {@code null} if it does not exist.
     * Used to enumerate bundled data-driven content before the registries freeze (external datapacks
     * load too late for that).
     */
    Path getBundledResourceDir(String path);

    void sendPayloadToPlayer(ServerPlayer player, CustomPacketPayload payload);

    void sendPayloadToServer(CustomPacketPayload payload);

    void openUndergroundMenu(ServerPlayer player, MiningSession session);

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
