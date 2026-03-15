package maxigregrze.cobblesafari.cstrader.logic;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class CsTraderOfferFactory {
    private CsTraderOfferFactory() {}

    public static List<MerchantOffer> generateOffers(String traderName, String variantId, RandomSource random) {
        CsTraderDefinition trader = CsTraderRegistry.getTrader(traderName);
        if (trader == null) return List.of();
        CsTraderVariantDefinition variant = trader.resolveVariant(variantId);
        if (variant == null) return List.of();

        List<CsTraderTradeDefinition> pool = new ArrayList<>(variant.getTrades());
        if (pool.isEmpty()) return List.of();

        Collections.shuffle(pool, new Random(random.nextLong()));
        int maxPick = Math.min(variant.getMaxTrades(), pool.size());
        int minPick = Math.max(1, Math.min(variant.getMinTrades(), maxPick));
        int count = minPick + (maxPick > minPick ? random.nextInt(maxPick - minPick + 1) : 0);

        List<MerchantOffer> offers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MerchantOffer offer = buildOffer(pool.get(i), random);
            if (offer != null) {
                offers.add(offer);
            }
        }
        return offers;
    }

    private static MerchantOffer buildOffer(CsTraderTradeDefinition trade, RandomSource random) {
        Item resultItem;
        int resultQty;
        if (trade.hasResultOptions()) {
            CsTraderResultOption chosen = trade.resultOptions().get(random.nextInt(trade.resultOptions().size()));
            resultItem = chosen.item();
            resultQty = chosen.qty();
        } else {
            resultItem = trade.resultItems().get(random.nextInt(trade.resultItems().size()));
            if (trade.resultQtyMin() > 0 && trade.resultQtyMax() >= trade.resultQtyMin()) {
                int max = trade.resultQtyMax();
                int min = trade.resultQtyMin();
                resultQty = min + (max > min ? random.nextInt(max - min + 1) : 0);
            } else {
                resultQty = trade.resultQty();
            }
        }

        int price1 = trade.sourceQty1();
        int price2 = trade.sourceQty2();
        if (trade.hasSecondSource() && trade.priceShiftMax() > 0 && random.nextBoolean()) {
            int shift = random.nextInt(trade.priceShiftMax() + 1);
            price1 = Math.max(1, price1 - shift);
            price2 = Math.max(1, price2 + shift);
        }

        if (trade.hasSecondSource()) {
            return new MerchantOffer(
                    new ItemCost(trade.sourceItem1(), Math.max(1, price1)),
                    Optional.of(new ItemCost(trade.sourceItem2(), Math.max(1, price2))),
                    new ItemStack(resultItem, Math.max(1, resultQty)),
                    Math.max(1, trade.maxUses()),
                    Math.max(0, trade.xp()),
                    trade.priceMultiplier()
            );
        }

        return new MerchantOffer(
                new ItemCost(trade.sourceItem1(), Math.max(1, price1)),
                new ItemStack(resultItem, Math.max(1, resultQty)),
                Math.max(1, trade.maxUses()),
                Math.max(0, trade.xp()),
                trade.priceMultiplier()
        );
    }
}
