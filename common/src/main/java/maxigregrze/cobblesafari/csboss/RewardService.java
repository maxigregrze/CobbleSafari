package maxigregrze.cobblesafari.csboss;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.CsBossSettings;
import net.minecraft.core.registries.Registries;
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
 * Reward distribution on victory (plan 100 § 16).
 */
public final class RewardService {

    private RewardService() {}

    public static void grant(ServerLevel level, BossBattleSession session) {
        CsBossSettings cfg = CsBossSettings.get();
        int yTol = cfg.getArenaYTolerance();
        CsBossDefinition def = session.getDefinition();

        // Living survivors AND still in radius at win time.
        List<ServerPlayer> winners = new ArrayList<>();
        for (ServerPlayer p : session.aliveParticipants(level)) {
            if (session.withinArena(p.position(), yTol)) {
                winners.add(p);
            }
        }
        if (winners.isEmpty()) {
            return;
        }

        // Unique reward (before common to limit ground drops).
        if (def.uniqueReward() != null) {
            List<ItemStack> unique = roll(level, session, def.uniqueReward());
            if (!unique.isEmpty()) {
                if (cfg.isUniqueLootCommunism()) {
                    for (ServerPlayer p : winners) {
                        give(level, session, p, copy(unique));
                    }
                } else {
                    ServerPlayer chosen = winners.get(level.getRandom().nextInt(winners.size()));
                    give(level, session, chosen, copy(unique));
                }
            }
        }

        // Common reward: SAME roll for everyone.
        List<ItemStack> common = roll(level, session, def.rewards());
        for (ServerPlayer p : winners) {
            give(level, session, p, copy(common));
        }
    }

    private static List<ItemStack> roll(ServerLevel level, BossBattleSession session, ResourceLocation tableId) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, tableId);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        if (table == LootTable.EMPTY) {
            CobbleSafari.LOGGER.error("[CSBoss] loot table not found: {}", tableId);
            return List.of();
        }
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, session.getArenaCenter())
                .create(LootContextParamSets.CHEST);
        return table.getRandomItems(params);
    }

    private static List<ItemStack> copy(List<ItemStack> src) {
        List<ItemStack> out = new ArrayList<>(src.size());
        for (ItemStack s : src) {
            out.add(s.copy());
        }
        return out;
    }

    private static void give(ServerLevel level, BossBattleSession session, ServerPlayer player, List<ItemStack> items) {
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            boolean added = player.getInventory().add(stack);
            if (!added || !stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }
}
