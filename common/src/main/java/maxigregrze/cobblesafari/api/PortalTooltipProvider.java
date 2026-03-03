package maxigregrze.cobblesafari.api;

import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface PortalTooltipProvider {
    String getDestinationDisplay(ServerLevel level, DungeonPortalBlockEntity portalEntity);
}
