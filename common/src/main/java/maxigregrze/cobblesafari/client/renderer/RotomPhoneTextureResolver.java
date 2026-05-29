package maxigregrze.cobblesafari.client.renderer;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.item.RotomPhoneItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class RotomPhoneTextureResolver {

    private static final Pattern SKIN_ID_PATTERN = Pattern.compile("^[a-z0-9_]+$");
    private static final String ITEM_MODEL_DIR = "models/item";
    private static final String VARIANT_PREFIX = "rotomphone_variant_";
    private static final String DEFAULT_ID = "default";
    private static final String ITEM_PREFIX = "item/";
    private static final String JSON_EXT = ".json";

    private RotomPhoneTextureResolver() {}

    public static ResourceLocation modelForStack(ItemStack stack) {
        String skin = RotomPhoneItem.getCurrentSkin(stack);
        boolean shiny = RotomPhoneItem.isShiny(stack);
        return modelFor(skin, shiny);
    }

    public static ResourceLocation modelFor(String skin, boolean shiny) {
        String id;
        if (skin == null || skin.isEmpty()) {
            id = DEFAULT_ID;
        } else {
            id = skin.toLowerCase(Locale.ROOT);
            if (!SKIN_ID_PATTERN.matcher(id).matches()) {
                id = DEFAULT_ID;
            }
        }
        String path = ITEM_PREFIX + VARIANT_PREFIX + id + (shiny ? "_s" : "");
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, path);
    }

    public static List<ResourceLocation> allVariantModels(ResourceManager resourceManager) {
        Set<String> baseIds = new LinkedHashSet<>();
        baseIds.add(DEFAULT_ID);
        for (String id : discoverVariantSkinIds(resourceManager)) {
            baseIds.add(id);
        }
        List<ResourceLocation> all = new ArrayList<>();
        for (String id : baseIds) {
            all.add(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, ITEM_PREFIX + VARIANT_PREFIX + id));
            all.add(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, ITEM_PREFIX + VARIANT_PREFIX + id + "_s"));
        }
        return all;
    }

    static Set<String> discoverVariantSkinIds(ResourceManager resourceManager) {
        Set<String> ids = new LinkedHashSet<>();
        String modId = CobbleSafari.MOD_ID;
        for (var entry : resourceManager.listResources(ITEM_MODEL_DIR, loc -> {
            if (!modId.equals(loc.getNamespace())) {
                return false;
            }
            String p = loc.getPath();
            return p.startsWith(ITEM_MODEL_DIR + "/") && p.startsWith(ITEM_MODEL_DIR + "/" + VARIANT_PREFIX) && p.endsWith(JSON_EXT);
        }).entrySet()) {
            String name = entry.getKey().getPath().substring(ITEM_MODEL_DIR.length() + 1);
            if (!name.startsWith(VARIANT_PREFIX) || !name.endsWith(JSON_EXT)) {
                continue;
            }
            name = name.substring(0, name.length() - JSON_EXT.length());
            if (name.endsWith("_s")) {
                name = name.substring(0, name.length() - 2);
            }
            if (name.length() > VARIANT_PREFIX.length()) {
                ids.add(name.substring(VARIANT_PREFIX.length()));
            }
        }
        return ids;
    }
}
