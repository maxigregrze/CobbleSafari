package maxigregrze.cobblesafari.cstrader.logic;

import maxigregrze.cobblesafari.CobbleSafari;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CsTraderRegistry {
    private CsTraderRegistry() {}

    private static final Map<String, CsTraderDefinition> TRADERS = new HashMap<>();

    public static void clear() {
        TRADERS.clear();
    }

    public static void register(CsTraderDefinition definition) {
        TRADERS.put(definition.getName(), definition);
    }

    public static CsTraderDefinition getTrader(String traderName) {
        if (traderName == null) return null;
        return TRADERS.get(traderName.toLowerCase(Locale.ROOT));
    }

    public static Collection<CsTraderDefinition> getAll() {
        return TRADERS.values();
    }

    public static List<String> getTraderNames() {
        return new ArrayList<>(TRADERS.keySet());
    }

    public static String getDefaultVariantId(String traderName) {
        CsTraderDefinition definition = getTrader(traderName);
        if (definition == null) return "small_sphere";
        return definition.getDefaultVariantId();
    }

    public static void logSummary() {
        CobbleSafari.LOGGER.info("Loaded {} cstrader definitions", TRADERS.size());
    }
}
