package maxigregrze.cobblesafari.compat.jade;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.ultraspace.UltraCropBlock;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/** Names a young Ultra Beast crop as a sprout, a grown one as the plant (action plan 192, section 12). */
public enum UltraCropJadeComponent implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlock() instanceof UltraCropBlock crop) {
            tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(crop.displayName(accessor.getBlockState())));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "ultra_crop_name");
    }
}
