package maxigregrze.cobblesafari.api;

import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import net.minecraft.server.level.ServerPlayer;

/**
 * Functional interface for addon mods to fully customize portal interactions.
 * <p>
 * Registered via {@link DungeonRegistrationAPI#registerPortalInterceptor(String, PortalInterceptor)}.
 * Called by CobbleSafari after basic validation when a player right-clicks a dungeon portal
 * whose dimension has a registered interceptor.
 */
@FunctionalInterface
public interface PortalInterceptor {

    /**
     * @param player       the server player interacting with the portal
     * @param portalEntity the portal block entity
     * @param config       the dungeon configuration for this portal's dimension
     * @return {@code true} if the interaction was fully handled (CobbleSafari skips its
     *         entire default flow), {@code false} to let CobbleSafari proceed normally
     */
    boolean handlePortalInteraction(ServerPlayer player,
                                    DungeonPortalBlockEntity portalEntity,
                                    DungeonConfig config);
}
