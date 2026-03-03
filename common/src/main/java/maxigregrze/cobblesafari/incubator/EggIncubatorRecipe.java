package maxigregrze.cobblesafari.incubator;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record EggIncubatorRecipe(
    ResourceLocation inputItem,
    List<String> outputs,
    int hatchTime,
    int shinyBoost
) {
}
