package maxigregrze.cobblesafari.client.renderer;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Function;

/**
 * Standalone baked model of the floating Lost Item's aura (action plan 193, section 7.3). Each loader
 * bakes {@link #STANDALONE} and installs the lookup, as for {@link UltraLootModels}.
 */
public final class LostItemModels {

    public static final ResourceLocation FLOATING_AURA =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "block/lost_item_floating_aura");

    public static final List<ResourceLocation> STANDALONE = List.of(FLOATING_AURA);

    private static Function<ResourceLocation, BakedModel> standaloneLookup;

    private LostItemModels() {}

    public static void setStandaloneLookup(Function<ResourceLocation, BakedModel> lookup) {
        standaloneLookup = lookup;
    }

    public static BakedModel get(ResourceLocation id) {
        return standaloneLookup != null
                ? standaloneLookup.apply(id)
                : Minecraft.getInstance().getModelManager().getMissingModel();
    }
}
