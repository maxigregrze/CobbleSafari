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
    private static final String PHONE_VARIANT_PREFIX = "rotomphone_variant_";
    private static final String EARPIECE_VARIANT_PREFIX = "earpiece_variant_";
    private static final String DEFAULT_ID = "default";
    private static final String ITEM_PREFIX = "item/";
    private static final String JSON_EXT = ".json";

    private RotomPhoneTextureResolver() {}

    // --- Rotom Phone (rotomphone_variant_*) ---

    public static ResourceLocation modelForStack(ItemStack stack) {
        return modelFor(PHONE_VARIANT_PREFIX, RotomPhoneItem.getCurrentSkin(stack), RotomPhoneItem.isShiny(stack));
    }

    public static ResourceLocation modelFor(String skin, boolean shiny) {
        return modelFor(PHONE_VARIANT_PREFIX, skin, shiny);
    }

    public static List<ResourceLocation> allVariantModels(ResourceManager resourceManager) {
        return allVariantModels(PHONE_VARIANT_PREFIX, resourceManager);
    }

    // --- Rotie Earpiece (earpiece_variant_*), sharing the phone's per-skin textures ---

    public static ResourceLocation earpieceModelForStack(ItemStack stack) {
        return modelFor(EARPIECE_VARIANT_PREFIX, RotomPhoneItem.getCurrentSkin(stack), RotomPhoneItem.isShiny(stack));
    }

    public static List<ResourceLocation> allEarpieceVariantModels(ResourceManager resourceManager) {
        return allVariantModels(EARPIECE_VARIANT_PREFIX, resourceManager);
    }

    // --- Shared, prefix-parameterized implementation ---

    private static ResourceLocation modelFor(String variantPrefix, String skin, boolean shiny) {
        String id;
        if (skin == null || skin.isEmpty()) {
            id = DEFAULT_ID;
        } else {
            id = skin.toLowerCase(Locale.ROOT);
            if (!SKIN_ID_PATTERN.matcher(id).matches()) {
                id = DEFAULT_ID;
            }
        }
        String path = ITEM_PREFIX + variantPrefix + id + (shiny ? "_s" : "");
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, path);
    }

    private static List<ResourceLocation> allVariantModels(String variantPrefix, ResourceManager resourceManager) {
        Set<String> baseIds = new LinkedHashSet<>();
        baseIds.add(DEFAULT_ID);
        baseIds.addAll(discoverVariantSkinIds(variantPrefix, resourceManager));
        List<ResourceLocation> all = new ArrayList<>();
        for (String id : baseIds) {
            all.add(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, ITEM_PREFIX + variantPrefix + id));
            all.add(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, ITEM_PREFIX + variantPrefix + id + "_s"));
        }
        return all;
    }

    static Set<String> discoverVariantSkinIds(String variantPrefix, ResourceManager resourceManager) {
        Set<String> ids = new LinkedHashSet<>();
        String modId = CobbleSafari.MOD_ID;
        for (var entry : resourceManager.listResources(ITEM_MODEL_DIR, loc -> {
            if (!modId.equals(loc.getNamespace())) {
                return false;
            }
            String p = loc.getPath();
            return p.startsWith(ITEM_MODEL_DIR + "/") && p.startsWith(ITEM_MODEL_DIR + "/" + variantPrefix) && p.endsWith(JSON_EXT);
        }).entrySet()) {
            String name = entry.getKey().getPath().substring(ITEM_MODEL_DIR.length() + 1);
            if (!name.startsWith(variantPrefix) || !name.endsWith(JSON_EXT)) {
                continue;
            }
            name = name.substring(0, name.length() - JSON_EXT.length());
            if (name.endsWith("_s")) {
                name = name.substring(0, name.length() - 2);
            }
            if (name.length() > variantPrefix.length()) {
                ids.add(name.substring(variantPrefix.length()));
            }
        }
        return ids;
    }
}
