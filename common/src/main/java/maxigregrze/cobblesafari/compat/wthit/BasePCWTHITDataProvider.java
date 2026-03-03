package maxigregrze.cobblesafari.compat.wthit;

import maxigregrze.cobblesafari.block.basepc.BasePCBlockEntity;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;

public class BasePCWTHITDataProvider implements IDataProvider<BasePCBlockEntity> {
    @Override
    public void appendData(IDataWriter data, IServerAccessor<BasePCBlockEntity> accessor, IPluginConfig config) {
        BasePCBlockEntity be = accessor.getTarget();
        int rank = be.getRank();
        int r = Math.min(rank, 5);
        data.raw().putInt("rank", r);
        data.raw().putInt("battery", be.getBattery());
        data.raw().putInt("max_battery", BasePCBlockEntity.getMaxBattery(rank));
        data.raw().putBoolean("active", be.isActive());
        data.raw().putInt("effect", be.getCurrentEffect());
    }
}
