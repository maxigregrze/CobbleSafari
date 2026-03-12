package maxigregrze.cobblesafari.cftrader.logic;

import maxigregrze.cobblesafari.CobbleSafari;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CfTraderRegistry {
    private CfTraderRegistry() {}

    private static final Map<String, CfTraderDefinition> TRADERS = new HashMap<>();

    public static void clear() {
        TRADERS.clear();
    }

    public static void register(CfTraderDefinition definition) {
        TRADERS.put(definition.getName(), definition);
    }

    public static CfTraderDefinition getTrader(String traderName) {
        if (traderName == null) return null;
        return TRADERS.get(traderName.toLowerCase(Locale.ROOT));
    }

    public static Collection<CfTraderDefinition> getAll() {
        return TRADERS.values();
    }

    public static List<String> getTraderNames() {
        return new ArrayList<>(TRADERS.keySet());
    }

    public static String getDefaultVariantId(String traderName) {
        CfTraderDefinition definition = getTrader(traderName);
        if (definition == null) return "small_sphere";
        return definition.getDefaultVariantId();
    }

    public static void logSummary() {
        CobbleSafari.LOGGER.info("Loaded {} cftrader definitions", TRADERS.size());
    }
}
