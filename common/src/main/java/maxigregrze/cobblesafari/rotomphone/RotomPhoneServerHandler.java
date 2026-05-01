package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.item.RotomPhoneItem;
import maxigregrze.cobblesafari.network.OpenRotomPhonePayload;
import maxigregrze.cobblesafari.network.RotomPhoneActionPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class RotomPhoneServerHandler {

    private RotomPhoneServerHandler() {}

    public static void openPhone(ServerPlayer player, ItemStack phoneStack) {
        RotomPhoneConfigSync.syncToPlayer(player);
        String name = RotomPhoneItem.getRotomName(phoneStack);
        boolean shiny = RotomPhoneItem.isShiny(phoneStack);
        String skin = RotomPhoneItem.getCurrentSkin(phoneStack);
        boolean safety = RotomPhoneItem.isSafetyMode(phoneStack);
        boolean rotoGlide = RotomPhoneItem.isRotoGlideEnabled(phoneStack);
        Services.PLATFORM.sendPayloadToPlayer(player,
                new OpenRotomPhonePayload(name, shiny, skin, safety, rotoGlide));
    }

    public static void handleAction(ServerPlayer player, RotomPhoneActionPayload payload) {
        ItemStack phoneStack = findPhoneInInventory(player);
        if (phoneStack.isEmpty()) return;

        switch (payload.actionType()) {
            case RotomPhoneActionPayload.ACTION_CHANGE_SKIN -> {
                String skinId = payload.data();
                if (skinId == null || skinId.isEmpty()) {
                    RotomPhoneItem.setCurrentSkin(phoneStack, "");
                } else {
                    RotomPhoneSkinDefinition skin = RotomPhoneSkinRegistry.getSkin(skinId);
                    if (skin != null && RotomPhoneSkinRegistry.isUnlockedByPlayer(player, skin)) {
                        RotomPhoneItem.setCurrentSkin(phoneStack, skinId);
                    }
                }
            }
            case RotomPhoneActionPayload.ACTION_TOGGLE_SAFETY -> {
                boolean current = RotomPhoneItem.isSafetyMode(phoneStack);
                RotomPhoneItem.setSafetyMode(phoneStack, !current);
            }
            case RotomPhoneActionPayload.ACTION_TOGGLE_ROTO_GLIDE -> {
                boolean current = RotomPhoneItem.isRotoGlideEnabled(phoneStack);
                RotomPhoneItem.setRotoGlideEnabled(phoneStack, !current);
            }
            case RotomPhoneActionPayload.ACTION_OPEN_PC -> {
                try {
                    var pcStore = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getPC(player);
                    com.cobblemon.mod.common.net.messages.client.storage.pc.OpenPCPacket openPCPacket =
                            new com.cobblemon.mod.common.net.messages.client.storage.pc.OpenPCPacket(pcStore.getUuid());
                    openPCPacket.sendToPlayer(player);
                } catch (Exception e) {
                    CobbleSafari.LOGGER.error("Failed to open PC for {}", player.getName().getString(), e);
                }
            }
            case RotomPhoneActionPayload.ACTION_CLOSE -> {
                // no-op
            }
            default -> CobbleSafari.LOGGER.warn("Unknown rotom phone action: {}", payload.actionType());
        }
    }

    private static ItemStack findPhoneInInventory(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack inHand = player.getItemInHand(hand);
            if (inHand.is(ModItems.ROTOM_PHONE)) {
                return inHand;
            }
        }
        if (Services.PLATFORM.isModLoaded("accessories")) {
            ItemStack accessoryPhone = maxigregrze.cobblesafari.compat.accessories.AccessoriesCompat.getPhoneFromSlot(player);
            if (!accessoryPhone.isEmpty()) return accessoryPhone;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.ROTOM_PHONE)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
