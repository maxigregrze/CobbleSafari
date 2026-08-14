package maxigregrze.cobblesafari.compat.wthit;

import maxigregrze.cobblesafari.compat.PokeStatueTooltipData;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.ITooltipComponent;
import mcp.mobius.waila.api.component.ItemComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class PokeStatueWTHITComponent implements IBlockComponentProvider {

    @Override
    public ITooltipComponent getIcon(IBlockAccessor accessor, IPluginConfig config) {
        // Show the statue's own inventory icon rather than the pedestal / invisible model.
        return new ItemComponent(new ItemStack(accessor.getBlock()));
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getData().raw();
        if (data == null) {
            return;
        }
        tooltip.addLine(PokeStatueTooltipData.speciesLine(data));
        if (PokeStatueTooltipData.isGlazed(data)) {
            tooltip.addLine(PokeStatueTooltipData.glazedLine());
        }
    }
}
