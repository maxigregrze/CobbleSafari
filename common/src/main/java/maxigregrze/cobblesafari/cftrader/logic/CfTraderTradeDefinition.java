package maxigregrze.cobblesafari.cftrader.logic;

import net.minecraft.world.item.Item;

import java.util.List;

public record CfTraderTradeDefinition(
        Item sourceItem1,
        int sourceQty1,
        Item sourceItem2,
        int sourceQty2,
        List<Item> resultItems,
        List<CfTraderResultOption> resultOptions,
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
}
