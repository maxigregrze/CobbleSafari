package maxigregrze.cobblesafari.power;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;

public final class PowerVariantRegistry {

    public static final int VARIANT_COUNT = 19;
    public static final int ELEMENTAL_COUNT = 18;
    public static final int INDEX_ALL = 18;

    private static final ElementalType[] ORDERED = {
            ElementalTypes.NORMAL,
            ElementalTypes.FIRE,
            ElementalTypes.WATER,
            ElementalTypes.GRASS,
            ElementalTypes.ELECTRIC,
            ElementalTypes.ICE,
            ElementalTypes.FIGHTING,
            ElementalTypes.POISON,
            ElementalTypes.GROUND,
            ElementalTypes.FLYING,
            ElementalTypes.PSYCHIC,
            ElementalTypes.BUG,
            ElementalTypes.ROCK,
            ElementalTypes.GHOST,
            ElementalTypes.DRAGON,
            ElementalTypes.DARK,
            ElementalTypes.STEEL,
            ElementalTypes.FAIRY
    };

    private PowerVariantRegistry() {}

    public static String suffix(int variantIndex) {
        if (variantIndex == INDEX_ALL) {
            return "all";
        }
        if (variantIndex < 0 || variantIndex >= ELEMENTAL_COUNT) {
            throw new IllegalArgumentException("variantIndex out of range: " + variantIndex);
        }
        return ORDERED[variantIndex].getShowdownId();
    }

    public static ElementalType elementalType(int variantIndex) {
        if (variantIndex < 0 || variantIndex >= ELEMENTAL_COUNT) {
            return null;
        }
        return ORDERED[variantIndex];
    }

    public static boolean pokemonHasVariantType(com.cobblemon.mod.common.pokemon.Pokemon pokemon, int variantIndex) {
        if (variantIndex == INDEX_ALL) {
            return true;
        }
        ElementalType t = elementalType(variantIndex);
        if (t == null) {
            return false;
        }
        for (ElementalType ct : pokemon.getTypes()) {
            if (ct.equals(t)) {
                return true;
            }
        }
        return false;
    }
}
