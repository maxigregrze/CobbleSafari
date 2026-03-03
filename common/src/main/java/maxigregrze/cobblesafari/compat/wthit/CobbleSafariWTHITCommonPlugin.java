package maxigregrze.cobblesafari.compat.wthit;

import maxigregrze.cobblesafari.block.basepc.BasePCBlockEntity;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.block.incubator.IncubatorBlockEntity;
import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IWailaCommonPlugin;

@SuppressWarnings("unused")
public class CobbleSafariWTHITCommonPlugin implements IWailaCommonPlugin {
    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.blockData(new DungeonPortalWTHITDataProvider(), DungeonPortalBlockEntity.class);
        registrar.blockData(new IncubatorWTHITDataProvider(), IncubatorBlockEntity.class);
        registrar.blockData(new BasePCWTHITDataProvider(), BasePCBlockEntity.class);
    }
}
