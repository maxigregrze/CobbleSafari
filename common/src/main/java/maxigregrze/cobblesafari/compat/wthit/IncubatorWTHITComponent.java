package maxigregrze.cobblesafari.compat.wthit;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class IncubatorWTHITComponent implements IBlockComponentProvider {
    private static final String KEY_TIME_READY = "time_ready";
    private static final String KEY_TIME_ARG = "time_arg";
    private static final String KEY_EGG_TYPE = "egg_type";

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getData().raw();
        if (data == null) return;
        if (data.getBoolean(KEY_TIME_READY)) {
            tooltip.addLine(Component.translatable("cobblesafari.waila.incubator.ready"));
        } else if (data.contains(KEY_TIME_ARG)) {
            tooltip.addLine(Component.translatable("cobblesafari.waila.incubator.time_remaining", data.getString(KEY_TIME_ARG)));
        }
        if (data.contains(KEY_EGG_TYPE)) {
            tooltip.addLine(Component.translatable("cobblesafari.waila.incubator.contains", data.getString(KEY_EGG_TYPE)));
        }
    }
}
