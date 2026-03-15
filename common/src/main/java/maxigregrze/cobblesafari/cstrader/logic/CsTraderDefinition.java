package maxigregrze.cobblesafari.cstrader.logic;

import java.util.List;
import java.util.Locale;

public class CsTraderDefinition {
    private final String name;
    private final String displayName;
    private final String textureFile;
    private final boolean killeable;
    private final List<CsTraderVariantDefinition> variants;

    public CsTraderDefinition(String name, String displayName, String textureFile, boolean killeable,
                              List<CsTraderVariantDefinition> variants) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.displayName = (displayName == null || displayName.isBlank()) ? name : displayName;
        this.textureFile = textureFile;
        this.killeable = killeable;
        this.variants = variants;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTextureFile() {
        return textureFile;
    }

    public boolean isKilleable() {
        return killeable;
    }

    public List<CsTraderVariantDefinition> getVariants() {
        return variants;
    }

    public CsTraderVariantDefinition resolveVariant(String variantIdOrAlias) {
        if (variants == null || variants.isEmpty()) return null;
        if (variantIdOrAlias == null || variantIdOrAlias.isBlank()) return variants.getFirst();
        for (CsTraderVariantDefinition variant : variants) {
            if (variant.matches(variantIdOrAlias)) return variant;
        }
        return null;
    }

    public String getDefaultVariantId() {
        CsTraderVariantDefinition variant = resolveVariant(null);
        return variant == null ? "default" : variant.getId();
    }
}
