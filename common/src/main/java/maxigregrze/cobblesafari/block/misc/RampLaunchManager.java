package maxigregrze.cobblesafari.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Launch logic for {@link BelltowerRampBlock}: one ballistic impulse, then vanilla physics.
 *
 * <h2>Why a single impulse and not a scripted per-tick trajectory</h2>
 * Player movement is <b>client-authoritative</b>. The server never simulates a player's walk; it
 * accepts the position the client reports. Re-asserting the velocity every tick therefore means
 * firing a {@link ClientboundSetEntityMotionPacket} per tick and hoping each one lands before the
 * client's next physics step - meanwhile the client is applying its own gravity and drag between
 * packets. The result drifts, and drifts by a different amount depending on latency and tick
 * ordering, which is exactly what "sometimes too far, sometimes too short" looks like.
 *
 * <p>One impulse sidesteps all of it: the client simulates the whole arc deterministically from a
 * single velocity, the way vanilla slime blocks and wind charges do. Mobs are server-simulated and
 * get the identical arc for free.</p>
 *
 * <h2>The arc</h2>
 * Solved against vanilla player air physics ({@code pos += v}; {@code v.y = (v.y - 0.08) * 0.98};
 * {@code v.xz *= 0.91}): an initial {@code (0.238 forward, 0.545 up)} peaks at <b>exactly 2 blocks</b>
 * on tick 7 and returns to the launch height <b>exactly 2 blocks forward</b> on tick 15 - the centre
 * of the second block along, so a ramp / gap / ramp row chains dead centre.
 *
 * <p><b>Known variable:</b> the player keeps normal air control (no input lock - see plan §3.4b), so
 * holding forward or back shifts the landing point by a few tenths of a block. The landing target is
 * the centre of the destination block, which absorbs that.</p>
 */
public final class RampLaunchManager {

    /** Upward impulse: apex exactly 2 blocks above the pad, on tick 7. */
    private static final double LAUNCH_UP = 0.545;
    /** Forward impulse: exactly 2 blocks travelled by the time the arc returns to pad height. */
    private static final double LAUNCH_FORWARD = 0.238;

    /**
     * Debounce window. Guards the one case where a single launch could fire twice: an entity whose
     * bounding box straddles two adjacent ramp cells gets an {@code entityInside} call from each in
     * the same tick, and the second would overwrite the first (possibly with a different facing).
     */
    private static final int DEBOUNCE_TICKS = 4;

    private static final Map<UUID, Long> LAST_LAUNCH = new ConcurrentHashMap<>();

    private RampLaunchManager() {}

    /**
     * Called from {@link BelltowerRampBlock#entityInside}. Requires the entity to actually be
     * resting on the pad: {@code entityInside} fires as soon as the bounding box enters the block
     * cell, which for a 1-pixel pad is a full block above the surface, and launching from there
     * would vary the take-off point by up to a block. {@code onGround()} has already been updated
     * for this tick by the time {@code checkInsideBlocks()} runs, so it is safe to test here.
     */
    public static void tryLaunch(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (living.isSpectator() || !living.onGround()) {
            return;
        }
        if (living instanceof Player player && (player.getAbilities().flying || player.isFallFlying())) {
            return;
        }
        long now = level.getGameTime();
        UUID id = living.getUUID();
        Long last = LAST_LAUNCH.get(id);
        if (last != null && now - last < DEBOUNCE_TICKS) {
            return;
        }
        prune(now);
        LAST_LAUNCH.put(id, now);

        Direction facing = state.getValue(BelltowerRampBlock.FACING);
        living.setDeltaMovement(
                facing.getStepX() * LAUNCH_FORWARD, LAUNCH_UP, facing.getStepZ() * LAUNCH_FORWARD);
        if (living instanceof ServerPlayer serverPlayer) {
            // The client owns player movement; without this packet the impulse is discarded.
            serverPlayer.connection.send(
                    new ClientboundSetEntityMotionPacket(serverPlayer.getId(), serverPlayer.getDeltaMovement()));
        } else {
            living.hurtMarked = true;
        }
    }

    /** Drops debounce entries that can no longer match, so the map stays at "entities launched just now". */
    private static void prune(long now) {
        Iterator<Map.Entry<UUID, Long>> it = LAST_LAUNCH.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() >= DEBOUNCE_TICKS) {
                it.remove();
            }
        }
    }
}
