package maxigregrze.cobblesafari.compat.jade;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.compat.PokeStatueTooltipData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

public enum PokeStatueJadeComponent implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
        // Show the statue's own inventory icon rather than the pedestal / invisible model.
        return IElementHelper.get().item(new ItemStack(accessor.getBlock()));
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null) {
            return;
        }
        tooltip.add(PokeStatueTooltipData.speciesLine(data));
        if (PokeStatueTooltipData.isGlazed(data)) {
            tooltip.add(PokeStatueTooltipData.glazedLine());
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        PokeStatueTooltipData.write(tag, accessor.getLevel(), accessor.getPosition(), accessor.getBlockState());
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "pokemon_statue");
    }
}
