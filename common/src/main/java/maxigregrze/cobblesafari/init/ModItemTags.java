package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {

    public static final TagKey<Item> TRAPS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "traps"));

    private ModItemTags() {}
}
