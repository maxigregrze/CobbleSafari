package maxigregrze.cobblesafari.client.renderer;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Function;

/**
 * Standalone baked models of entities drawn from Java block models: the Ultra Space loot projectiles
 * (action plan 192, section 7.4) and the Safari exit drone (action plan 198, part G). Each loader
 * bakes {@link #STANDALONE} and installs the lookup, as for {@link UltraWarpDiveModels}.
 */
public final class UltraLootModels {

    public static final ResourceLocation CELESTEELA_PROJECTILE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "entity/celesteela_projectile");
    public static final ResourceLocation PAPER_PLANE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "entity/paper_plane");

    public static final ResourceLocation SAFARI_EXIT_DRONE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "entity/safari_exit_drone");

    public static final List<ResourceLocation> STANDALONE = List.of(CELESTEELA_PROJECTILE, PAPER_PLANE, SAFARI_EXIT_DRONE);

    private static Function<ResourceLocation, BakedModel> standaloneLookup;

    private UltraLootModels() {}

    public static void setStandaloneLookup(Function<ResourceLocation, BakedModel> lookup) {
        standaloneLookup = lookup;
    }

    public static BakedModel get(ResourceLocation id) {
        return standaloneLookup != null
                ? standaloneLookup.apply(id)
                : Minecraft.getInstance().getModelManager().getMissingModel();
    }
}
