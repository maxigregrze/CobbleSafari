package maxigregrze.cobblesafari.network;

import com.mojang.logging.LogUtils;
import maxigregrze.cobblesafari.block.misc.LostItemBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.function.BiConsumer;
import org.slf4j.Logger;

public final class LostItemConfigServerHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String FIELD_LOOT_TABLE_MODE1 = "gui.cobblesafari.lost_item.field.lost_item_loot_table";
    private static final String FIELD_LOOT_ITEM_MODE2 = "gui.cobblesafari.lost_item.field.loot_item";
    private static final String TYPE_LOOT_TABLE = "gui.cobblesafari.lost_item.type.loot_table";
    private static final String TYPE_ITEM = "gui.cobblesafari.lost_item.type.item";

    private LostItemConfigServerHandler() {
    }

    public static void handleReset(ServerPlayer player, LostItemResetClaimsPayload payload) {
        if (!player.isCreative()) {
            return;
        }
        if (!player.blockPosition().closerThan(payload.pos(), 8.0D)) {
            return;
        }
        if (!(player.level().getBlockEntity(payload.pos()) instanceof LostItemBlockEntity be)) {
            return;
        }
        be.resetClaims();
    }

    public static void handleSave(ServerPlayer player, SaveLostItemConfigPayload payload) {
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
        if (!(raw instanceof LostItemBlockEntity be)) {
            return;
        }

        int mode = payload.mode();
        if (mode < 0 || mode > 2) {
            mode = 0;
        }

        switch (mode) {
            case 0 -> applyMode0(serverLevel, player, be, payload);
            case 1 -> applyMode1(serverLevel, player, be, payload.lostItemLootTableId().trim());
            case 2 -> applyMode2(player, be, payload.lootItemId().trim());
            default -> applyMode0(serverLevel, player, be, payload);
        }
    }

    private static void applyMode0(ServerLevel level, ServerPlayer player, LostItemBlockEntity be, SaveLostItemConfigPayload payload) {
        tryApplyLootPool(level, player, be, payload.poolBerryId().trim(), LostItemBlockEntity::setPoolBerryId, "gui.cobblesafari.lost_item.field.pool_berry");
        tryApplyLootPool(level, player, be, payload.poolCandyId().trim(), LostItemBlockEntity::setPoolCandyId, "gui.cobblesafari.lost_item.field.pool_candy");
        tryApplyLootPool(level, player, be, payload.poolBallsId().trim(), LostItemBlockEntity::setPoolBallsId, "gui.cobblesafari.lost_item.field.pool_balls");
        tryApplyLootPool(level, player, be, payload.poolTreasuresId().trim(), LostItemBlockEntity::setPoolTreasuresId, "gui.cobblesafari.lost_item.field.pool_treasures");

        boolean minOk = tryParseRoll(player, payload.minRollStr().trim(), "gui.cobblesafari.lost_item.field.min_roll", v -> be.setMinRoll(v));
        boolean maxOk = tryParseRoll(player, payload.maxRollStr().trim(), "gui.cobblesafari.lost_item.field.max_roll", v -> be.setMaxRoll(v));
        if (minOk || maxOk) {
            be.normalizeRollBounds();
        }

        be.setMode(0);
        be.syncConfigToClients();
    }

    private static void applyMode1(ServerLevel level, ServerPlayer player, LostItemBlockEntity be, String raw) {
        if (raw.isEmpty()) {
            sendInvalid(player, raw, FIELD_LOOT_TABLE_MODE1, TYPE_LOOT_TABLE);
            return;
        }
        ResourceLocation loc = ResourceLocation.tryParse(raw);
        if (loc == null) {
            sendInvalid(player, raw, FIELD_LOOT_TABLE_MODE1, TYPE_LOOT_TABLE);
            return;
        }
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, loc);
        if (!lootTableExists(level, key)) {
            sendInvalid(player, raw, FIELD_LOOT_TABLE_MODE1, TYPE_LOOT_TABLE);
            return;
        }
        be.setLostItemLootTableId(raw);
        be.setMode(1);
        be.syncConfigToClients();
    }

    private static void applyMode2(ServerPlayer player, LostItemBlockEntity be, String raw) {
        if (raw.isEmpty()) {
            sendInvalid(player, raw, FIELD_LOOT_ITEM_MODE2, TYPE_ITEM);
            return;
        }
        ResourceLocation loc = ResourceLocation.tryParse(raw);
        if (loc == null || !BuiltInRegistries.ITEM.containsKey(loc)) {
            sendInvalid(player, raw, FIELD_LOOT_ITEM_MODE2, TYPE_ITEM);
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(loc);
        if (item == Items.AIR) {
            sendInvalid(player, raw, FIELD_LOOT_ITEM_MODE2, TYPE_ITEM);
            return;
        }
        be.setLootItemId(raw);
        be.setMode(2);
        be.syncConfigToClients();
    }

    private static void tryApplyLootPool(
            ServerLevel level,
            ServerPlayer player,
            LostItemBlockEntity be,
            String raw,
            BiConsumer<LostItemBlockEntity, String> setter,
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
            sendInvalid(player, raw, fieldKey, "gui.cobblesafari.lost_item.type.number");
            return false;
        }
        try {
            int v = Integer.parseInt(raw);
            apply.accept(v);
            return true;
        } catch (NumberFormatException ex) {
            sendInvalid(player, raw, fieldKey, "gui.cobblesafari.lost_item.type.number");
            return false;
        }
    }

    private static boolean lootTableExists(ServerLevel level, ResourceKey<LootTable> key) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        return table != LootTable.EMPTY;
    }

    private static void sendInvalid(ServerPlayer player, String value, String fieldKey, String typeKey) {
        player.sendSystemMessage(Component.translatable(
                "gui.cobblesafari.lost_item.save_failed",
                value,
                Component.translatable(fieldKey),
                Component.translatable(typeKey)
        ));
    }

    public static void logPoolRollFailure(String reason, String poolId) {
        LOGGER.error("[LostItem] Cannot roll loot pool ({}): {}", reason, poolId);
    }
}
