package maxigregrze.cobblesafari.objectives;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballGoldBlockEntity;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballGoldIndex;
import maxigregrze.cobblesafari.config.MiscConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Reward distribution for dimensional objectives: per-task loot, final completion loot, and the
 * Golden Auspicious Pokeball redeem scan.
 */
public final class ObjectivesRewardService {

    private ObjectivesRewardService() {}

    /** Gives the per-task reward. No-op if the table id is empty. */
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

    /** Gives the all-objectives-complete rewards, announced by a "TASKS COMPLETE" title (plan 196, § 7.7). */
    public static void giveFinalRewards(ServerPlayer player, DimensionalObjectivesDefinition def) {
        ObjectiveTitles.show(player, Component.translatable("cobblesafari.dimensional_objectives.tasks_complete"));
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

        // Query the live per-dimension index instead of scanning (2·radius+1)² chunks + all their block
        // entities: keep the "within N chunks of the player" semantics, resolve only the few candidates (B5).
        List<AuspiciousPokeballGoldBlockEntity> found = new ArrayList<>();
        for (BlockPos pos : AuspiciousPokeballGoldIndex.positionsIn(level.dimension().location())) {
            if (Math.abs((pos.getX() >> 4) - centerX) > radius || Math.abs((pos.getZ() >> 4) - centerZ) > radius) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof AuspiciousPokeballGoldBlockEntity gold) {
                found.add(gold);
            }
        }

        if (found.isEmpty()) {
            CobbleSafari.LOGGER.error(
                    "[Objectives] No Golden Auspicious Pokeball found within {} chunks of {} for player {} - giving fallback reward",
                    radius, player.blockPosition(), player.getGameProfile().getName());
            if (def.fallbackCompletionReward() != null) {
                rollAndGive(player, def.fallbackCompletionReward());
            }
            return;
        }

        AuspiciousPokeballGoldBlockEntity chosen = found.get(level.getRandom().nextInt(found.size()));
        chosen.addEarner(player.getGameProfile().getName());

        ObjectiveTitles.showNext(player, Component.translatable("cobblesafari.dimensional_objectives.bonus_ball_appeared")
                .withStyle(ChatFormatting.YELLOW));
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
