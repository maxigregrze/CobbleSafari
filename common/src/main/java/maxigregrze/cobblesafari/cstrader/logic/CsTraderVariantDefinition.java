package maxigregrze.cobblesafari.cstrader.logic;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CsTraderVariantDefinition {
    private final String id;
    private final Set<String> aliases;
    private final int minTrades;
    private final int maxTrades;
    private final List<CsTraderTradeDefinition> trades;

    public CsTraderVariantDefinition(String id, List<String> aliases, int minTrades, int maxTrades,
                                     List<CsTraderTradeDefinition> trades) {
        this.id = id;
        this.aliases = new HashSet<>();
        this.aliases.add(id.toLowerCase(Locale.ROOT));
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias != null && !alias.isBlank()) {
                    this.aliases.add(alias.toLowerCase(Locale.ROOT));
                }
            }
        }
        this.minTrades = minTrades;
        this.maxTrades = maxTrades;
        this.trades = trades;
    }

    public String getId() {
        return id;
    }

    public int getMinTrades() {
        return minTrades;
    }

    public int getMaxTrades() {
        return maxTrades;
    }

    public List<CsTraderTradeDefinition> getTrades() {
        return trades;
    }

    public boolean matches(String variantIdOrAlias) {
        if (variantIdOrAlias == null) return false;
        return aliases.contains(variantIdOrAlias.toLowerCase(Locale.ROOT));
    }
}
