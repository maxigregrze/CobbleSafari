package maxigregrze.cobblesafari.client.teleporter;

import maxigregrze.cobblesafari.block.teleporter.TeleportPadBlock;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.network.TeleportPadJumpPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Detects the jump or sneak key client-side while the player stands on a teleport pad and forwards a
 * signal so the server can teleport. Edge-detected like {@code RotoGlideClient}. Sneaking is also the
 * config key (shift-right-click), so the sneak trigger is suppressed during the GUI-open gesture
 * (holding a Tinkhammer on a survival pad, or creative + empty hand on a creative pad).
 */
public final class TeleportPadClient {

    private static boolean prevJumpDown = false;
    private static boolean prevSneakDown = false;
    private static boolean prevOnGround = false;

    private TeleportPadClient() {}

    public static void tick(Minecraft minecraft) {
        if (minecraft.screen != null || minecraft.isPaused()) {
            return;
        }
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        boolean jumpDown = minecraft.options.keyJump.isDown();
        boolean jumpPressed = jumpDown && !prevJumpDown;
        prevJumpDown = jumpDown;

        boolean sneakDown = minecraft.options.keyShift.isDown();
        boolean sneakPressed = sneakDown && !prevSneakDown;
        prevSneakDown = sneakDown;

        // Runs after the player tick, so the jump impulse has already cleared onGround on the press
        // tick: accept "grounded last tick OR this tick" (sneaking stays grounded, so this is a no-op
        // for the sneak path) while still ignoring sustained flight (both false).
        boolean onGround = player.onGround();
        boolean grounded = onGround || prevOnGround;
        prevOnGround = onGround;

        if (!grounded || (!jumpPressed && !sneakPressed)) {
            return;
        }
        BlockState pad = padUnder(level, player.blockPosition());
        if (pad == null) {
            return;
        }
        // Jump always teleports. Sneak teleports too, except while doing the GUI-open gesture (shift is
        // shared with shift-right-click), so configuring a pad doesn't fling the player away.
        if (!jumpPressed && opensConfigGui(player, pad)) {
            return;
        }
        Services.PLATFORM.sendPayloadToServer(new TeleportPadJumpPayload());
    }

    @Nullable
    private static BlockState padUnder(Level level, BlockPos feet) {
        BlockState state = level.getBlockState(feet);
        if (state.getBlock() instanceof TeleportPadBlock) {
            return state;
        }
        state = level.getBlockState(feet.below());
        return state.getBlock() instanceof TeleportPadBlock ? state : null;
    }

    /** True when the player's current state is the gesture that opens/uses the pad config GUI. */
    private static boolean opensConfigGui(LocalPlayer player, BlockState pad) {
        if (pad.is(ModBlocks.SURVIVAL_TELEPORT_PAD)) {
            return player.getMainHandItem().is(ModItems.TINKHAMMER)
                    || player.getOffhandItem().is(ModItems.TINKHAMMER);
        }
        if (pad.is(ModBlocks.TELEPORT_PAD)) {
            return player.getAbilities().instabuild && player.getMainHandItem().isEmpty();
        }
        return false;
    }
}
