package maxigregrze.cobblesafari.objectives;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime registry of dimension → objective definition, rebuilt on datapack (re)load (plan 118 §2.3).
 */
public final class DimensionalObjectivesRegistry {

    private static final Map<ResourceLocation, DimensionalObjectivesDefinition> BY_DIMENSION = new HashMap<>();

    private DimensionalObjectivesRegistry() {}

    public static void clear() {
        BY_DIMENSION.clear();
    }

    public static void register(DimensionalObjectivesDefinition def) {
        BY_DIMENSION.put(def.dimensionId(), def);
    }

    public static void unregister(ResourceLocation dimensionId) {
        BY_DIMENSION.remove(dimensionId);
    }

    public static DimensionalObjectivesDefinition get(ResourceLocation dimensionId) {
        return BY_DIMENSION.get(dimensionId);
    }

    public static boolean has(ResourceLocation dimensionId) {
        return BY_DIMENSION.containsKey(dimensionId);
    }
}
