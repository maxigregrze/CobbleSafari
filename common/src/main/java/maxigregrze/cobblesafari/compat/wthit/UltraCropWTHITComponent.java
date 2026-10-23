package maxigregrze.cobblesafari.compat.wthit;

import maxigregrze.cobblesafari.block.ultraspace.UltraCropBlock;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaConfig;
import mcp.mobius.waila.api.WailaConstants;

/** Names a young Ultra Beast crop as a sprout, a grown one as the plant (action plan 192, section 12). */
public class UltraCropWTHITComponent implements IBlockComponentProvider {

    @Override
    public void appendHead(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlock() instanceof UltraCropBlock crop) {
            tooltip.setLine(WailaConstants.OBJECT_NAME_TAG, IWailaConfig.get().getFormatter()
                    .blockName(crop.displayName(accessor.getBlockState()).getString()));
        }
    }
}
