package maxigregrze.cobblesafari.item.donut;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DonutPowerRegistry {

    private static final Map<String, DonutPower> REGISTRY = new LinkedHashMap<>();

    private DonutPowerRegistry() {}

    public static void register(DonutPower power) {
        REGISTRY.put(power.id(), power);
    }

    public static DonutPower get(String id) {
        return REGISTRY.get(id);
    }

    public static List<DonutPower> byFlavor(DonutMainFlavor flavor) {
        return REGISTRY.values().stream()
                .filter(p -> p.flavor() == flavor)
                .collect(Collectors.toList());
    }

    public static List<DonutPower> byFlavorAndCategory(DonutMainFlavor flavor, int category) {
        return REGISTRY.values().stream()
                .filter(p -> p.flavor() == flavor && p.category() == category)
                .collect(Collectors.toList());
    }

    public static Collection<DonutPower> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static void init() {
        registerAll();
    }

    private static void registerAll() {
        // "alpha" power is not yet implemented in pre-1.8.0
        register(new DonutPower("friendship", DonutMainFlavor.SWEET, 1, 1, 50, 75, 100));
        register(new DonutPower("hidden", DonutMainFlavor.DRY, 2, 19, 10, 20, 30));
        register(new DonutPower("atypical", DonutMainFlavor.SWEET, 2, 19, 1, 2, 3));
        register(new DonutPower("sparkling", DonutMainFlavor.SWEET, 2, 19, 1, 2, 3));
        register(new DonutPower("capture", DonutMainFlavor.DRY, 2, 19, 10, 20, 30));
        register(new DonutPower("encounter", DonutMainFlavor.DRY, 1, 19, 10, 25, 50));
        register(new DonutPower("humongo", DonutMainFlavor.SWEET, 1, 1, 5, 10, 15));
        register(new DonutPower("luck", DonutMainFlavor.SOUR, 1, 1, 1, 3, 5));
        register(new DonutPower("salvage", DonutMainFlavor.SOUR, 2, 19, 1, 2, 3));
        register(new DonutPower("teensy", DonutMainFlavor.SWEET, 1, 1, 5, 10, 15));
        register(new DonutPower("attack", DonutMainFlavor.SPICY, 1, 1, 10, 20, 30));
        register(new DonutPower("defense", DonutMainFlavor.BITTER, 1, 1, 10, 20, 30));
        register(new DonutPower("sp_atk", DonutMainFlavor.SPICY, 1, 1, 10, 20, 30));
        register(new DonutPower("sp_def", DonutMainFlavor.BITTER, 1, 1, 10, 20, 30));
        register(new DonutPower("speed", DonutMainFlavor.SPICY, 1, 1, 10, 20, 30));
        register(new DonutPower("move", DonutMainFlavor.SPICY, 2, 19, 10, 20, 30));
        register(new DonutPower("resistance", DonutMainFlavor.BITTER, 2, 19, 10, 20, 30));
        register(new DonutPower("self_attack", DonutMainFlavor.SPICY, 2, 19, 10, 20, 30));
        register(new DonutPower("self_defense", DonutMainFlavor.BITTER, 1, 19, 10, 20, 30));
    }
}
