package maxigregrze.cobblesafari.cstrader.logic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;

public record CsTraderTradeDefinition(
        Item sourceItem1,
        int sourceQty1,
        Item sourceItem2,
        int sourceQty2,
        List<CsTraderItemSpec> resultItems,
        List<CsTraderResultOption> resultOptions,
        List<ResourceLocation> resultTmPool,
        int resultQty,
        int resultQtyMin,
        int resultQtyMax,
        int priceShiftMax,
        int maxUses,
        int xp,
        float priceMultiplier
) {
    public boolean hasSecondSource() {
        return sourceItem2 != null && sourceQty2 > 0;
    }

    public boolean hasResultOptions() {
        return resultOptions != null && !resultOptions.isEmpty();
    }

    /**
     * Whether this trade hands out a Technical Machine.
     *
     * <p>The pool holds raw ids rather than resolved {@code TechnicalMachine}s: the TM registry is a
     * datapack registry with no ordering guarantee against our own resource reload, so resolution is
     * deferred to offer-generation time.
     */
    public boolean hasResultTmPool() {
        return resultTmPool != null && !resultTmPool.isEmpty();
    }
}
