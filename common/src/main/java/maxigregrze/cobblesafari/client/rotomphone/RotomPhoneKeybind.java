package maxigregrze.cobblesafari.client.rotomphone;

import com.mojang.blaze3d.platform.InputConstants;
import maxigregrze.cobblesafari.network.OpenRotomPhoneRequestPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class RotomPhoneKeybind {

    public static final String CATEGORY = "key.categories.cobblesafari";
    public static final KeyMapping OPEN_PHONE = new KeyMapping(
            "key.cobblesafari.open_rotom_phone",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            CATEGORY);

    private RotomPhoneKeybind() {}

    public static void clientTick(Minecraft mc) {
        if (mc.player == null || mc.screen != null) return;
        while (OPEN_PHONE.consumeClick()) {
            Services.PLATFORM.sendPayloadToServer(new OpenRotomPhoneRequestPayload());
        }
    }
}
