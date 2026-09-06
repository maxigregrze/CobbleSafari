package maxigregrze.cobblesafari.cstrader.logic;

import com.cobblemon.mod.common.api.tms.TechnicalMachine;
import com.cobblemon.mod.common.api.tms.TechnicalMachines;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
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
        ItemStack result = buildResult(trade, random);
        if (result == null || result.isEmpty()) {
            return null;
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
                    result,
                    Math.max(1, trade.maxUses()),
                    Math.max(0, trade.xp()),
                    trade.priceMultiplier()
            );
        }

        return new MerchantOffer(
                new ItemCost(trade.sourceItem1(), Math.max(1, price1)),
                result,
                Math.max(1, trade.maxUses()),
                Math.max(0, trade.xp()),
                trade.priceMultiplier()
        );
    }

    private static ItemStack buildResult(CsTraderTradeDefinition trade, RandomSource random) {
        if (trade.hasResultTmPool()) {
            return buildTmResult(trade, random);
        }

        CsTraderItemSpec spec;
        int resultQty;
        if (trade.hasResultOptions()) {
            CsTraderResultOption chosen = trade.resultOptions().get(random.nextInt(trade.resultOptions().size()));
            spec = chosen.spec();
            resultQty = chosen.qty();
        } else {
            if (trade.resultItems().isEmpty()) {
                return null;
            }
            spec = trade.resultItems().get(random.nextInt(trade.resultItems().size()));
            if (trade.resultQtyMin() > 0 && trade.resultQtyMax() >= trade.resultQtyMin()) {
                int max = trade.resultQtyMax();
                int min = trade.resultQtyMin();
                resultQty = min + (max > min ? random.nextInt(max - min + 1) : 0);
            } else {
                resultQty = trade.resultQty();
            }
        }
        return spec.toStack(resultQty);
    }

    /**
     * Resolves the TM pool now rather than at datapack-load time: {@code TechnicalMachines} is a
     * datapack registry with no ordering guarantee against our own resource reload. Unresolvable
     * ids drop out of the draw; if the whole pool is dead the offer is simply not produced.
     */
    private static ItemStack buildTmResult(CsTraderTradeDefinition trade, RandomSource random) {
        List<TechnicalMachine> resolved = new ArrayList<>();
        for (ResourceLocation id : trade.resultTmPool()) {
            TechnicalMachine tm = TechnicalMachines.INSTANCE.getByResourceLocation(id);
            if (tm == null) {
                CobbleSafari.LOGGER.warn("[CSTrader] Unknown technical machine '{}', skipped", id);
                continue;
            }
            resolved.add(tm);
        }
        if (resolved.isEmpty()) {
            return null;
        }
        return resolved.get(random.nextInt(resolved.size())).createItemStack();
    }
}
