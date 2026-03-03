package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.config.DimensionTimerEntry;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.network.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonTeleportCountdown {

    private static final int COUNTDOWN_TICKS = 100;
    private static final double MOVE_THRESHOLD_SQ = 0.04;
    private static final Map<UUID, TeleportState> pendingTeleports = new ConcurrentHashMap<>();

    private static final int PHASE_POSITION_CALCULATION = 95;
    private static final int PHASE_CHUNK_LOADING = 90;
    private static final int PHASE_STRUCTURE_GENERATION = 85;
    private static final int PHASE_BLOCK_REPLACEMENT_START = 80;
    private static final int PHASE_BLOCK_REPLACEMENT_STEP_2 = 60;
    private static final int PHASE_BLOCK_REPLACEMENT_STEP_3 = 40;
    private static final int PHASE_BLOCK_REPLACEMENT_STEP_4 = 20;

    private DungeonTeleportCountdown() {}

    public static void register() {
    }

    public static boolean startTeleportSequence(ServerPlayer player, DungeonPortalBlockEntity portalEntity) {
        UUID playerId = player.getUUID();

        if (pendingTeleports.containsKey(playerId)) {
            return false;
        }

        DungeonTeleportHandler.DungeonValidationResult validation =
                DungeonTeleportHandler.validateDungeonEntry(player, portalEntity);
        if (validation == null) {
            return false;
        }

        Vec3 startPos = player.position();

        pendingTeleports.put(playerId, new TeleportState(
                portalEntity.getBlockPos(),
                player.level().dimension(),
                startPos,
                COUNTDOWN_TICKS,
                validation,
                null,
                false
        ));

        int secondsTotal = COUNTDOWN_TICKS / 20;

        CobbleSafari.LOGGER.info("Started dungeon teleport countdown for player {} ({} seconds)",
                player.getName().getString(), secondsTotal);

        return true;
    }

    public static boolean isTeleporting(UUID playerId) {
        return pendingTeleports.containsKey(playerId);
    }

    public static void cancelTeleport(UUID playerId) {
        pendingTeleports.remove(playerId);
    }

    public static void onServerTick(MinecraftServer server) {
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, TeleportState> entry : pendingTeleports.entrySet()) {
            UUID playerId = entry.getKey();
            TeleportState state = entry.getValue();

            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            if (player == null) {
                toRemove.add(playerId);
                continue;
            }

            Vec3 currentPos = player.position();
            double dx = currentPos.x - state.startPos.x;
            double dz = currentPos.z - state.startPos.z;
            double horizontalDistSq = dx * dx + dz * dz;

            if (horizontalDistSq > MOVE_THRESHOLD_SQ) {
                ModNetworking.sendCloseTpAccept(player);
                player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.countdown.cancelled"));
                toRemove.add(playerId);
                CobbleSafari.LOGGER.debug("Dungeon teleport cancelled for player {} (moved)", player.getName().getString());
                continue;
            }

            ServerLevel portalLevel = server.getLevel(state.portalDimension);
            if (portalLevel == null) {
                toRemove.add(playerId);
                continue;
            }
            BlockEntity be = portalLevel.getBlockEntity(state.portalPos);
            if (!(be instanceof DungeonPortalBlockEntity portalEntity)) {
                ModNetworking.sendCloseTpAccept(player);
                player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.countdown.cancelled"));
                toRemove.add(playerId);
                continue;
            }

            if (!state.generationDone) {
                if (state.ticksRemaining <= PHASE_POSITION_CALCULATION && state.generationPhase == 0) {
                    if (!DungeonTeleportHandler.calculatePosition(player, portalEntity, state.validation)) {
                        ModNetworking.sendCloseTpAccept(player);
                        toRemove.add(playerId);
                        continue;
                    }
                    state.generationPhase = 1;
                    CobbleSafari.LOGGER.debug("Phase 1 (95-90s): Position calculated for player {}", player.getName().getString());
                }

                if (state.ticksRemaining <= PHASE_CHUNK_LOADING && state.generationPhase == 1) {
                    if (!DungeonTeleportHandler.loadChunks(player, portalEntity, state.validation)) {
                        ModNetworking.sendCloseTpAccept(player);
                        toRemove.add(playerId);
                        continue;
                    }
                    state.generationPhase = 2;
                    CobbleSafari.LOGGER.debug("Phase 2 (90-85s): Chunks loaded for player {}", player.getName().getString());
                }

                if (state.ticksRemaining <= PHASE_STRUCTURE_GENERATION && state.generationPhase == 2) {
                    if (!DungeonTeleportHandler.generateStructure(player, portalEntity, state.validation)) {
                        ModNetworking.sendCloseTpAccept(player);
                        toRemove.add(playerId);
                        continue;
                    }
                    state.generationPhase = 3;
                    CobbleSafari.LOGGER.debug("Phase 3 (85-80s): Structure generated for player {}", player.getName().getString());
                }

                if (state.ticksRemaining <= PHASE_BLOCK_REPLACEMENT_START && state.generationPhase == 3) {
                    if (!DungeonTeleportHandler.startFinalization(player, portalEntity, state.validation)) {
                        ModNetworking.sendCloseTpAccept(player);
                        toRemove.add(playerId);
                        continue;
                    }
                    state.generationPhase = 4;
                    CobbleSafari.LOGGER.debug("Phase 4.1 (80-60s): Exit portal placement started for player {}", player.getName().getString());
                }

                if (state.ticksRemaining <= PHASE_BLOCK_REPLACEMENT_STEP_2 && state.generationPhase == 4) {
                    state.generationPhase = 5;
                    CobbleSafari.LOGGER.debug("Phase 4.2 (60-40s): Block replacement step 2 for player {}", player.getName().getString());
                }

                if (state.ticksRemaining <= PHASE_BLOCK_REPLACEMENT_STEP_3 && state.generationPhase == 5) {
                    state.generationPhase = 6;
                    CobbleSafari.LOGGER.debug("Phase 4.3 (40-20s): Block replacement step 3 for player {}", player.getName().getString());
                }

                if (state.ticksRemaining <= PHASE_BLOCK_REPLACEMENT_STEP_4 && state.generationPhase == 6) {
                    DungeonTeleportHandler.DungeonPrepResult prepResult =
                            DungeonTeleportHandler.completeFinalization(player, portalEntity, state.validation);
                    if (prepResult == null) {
                        ModNetworking.sendCloseTpAccept(player);
                        toRemove.add(playerId);
                        continue;
                    }
                    state.prepResult = prepResult;
                    state.generationDone = true;
                    CobbleSafari.LOGGER.debug("Phase 4.4 (20-0s): Generation finalized for player {}", player.getName().getString());
                }
            }

            state.ticksRemaining--;

            if (state.ticksRemaining <= 0 && state.prepResult != null) {
                long portalRemaining = portalEntity.getRemainingLifetimeTicks();
                int recalculatedTicks;
                
                if (!state.validation.isReEntry()) {
                    int configuredTicks = SafariTimerConfig.getDimensionConfig(state.prepResult.dimensionId())
                            .map(DimensionTimerEntry::getTimerDurationTicks)
                            .orElse(SafariTimerConfig.getTimerDurationTicks());
                    if (portalRemaining >= 0 && portalRemaining < configuredTicks) {
                        recalculatedTicks = (int) portalRemaining;
                    } else {
                        recalculatedTicks = configuredTicks;
                    }
                } else {
                    int currentTimerTicks = state.prepResult.timerTicks();
                    if (portalRemaining >= 0 && portalRemaining < currentTimerTicks) {
                        recalculatedTicks = (int) portalRemaining;
                        CobbleSafari.LOGGER.info("Player {} re-entering dungeon (countdown): timer capped from {} to {} ticks (portal expiring)",
                                player.getName().getString(), currentTimerTicks, recalculatedTicks);
                    } else {
                        recalculatedTicks = currentTimerTicks;
                    }
                }
                
                state.prepResult = new DungeonTeleportHandler.DungeonPrepResult(
                        state.prepResult.dungeonLevel(),
                        state.prepResult.playerSpawnPos(),
                        state.prepResult.playerYaw(),
                        state.prepResult.dimensionId(),
                        state.prepResult.isReEntry(),
                        recalculatedTicks,
                        state.prepResult.playerOriginPos(),
                        state.prepResult.playerOriginDimension()
                );
                DungeonTeleportHandler.executeDungeonTeleport(player, portalEntity, state.prepResult);
                toRemove.add(playerId);
                CobbleSafari.LOGGER.info("Dungeon teleport completed for player {}", player.getName().getString());
            }
        }

        for (UUID id : toRemove) {
            pendingTeleports.remove(id);
        }
    }

    private static class TeleportState {
        final BlockPos portalPos;
        final ResourceKey<Level> portalDimension;
        final Vec3 startPos;
        int ticksRemaining;
        final DungeonTeleportHandler.DungeonValidationResult validation;
        DungeonTeleportHandler.DungeonPrepResult prepResult;
        boolean generationDone;
        int generationPhase;

        TeleportState(BlockPos portalPos, ResourceKey<Level> portalDimension, Vec3 startPos,
                      int ticksRemaining, DungeonTeleportHandler.DungeonValidationResult validation,
                      DungeonTeleportHandler.DungeonPrepResult prepResult, boolean generationDone) {
            this.portalPos = portalPos;
            this.portalDimension = portalDimension;
            this.startPos = startPos;
            this.ticksRemaining = ticksRemaining;
            this.validation = validation;
            this.prepResult = prepResult;
            this.generationDone = generationDone;
            this.generationPhase = 0;
        }
    }
}
