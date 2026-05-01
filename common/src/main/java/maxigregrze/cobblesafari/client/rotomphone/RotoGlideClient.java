package maxigregrze.cobblesafari.client.rotomphone;

import maxigregrze.cobblesafari.item.RotomPhoneItem;
import maxigregrze.cobblesafari.network.RotomPhoneRotoGlideRequestPayload;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneEquipped;
import maxigregrze.cobblesafari.rotomphone.RotoGlideServerLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class RotoGlideClient {

    private static boolean prevJumpDown = false;
    private static boolean sentThisFall = false;

    private RotoGlideClient() {}

    public static void tick(Minecraft minecraft) {
        if (minecraft.screen != null) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.isPaused()) {
            return;
        }

        boolean jumpDown = minecraft.options.keyJump.isDown();
        boolean justPressed = jumpDown && !prevJumpDown;
        prevJumpDown = jumpDown;

        if (player.onGround()) {
            sentThisFall = false;
            return;
        }

        if (player.getDeltaMovement().y >= 0) {
            return;
        }

        if (sentThisFall) {
            return;
        }

        if (!justPressed) {
            return;
        }

        if (RotoGlideServerLogic.isGloballyBlocked(player)) {
            return;
        }

        var phone = RotomPhoneEquipped.findPhoneForHandOrAccessory(player);
        if (phone.isEmpty() || !RotomPhoneItem.isRotoGlideEnabled(phone)) {
            return;
        }

        Vec3 d = player.getDeltaMovement();
        Services.PLATFORM.sendPayloadToServer(new RotomPhoneRotoGlideRequestPayload(d.x, d.z));
    }
}
