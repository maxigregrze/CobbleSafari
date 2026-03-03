package maxigregrze.cobblesafari.compat.jade;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.api.DungeonRegistrationAPI;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlock;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.dungeon.PortalSpawnConfig;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DungeonPortalJadeComponent implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (serverData == null) return;

        String portalType = serverData.getString("portal_type");

        if ("entrance".equals(portalType)) {
            if (serverData.contains("destination_display")) {
                tooltip.add(Component.translatable("tooltip.cobblesafari.portal.dimension",
                        Component.literal(serverData.getString("destination_display"))));
            } else if (serverData.contains("dungeon_id")) {
                String dungeonId = serverData.getString("dungeon_id");
                String translationKey = "tooltip.cobblesafari.portal." + dungeonId;
                tooltip.add(Component.translatable("tooltip.cobblesafari.portal.dimension",
                        Component.translatable(translationKey)));
            }
            if (serverData.contains("remaining_time")) {
                tooltip.add(Component.translatable("tooltip.cobblesafari.portal.remaining",
                        serverData.getString("remaining_time")));
            }
        } else if ("exit".equals(portalType)) {
            tooltip.add(Component.translatable("tooltip.cobblesafari.portal.exit"));
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (!(blockEntity instanceof DungeonPortalBlockEntity portalEntity)) return;

        BlockState state = accessor.getBlockState();
        DungeonPortalBlock.PortalType type = state.getValue(DungeonPortalBlock.PORTAL_TYPE);
        compoundTag.putString("portal_type", type.getSerializedName());

        if (type == DungeonPortalBlock.PortalType.ENTRANCE) {
            if (portalEntity.getDungeonDimensionId() != null) {
                String dungeonId = portalEntity.getDungeonDimensionId();
                compoundTag.putString("dungeon_id", dungeonId);
                var provider = DungeonRegistrationAPI.getPortalTooltipProvider(dungeonId);
                if (provider != null && portalEntity.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    String display = provider.getDestinationDisplay(serverLevel, portalEntity);
                    if (display != null && !display.isEmpty()) {
                        compoundTag.putString("destination_display", display);
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
                compoundTag.putString("remaining_time", formatTicks(remaining));
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

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_portal");
    }
}
