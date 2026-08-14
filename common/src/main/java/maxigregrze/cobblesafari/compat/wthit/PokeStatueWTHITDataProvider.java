package maxigregrze.cobblesafari.compat.wthit;

import maxigregrze.cobblesafari.block.pokestatue.AbstractPokeStatueBlockEntity;
import maxigregrze.cobblesafari.compat.PokeStatueTooltipData;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;

public class PokeStatueWTHITDataProvider implements IDataProvider<AbstractPokeStatueBlockEntity> {

    @Override
    public void appendData(IDataWriter data, IServerAccessor<AbstractPokeStatueBlockEntity> accessor, IPluginConfig config) {
        AbstractPokeStatueBlockEntity be = accessor.getTarget();
        PokeStatueTooltipData.write(data.raw(), be.getLevel(), be.getBlockPos(), be.getBlockState());
    }
}
