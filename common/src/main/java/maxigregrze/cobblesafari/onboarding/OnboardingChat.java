package maxigregrze.cobblesafari.onboarding;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/**
 * Clickable chat fragments that open the onboarding, shared by the server (login invitation, save
 * confirmation) and the client (Esc reminder) - action plan 198, section 4.2.
 */
public final class OnboardingChat {

    public static final String COMMAND = "/cobblesafari onboarding";

    private OnboardingChat() {}

    /**
     * {@code text}, green and underlined, that runs the onboarding command <b>as the clicking
     * player</b>: the command's own permission applies to the link too.
     */
    public static Component runLink(Component text) {
        return text.copy().withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, COMMAND))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("cobblesafari.onboarding.chat.click"))));
    }

    /** A {@code key} line whose single argument is the command itself, as a link. */
    public static Component rerunHint(String key) {
        return Component.translatable(key, runLink(Component.literal(COMMAND)));
    }
}
