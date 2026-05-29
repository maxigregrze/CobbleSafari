package maxigregrze.cobblesafari.network;

import com.mojang.logging.LogUtils;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;

import java.util.function.BiConsumer;

public final class AuspiciousPokeballConfigServerHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TYPE_LOOT_TABLE = "gui.cobblesafari.auspicious_pokeball.type.loot_table";
    private static final String TYPE_NUMBER = "gui.cobblesafari.auspicious_pokeball.type.number";

    private AuspiciousPokeballConfigServerHandler() {
    }

    public static void handleReset(ServerPlayer player, AuspiciousPokeballResetClaimsPayload payload) {
        if (!player.isCreative()) {
            return;
        }
        if (!player.blockPosition().closerThan(payload.pos(), 8.0D)) {
            return;
        }
        if (!(player.level().getBlockEntity(payload.pos()) instanceof AuspiciousPokeballBlockEntity be)) {
            return;
        }
        be.resetClaims();
    }

    public static void handleSave(ServerPlayer player, SaveAuspiciousPokeballConfigPayload payload) {
        if (!player.isCreative()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!player.blockPosition().closerThan(payload.pos(), 8.0D)) {
            return;
        }
        BlockEntity raw = player.level().getBlockEntity(payload.pos());
        if (!(raw instanceof AuspiciousPokeballBlockEntity be)) {
            return;
        }

        tryApplyLootPool(serverLevel, player, be, payload.poolBerryId().trim(), AuspiciousPokeballBlockEntity::setPoolBerryId, "gui.cobblesafari.auspicious_pokeball.field.pool_berry");
        tryApplyLootPool(serverLevel, player, be, payload.poolCandyId().trim(), AuspiciousPokeballBlockEntity::setPoolCandyId, "gui.cobblesafari.auspicious_pokeball.field.pool_candy");
        tryApplyLootPool(serverLevel, player, be, payload.poolBallsId().trim(), AuspiciousPokeballBlockEntity::setPoolBallsId, "gui.cobblesafari.auspicious_pokeball.field.pool_balls");
        tryApplyLootPool(serverLevel, player, be, payload.poolTreasuresId().trim(), AuspiciousPokeballBlockEntity::setPoolTreasuresId, "gui.cobblesafari.auspicious_pokeball.field.pool_treasures");

        boolean minOk = tryParseRoll(player, payload.minRollStr().trim(), "gui.cobblesafari.auspicious_pokeball.field.min_roll", be::setMinRoll);
        boolean maxOk = tryParseRoll(player, payload.maxRollStr().trim(), "gui.cobblesafari.auspicious_pokeball.field.max_roll", be::setMaxRoll);
        if (minOk || maxOk) {
            be.normalizeRollBounds();
        }

        be.syncConfigToClients();
    }

    private static void tryApplyLootPool(
            ServerLevel level,
            ServerPlayer player,
            AuspiciousPokeballBlockEntity be,
            String raw,
            BiConsumer<AuspiciousPokeballBlockEntity, String> setter,
            String fieldKey
    ) {
        if (raw.isEmpty()) {
            sendInvalid(player, raw, fieldKey, TYPE_LOOT_TABLE);
            return;
        }
        ResourceLocation loc = ResourceLocation.tryParse(raw);
        if (loc == null) {
            sendInvalid(player, raw, fieldKey, TYPE_LOOT_TABLE);
            return;
        }
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, loc);
        if (!lootTableExists(level, key)) {
            sendInvalid(player, raw, fieldKey, TYPE_LOOT_TABLE);
            return;
        }
        setter.accept(be, raw);
    }

    private static boolean tryParseRoll(ServerPlayer player, String raw, String fieldKey, java.util.function.IntConsumer apply) {
        if (raw.isEmpty()) {
            sendInvalid(player, raw, fieldKey, TYPE_NUMBER);
            return false;
        }
        try {
            int v = Integer.parseInt(raw);
            apply.accept(v);
            return true;
        } catch (NumberFormatException ex) {
            sendInvalid(player, raw, fieldKey, TYPE_NUMBER);
            return false;
        }
    }

    private static boolean lootTableExists(ServerLevel level, ResourceKey<LootTable> key) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        return table != LootTable.EMPTY;
    }

    private static void sendInvalid(ServerPlayer player, String value, String fieldKey, String typeKey) {
        player.sendSystemMessage(Component.translatable(
                "gui.cobblesafari.auspicious_pokeball.save_failed",
                value,
                Component.translatable(fieldKey),
                Component.translatable(typeKey)
        ));
    }

    public static void logPoolRollFailure(String reason, String poolId) {
        LOGGER.error("[AuspiciousPokeball] Cannot roll loot pool ({}): {}", reason, poolId);
    }
}
