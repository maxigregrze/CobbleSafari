package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.block.csboss.CsBossTriggerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Validation serveur de la GUI du trigger (plan 100 § 3.3) : créatif + proximité,
 * jamais de confiance au client.
 */
public final class CsBossTriggerConfigServerHandler {

    private static final double MAX_DISTANCE = 8.0D;

    private CsBossTriggerConfigServerHandler() {}

    public static void handleSave(ServerPlayer player, SaveCsBossTriggerConfigPayload payload) {
        if (!player.isCreative()) {
            return;
        }
        if (!player.blockPosition().closerThan(payload.pos(), MAX_DISTANCE)) {
            return;
        }
        if (!(player.level().getBlockEntity(payload.pos()) instanceof CsBossTriggerBlockEntity be)) {
            return;
        }

        be.setBossRef(payload.bossRef().trim());

        String cost = payload.costItemId().trim();
        if (cost.isEmpty()) {
            be.setCostItemId("");
        } else {
            ResourceLocation loc = ResourceLocation.tryParse(cost);
            if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                be.setCostItemId(cost);
            } else {
                be.setCostItemId("");
                player.sendSystemMessage(Component.translatable("cobblesafari.csboss.bad_cost"));
            }
        }

        be.setPlayerRadiusOverride(parseIntOrDefault(payload.playerRadiusStr(), -1));
        be.setBlockRadiusOverride(parseIntOrDefault(payload.blockRadiusStr(), -1));

        be.syncToClients();
    }

    private static int parseIntOrDefault(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
