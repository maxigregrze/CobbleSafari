package maxigregrze.cobblesafari.compat.wthit;

import maxigregrze.cobblesafari.incubator.CobbreedingCompat;
import maxigregrze.cobblesafari.block.incubator.IncubatorBlockEntity;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class IncubatorWTHITDataProvider implements IDataProvider<IncubatorBlockEntity> {
    private static final String KEY_TIME_READY = "time_ready";
    private static final String KEY_TIME_ARG = "time_arg";
    private static final String KEY_EGG_TYPE = "egg_type";

    @Override
    public void appendData(IDataWriter data, IServerAccessor<IncubatorBlockEntity> accessor, IPluginConfig config) {
        IncubatorBlockEntity incubator = accessor.getTarget();
        int ticksRemaining = incubator.getTicksRemaining();
        ItemStack input = incubator.getInputItem();
        boolean isCobbreeding = incubator.isCobbreedingEgg();
        String storedName = incubator.getStoredEggSpeciesName();

        boolean hasEgg = !input.isEmpty() || (isCobbreeding && (storedName != null && !storedName.isEmpty()) || ticksRemaining >= 0);
        if (!hasEgg) return;

        if (ticksRemaining <= 0) {
            data.raw().putBoolean(KEY_TIME_READY, true);
        } else {
            data.raw().putBoolean(KEY_TIME_READY, false);
            int totalSeconds = ticksRemaining / 20;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            data.raw().putString(KEY_TIME_ARG, String.format("%02d:%02d", minutes, seconds));
        }

        String itemName = getEggTypeDisplayName(incubator);
        if (itemName != null) {
            data.raw().putString(KEY_EGG_TYPE, itemName);
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
}
