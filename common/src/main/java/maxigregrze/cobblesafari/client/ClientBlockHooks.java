package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballBlock;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballBlockEntity;
import maxigregrze.cobblesafari.block.misc.OnlineFeaturePcBlock;
import maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneGTSScreen;
import maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneUnionScreen;
import maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneWonderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Client-only wiring for common blocks whose behaviour needs the local player or a client screen.
 *
 * <p>These hooks let the common block classes stay free of any {@code net.minecraft.client}
 * reference — such a reference would fail class verification and crash a <b>dedicated server</b>
 * the moment {@code ModBlocks} constructs the block. This class lives in the client-only
 * {@code client} package and is loaded only from each loader's client entrypoint.
 */
public final class ClientBlockHooks {

    private ClientBlockHooks() {}

    public static void init() {
        AuspiciousPokeballBlock.particleGate = ClientBlockHooks::auspiciousParticlesVisible;
        OnlineFeaturePcBlock.screenOpener = ClientBlockHooks::openOnlineFeaturePcScreen;
    }

    /** Mirrors the block-entity-renderer world-model gate: hide particles the local player can't see. */
    private static boolean auspiciousParticlesVisible(Level level, BlockPos pos) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return false;
        }
        return !(level.getBlockEntity(pos) instanceof AuspiciousPokeballBlockEntity be
                && be.shouldHideWorldModelForLocalPlayer(localPlayer));
    }

    private static void openOnlineFeaturePcScreen(OnlineFeaturePcBlock.Kind kind) {
        Minecraft mc = Minecraft.getInstance();
        switch (kind) {
            case UNION -> mc.setScreen(RotomPhoneUnionScreen.forOnlinePc());
            case GTS -> mc.setScreen(RotomPhoneGTSScreen.forOnlinePc());
            case WONDER -> mc.setScreen(RotomPhoneWonderScreen.forOnlinePc());
            default -> { /* no screen for other kinds */ }
        }
    }
}
