package maxigregrze.cobblesafari.client.objectives;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * "Objectives HUD toggler" keybind (default {@code O}). Toggles the client-local
 * session force-open flag (initialized from {@code hud_config.json}, not written back); purely
 * client-side, no networking.
 */
public final class ObjectivesKeybind {

    public static final String CATEGORY = "key.categories.cobblesafari";
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.cobblesafari.objectives_hud_toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY);

    private ObjectivesKeybind() {}

    public static void clientTick(Minecraft mc) {
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (TOGGLE_HUD.consumeClick()) {
            ObjectivesHudController.toggleForceOpen();
        }
    }
}
