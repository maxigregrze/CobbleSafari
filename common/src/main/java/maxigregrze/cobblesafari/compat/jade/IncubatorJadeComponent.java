package maxigregrze.cobblesafari.compat.jade;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.incubator.CobbreedingCompat;
import maxigregrze.cobblesafari.block.incubator.IncubatorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum IncubatorJadeComponent implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String KEY_TIME_READY = "time_ready";
    private static final String KEY_TIME_ARG = "time_arg";
    private static final String KEY_EGG_TYPE = "egg_type";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (serverData.getBoolean(KEY_TIME_READY)) {
            tooltip.add(Component.translatable("cobblesafari.waila.incubator.ready"));
        } else if (serverData.contains(KEY_TIME_ARG)) {
            tooltip.add(Component.translatable("cobblesafari.waila.incubator.time_remaining", serverData.getString(KEY_TIME_ARG)));
        }
        if (serverData.contains(KEY_EGG_TYPE)) {
            tooltip.add(Component.translatable("cobblesafari.waila.incubator.contains", serverData.getString(KEY_EGG_TYPE)));
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof IncubatorBlockEntity incubator)) return;

        int ticksRemaining = incubator.getTicksRemaining();
        ItemStack input = incubator.getInputItem();
        boolean isCobbreeding = incubator.isCobbreedingEgg();
        String storedName = incubator.getStoredEggSpeciesName();

        boolean hasEgg = !input.isEmpty() || (isCobbreeding && (storedName != null && !storedName.isEmpty()) || ticksRemaining >= 0);
        if (!hasEgg) return;

        if (ticksRemaining <= 0) {
            tag.putBoolean(KEY_TIME_READY, true);
        } else {
            tag.putBoolean(KEY_TIME_READY, false);
            int totalSeconds = ticksRemaining / 20;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            tag.putString(KEY_TIME_ARG, String.format("%02d:%02d", minutes, seconds));
        }

        String itemName = getEggTypeDisplayName(incubator);
        if (itemName != null) {
            tag.putString(KEY_EGG_TYPE, itemName);
        }
    }

    private static String getEggTypeDisplayName(IncubatorBlockEntity incubator) {
        ItemStack input = incubator.getInputItem();
        boolean isCobbreeding = incubator.isCobbreedingEgg();
        String storedName = incubator.getStoredEggSpeciesName() != null ? incubator.getStoredEggSpeciesName() : "";

        if (isCobbreeding && !CobbreedingCompat.isCobbreedingLoaded()) {
            String species = storedName.isEmpty() ? "Unknown" : storedName.substring(0, 1).toUpperCase() + storedName.substring(1);
            return Component.translatable("cobblesafari.incubator.strange_egg", species).getString();
        }
        if (isCobbreeding && CobbreedingCompat.isCobbreedingLoaded() && !input.isEmpty()) {
            String speciesName = CobbreedingCompat.getEggName(input);
            if (speciesName != null && !speciesName.isEmpty()) {
                return speciesName.substring(0, 1).toUpperCase() + speciesName.substring(1) + " Egg";
            }
            return "Unknown Egg";
        }
        if (!input.isEmpty()) {
            return input.getHoverName().getString();
        }
        return null;
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "incubator");
    }
}
