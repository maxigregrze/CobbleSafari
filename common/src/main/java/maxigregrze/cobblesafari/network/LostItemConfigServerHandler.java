package maxigregrze.cobblesafari.network;

import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import maxigregrze.cobblesafari.block.misc.LostItemBlock;
import maxigregrze.cobblesafari.block.misc.LostItemBlockEntity;
import maxigregrze.cobblesafari.block.misc.LostItemSkin;
import maxigregrze.cobblesafari.block.misc.LostItemStackText;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
    private static final String FIELD_LOOT_ITEM_COUNT = "gui.cobblesafari.lost_item.field.loot_item_count";
    private static final String TYPE_LOOT_ITEM_COUNT = "gui.cobblesafari.lost_item.type.loot_item_count";
    private static final String FIELD_POKEBALL = "gui.cobblesafari.lost_item.field.pokeball";
    private static final String TYPE_POKEBALL = "gui.cobblesafari.lost_item.type.pokeball";

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

        // Applied before the mode switch on purpose: every applyMode* branch can bail out on a
        // validation failure, and losing the skin because a loot table id was misspelled would be a trap.
        LostItemBlock.applySkin(player.level(), payload.pos(), LostItemSkin.byName(payload.skin()));
        applyPokeball(player, be, payload.pokeball().trim());

        int mode = payload.mode();
        if (mode < 0 || mode > 2) {
            mode = 0;
        }

        switch (mode) {
            case 0 -> applyMode0(serverLevel, player, be, payload);
            case 1 -> applyMode1(serverLevel, player, be, payload.lostItemLootTableId().trim());
            case 2 -> applyMode2(player, be, payload.lootItemId().trim(), payload.lootItemCountStr().trim(), payload.lootItemComponents());
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

    /** Purely visual, so applied before the mode switch like the skin (action plan 193, section 8.4). */
    private static void applyPokeball(ServerPlayer player, LostItemBlockEntity be, String raw) {
        ResourceLocation loc = ResourceLocation.tryParse(raw);
        if (loc == null || PokeBalls.getPokeBall(loc) == null) {
            sendInvalid(player, raw, FIELD_POKEBALL, TYPE_POKEBALL);
            return;
        }
        if (!raw.equals(be.getPokeballId())) {
            be.setPokeballId(loc.toString());
            be.syncConfigToClients();
        }
    }

    /**
     * Rebuilds the mode 2 stack from its three GUI fields; nothing is stored unless all three are valid.
     * The fields are the source of truth: a stack set by shift-click comes back identical when saved as is.
     */
    private static void applyMode2(ServerPlayer player, LostItemBlockEntity be, String rawId, String rawCount, String rawData) {
        if (rawId.isEmpty()) {
            sendInvalid(player, rawId, FIELD_LOOT_ITEM_MODE2, TYPE_ITEM);
            return;
        }
        ResourceLocation loc = ResourceLocation.tryParse(rawId);
        if (loc == null || !BuiltInRegistries.ITEM.containsKey(loc) || BuiltInRegistries.ITEM.get(loc) == Items.AIR) {
            sendInvalid(player, rawId, FIELD_LOOT_ITEM_MODE2, TYPE_ITEM);
            return;
        }
        int count;
        try {
            count = Integer.parseInt(rawCount);
        } catch (NumberFormatException ex) {
            sendInvalid(player, rawCount, FIELD_LOOT_ITEM_COUNT, TYPE_LOOT_ITEM_COUNT);
            return;
        }
        ItemStack stack;
        try {
            stack = LostItemStackText.parse(rawId, rawData, Math.max(1, count), player.registryAccess());
        } catch (CommandSyntaxException ex) {
            player.sendSystemMessage(Component.translatable("gui.cobblesafari.lost_item.loot_item_data_invalid",
                    ComponentUtils.fromMessage(ex.getRawMessage())));
            return;
        }
        // Bounded after parsing: a max_stack_size component in the data changes the limit.
        if (count < 1 || count > stack.getMaxStackSize()) {
            sendInvalid(player, rawCount, FIELD_LOOT_ITEM_COUNT, TYPE_LOOT_ITEM_COUNT);
            return;
        }
        be.setLootItemStack(stack);
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
