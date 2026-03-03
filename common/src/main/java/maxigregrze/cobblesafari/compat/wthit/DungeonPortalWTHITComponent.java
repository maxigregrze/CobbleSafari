package maxigregrze.cobblesafari.compat.wthit;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class DungeonPortalWTHITComponent implements IBlockComponentProvider {
    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getData().raw();
        if (serverData == null) return;

        String portalType = serverData.getString("portal_type");

        if ("entrance".equals(portalType)) {
            if (serverData.contains("destination_display")) {
                tooltip.addLine(Component.translatable("tooltip.cobblesafari.portal.dimension",
                        Component.literal(serverData.getString("destination_display"))));
            } else if (serverData.contains("dungeon_id")) {
                String dungeonId = serverData.getString("dungeon_id");
                String translationKey = "tooltip.cobblesafari.portal." + dungeonId;
                tooltip.addLine(Component.translatable("tooltip.cobblesafari.portal.dimension",
                        Component.translatable(translationKey)));
            }
            if (serverData.contains("remaining_time")) {
                tooltip.addLine(Component.translatable("tooltip.cobblesafari.portal.remaining",
                        serverData.getString("remaining_time")));
            }
        } else if ("exit".equals(portalType)) {
            tooltip.addLine(Component.translatable("tooltip.cobblesafari.portal.exit"));
        }
    }
}
