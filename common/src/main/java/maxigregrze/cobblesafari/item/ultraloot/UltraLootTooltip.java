package maxigregrze.cobblesafari.item.ultraloot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Two-line tooltip shared by the Ultra Space loot items (action plan 192, section 2.4): an italic
 * flavour line, then a plain line describing the behaviour.
 */
public final class UltraLootTooltip {

    private static final String PREFIX = "tooltip.cobblesafari.";

    private UltraLootTooltip() {}

    public static void append(List<Component> tooltip, String key) {
        append(tooltip, key, PREFIX + key + ".line2");
    }

    /** Same, with an explicit translation key for the second line (state-dependent wording). */
    public static void append(List<Component> tooltip, String key, String line2Key) {
        tooltip.add(Component.translatable(PREFIX + key + ".line1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(line2Key).withStyle(ChatFormatting.GRAY));
    }
}
