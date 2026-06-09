package maxigregrze.cobblesafari.objectives;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballGoldBlockEntity;
import maxigregrze.cobblesafari.config.MiscConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Reward distribution for dimensional objectives: per-task loot, final completion loot, and the
 * Golden Auspicious Pokeball redeem scan (plan 118 §8).
 */
public final class ObjectivesRewardService {

    private ObjectivesRewardService() {}

    /** Gives the per-task reward (plan 118 §8.1). No-op if the table id is empty. */
    public static void giveTaskReward(ServerPlayer player, ObjectiveTask task) {
        if (task.isTaskRewardGiven() || task.taskRewardTable().isEmpty()) {
            return;
        }
        ResourceLocation table = ResourceLocation.tryParse(task.taskRewardTable());
        if (table != null) {
            rollAndGive(player, table);
        }
        task.markTaskRewardGiven();
    }

    /** Gives the all-objectives-complete rewards (plan 118 §8.2). */
    public static void giveFinalRewards(ServerPlayer player, DimensionalObjectivesDefinition def) {
        if (def.enableFinalCompletionReward() && def.finalCompletionReward() != null) {
            rollAndGive(player, def.finalCompletionReward());
        }
        if (def.enableFinalCompletionAuspiciousReward()) {
            scanAndRedeem(player, def);
        }
    }

    private static void scanAndRedeem(ServerPlayer player, DimensionalObjectivesDefinition def) {
        ServerLevel level = player.serverLevel();
        int radius = MiscConfig.getDimensionalObjectivesGoldScanRadius();
        int centerX = player.chunkPosition().x;
        int centerZ = player.chunkPosition().z;

        List<AuspiciousPokeballGoldBlockEntity> found = new ArrayList<>();
        for (int cx = centerX - radius; cx <= centerX + radius; cx++) {
            for (int cz = centerZ - radius; cz <= centerZ + radius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof AuspiciousPokeballGoldBlockEntity gold) {
                        found.add(gold);
                    }
                }
            }
        }

        if (found.isEmpty()) {
            CobbleSafari.LOGGER.error(
                    "[Objectives] No Golden Auspicious Pokeball found within {} chunks of {} for player {} — giving fallback reward",
                    radius, player.blockPosition(), player.getGameProfile().getName());
            if (def.fallbackCompletionReward() != null) {
                rollAndGive(player, def.fallbackCompletionReward());
            }
            return;
        }

        AuspiciousPokeballGoldBlockEntity chosen = found.get(level.getRandom().nextInt(found.size()));
        chosen.addEarner(player.getGameProfile().getName());

        Component title = Component.translatable(
                "cobblesafari.dimensional_objectives.auspicious_pokeball_appeared",
                Component.translatable(def.auspiciousPokeballRewardDisplayName()));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private static void rollAndGive(ServerPlayer player, ResourceLocation tableId) {
        ServerLevel level = player.serverLevel();
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, tableId);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        if (table == LootTable.EMPTY) {
            CobbleSafari.LOGGER.error("[Objectives] loot table not found: {}", tableId);
            return;
        }
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .create(LootContextParamSets.CHEST);
        for (ItemStack stack : table.getRandomItems(params)) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!player.getInventory().add(stack) || !stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }
}
