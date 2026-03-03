package maxigregrze.cobblesafari.api;

import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import net.minecraft.server.MinecraftServer;

/**
 * Functional interface for addon mods to handle portal expiration for externally-managed dungeons.
 * <p>
 * Registered via {@link DungeonRegistrationAPI#registerPortalExpirationHandler(String, PortalExpirationHandler)}.
 * Called by CobbleSafari when a portal whose dungeon is marked as {@code externallyManaged}
 * is removed (expired or destroyed). The addon mod should handle its own cleanup
 * (e.g. close raids, teleport players back, clear regions).
 * <p>
 * CobbleSafari still removes the portal from its tracking maps and saved data.
 */
@FunctionalInterface
public interface PortalExpirationHandler {

    /**
     * @param server       the minecraft server instance
     * @param portalEntity the portal block entity being removed
     * @param config       the dungeon configuration for this portal's dimension
     */
    void onPortalExpired(MinecraftServer server,
                         DungeonPortalBlockEntity portalEntity,
                         DungeonConfig config);
}
