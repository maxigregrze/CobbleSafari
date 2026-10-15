package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.block.dungeon.PortalSpawnerBlockEntity;
import maxigregrze.cobblesafari.block.dungeon.PortalSpawnerMode;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Validates and applies {@link SavePortalSpawnerConfigPayload} (action plan 188, section 12.8). */
public final class PortalSpawnerConfigServerHandler {

    private static final double MAX_DISTANCE = 8.0D;

    private PortalSpawnerConfigServerHandler() {
    }

    public static void handleSave(ServerPlayer player, SavePortalSpawnerConfigPayload payload) {
        if (!player.isCreative()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!player.blockPosition().closerThan(payload.pos(), MAX_DISTANCE)) {
            return;
        }
        if (!level.getBlockState(payload.pos()).is(ModBlocks.PORTAL_SPAWNER)
                || !(level.getBlockEntity(payload.pos()) instanceof PortalSpawnerBlockEntity be)) {
            return;
        }
        String destination = payload.destination() == null ? "" : payload.destination().trim();
        if (!isValidDestination(destination)) {
            // An invented id is refused: the spawner keeps its current destination.
            destination = be.getDestination();
        }
        be.applySettings(level, new PortalSpawnerBlockEntity.Settings(
                payload.enabled(),
                destination,
                payload.lifespanSeconds(),
                PortalSpawnerMode.byOrdinal(payload.mode()),
                payload.intervalSeconds(),
                payload.minIntervalSeconds(),
                payload.maxIntervalSeconds(),
                payload.minSignal()));
    }

    /** {@code ""} (random) or a known dungeon that is not a sub-dungeon. */
    private static boolean isValidDestination(String id) {
        if (id.isEmpty()) {
            return true;
        }
        DungeonConfig config = DungeonDimensions.getDungeonById(id);
        return config != null && !config.isSubDungeon();
    }
}
