package maxigregrze.cobblesafari.cstrader.logic;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CsTraderVariantDefinition {
    private final String id;
    private final Set<String> aliases;
    private final List<CsTraderTradeGroup> tradeGroups;

    public CsTraderVariantDefinition(String id, List<String> aliases, List<CsTraderTradeGroup> tradeGroups) {
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
        this.tradeGroups = tradeGroups;
    }

    public String getId() {
        return id;
    }

    /** Each group is drawn independently; a legacy variant has a single group. */
    public List<CsTraderTradeGroup> getTradeGroups() {
        return tradeGroups;
    }

    public boolean matches(String variantIdOrAlias) {
        if (variantIdOrAlias == null) return false;
        return aliases.contains(variantIdOrAlias.toLowerCase(Locale.ROOT));
    }
}
