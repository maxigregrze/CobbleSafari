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

    private static final String KEY_NICKNAME = "Nickname";
    private static final String KEY_HELD_ITEM = "HeldItem";

    /**
     * Modifies {@code nbt} in place. Minimal scope: nickname formatting, banned held items, status.
     */
    public static void sanitize(CompoundTag nbt, BanlistView banlist) {
        if (nbt.contains(KEY_NICKNAME, Tag.TAG_STRING)) {
            String n = nbt.getString(KEY_NICKNAME);
            n = n.replaceAll("§.", "");
            if (n.length() > 16) {
                n = n.substring(0, 16);
            }
            nbt.putString(KEY_NICKNAME, n);
        }
        if (nbt.contains(KEY_HELD_ITEM, Tag.TAG_COMPOUND)) {
            CompoundTag held = nbt.getCompound(KEY_HELD_ITEM);
            String itemId = held.contains("id", Tag.TAG_STRING) ? held.getString("id") : "";
            if (!itemId.isEmpty() && banlist.isHeldItemBanned(itemId.toLowerCase(Locale.ROOT))) {
                nbt.remove(KEY_HELD_ITEM);
            }
        }
        nbt.remove("Status");
        nbt.remove("StatusTurns");
    }
}
