package maxigregrze.cobblesafari.compat.wthit;

import maxigregrze.cobblesafari.api.DungeonRegistrationAPI;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlock;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.dungeon.PortalSpawnConfig;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class DungeonPortalWTHITDataProvider implements IDataProvider<DungeonPortalBlockEntity> {
    @Override
    public void appendData(IDataWriter data, IServerAccessor<DungeonPortalBlockEntity> accessor, IPluginConfig config) {
        DungeonPortalBlockEntity portalEntity = accessor.getTarget();
        BlockState blockState = portalEntity.getBlockState();
        DungeonPortalBlock.PortalType type = blockState.getValue(DungeonPortalBlock.PORTAL_TYPE);

        data.raw().putString("portal_type", type.getSerializedName());

        if (type == DungeonPortalBlock.PortalType.ENTRANCE) {
            if (portalEntity.getDungeonDimensionId() != null) {
                String dungeonId = portalEntity.getDungeonDimensionId();
                data.raw().putString("dungeon_id", dungeonId);
                var provider = DungeonRegistrationAPI.getPortalTooltipProvider(dungeonId);
                if (provider != null && portalEntity.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    String display = provider.getDestinationDisplay(serverLevel, portalEntity);
                    if (display != null && !display.isEmpty()) {
                        data.raw().putString("destination_display", display);
                    }
                }
            }

            long remaining = portalEntity.getRemainingLifetimeTicks();
            if (remaining < 0) {
                PortalSpawnManager.ActivePortal activePortal = PortalSpawnManager.getActivePortalById(portalEntity.getPortalId());
                if (activePortal != null && portalEntity.getLevel() != null) {
                    long elapsed = portalEntity.getLevel().getGameTime() - activePortal.spawnTick();
                    remaining = Math.max(0, PortalSpawnConfig.getPortalLifetimeTicks() - elapsed);
                }
            }

            if (remaining >= 0) {
                data.raw().putString("remaining_time", formatTicks(remaining));
            }
        }
    }

    private String formatTicks(long ticks) {
        int totalSeconds = (int) (ticks / 20);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
