package maxigregrze.cobblesafari.cstrader.logic;

import java.util.List;

/** A pool of trades from which {@code minTrades..maxTrades} distinct ones are drawn per trader. */
public record CsTraderTradeGroup(int minTrades, int maxTrades, List<CsTraderTradeDefinition> trades) {
}
