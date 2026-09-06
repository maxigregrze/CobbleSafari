package maxigregrze.cobblesafari.cstrader.logic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A trade result item, optionally carrying data components.
 *
 * <p>Authored either as a bare id — {@code "cobblemon:rare_candy"} — or as an object:
 * <pre>
 * { "id": "cobblemon:technical_machine",
 *   "dataComponents": [ { "dataId": "cobblemon:tm_move", "dataValue": { "move": "thunderbolt" } } ] }
 * </pre>
 * Vanilla serialises a {@code MerchantOffer}'s result with {@code ItemStack.CODEC}, so the
 * components survive save/load and reach the client — the same path enchanted books use.
 */
public record CsTraderItemSpec(Item item, List<CsTraderComponentSpec> components) {

    public static CsTraderItemSpec of(Item item) {
        return new CsTraderItemSpec(item, List.of());
    }

    public ItemStack toStack(int qty) {
        ItemStack stack = new ItemStack(item, Math.max(1, qty));
        for (CsTraderComponentSpec component : components) {
            component.applyTo(stack);
        }
        return stack;
    }
}
