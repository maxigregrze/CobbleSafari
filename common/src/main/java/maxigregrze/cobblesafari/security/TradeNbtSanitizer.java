package maxigregrze.cobblesafari.security;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Locale;

public final class TradeNbtSanitizer {

    /** Allows each service (GTS / Wonder) to supply its own held-item banlist. */
    public interface BanlistView {
        boolean isHeldItemBanned(String id);
    }

    private TradeNbtSanitizer() {}

    /**
     * Modifies {@code nbt} in place. Minimal scope: nickname formatting, banned held items, status.
     */
    public static void sanitize(CompoundTag nbt, BanlistView banlist) {
        if (nbt.contains("Nickname", Tag.TAG_STRING)) {
            String n = nbt.getString("Nickname");
            n = n.replaceAll("§.", "");
            if (n.length() > 16) {
                n = n.substring(0, 16);
            }
            nbt.putString("Nickname", n);
        }
        if (nbt.contains("HeldItem", Tag.TAG_COMPOUND)) {
            CompoundTag held = nbt.getCompound("HeldItem");
            String itemId = held.contains("id", Tag.TAG_STRING) ? held.getString("id") : "";
            if (!itemId.isEmpty() && banlist.isHeldItemBanned(itemId.toLowerCase(Locale.ROOT))) {
                nbt.remove("HeldItem");
            }
        }
        nbt.remove("Status");
        nbt.remove("StatusTurns");
    }
}
