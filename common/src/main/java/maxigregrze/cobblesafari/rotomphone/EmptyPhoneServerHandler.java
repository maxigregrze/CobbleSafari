package maxigregrze.cobblesafari.rotomphone;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.RotomPhoneConfig;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.item.RotomPhoneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EmptyPhoneServerHandler {

    private EmptyPhoneServerHandler() {}

    /** Which filled device a pending confirmation should produce (and which empty item it consumes). */
    public enum FillTarget { PHONE, EARPIECE }

    private static final Map<UUID, PendingFill> PENDING_FILLS = new ConcurrentHashMap<>();

    public static void attemptFill(ServerPlayer player, boolean isFromBlock, BlockPos blockPos,
                                   int inventorySlot, FillTarget target) {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Pokemon firstRotom = findFirstRotom(party);
        if (firstRotom == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("cobblesafari.rotomphone.no_rotom"), true);
            return;
        }
        String rotomName = firstRotom.getSpecies().getName();
        int rotomLevel = firstRotom.getLevel();
        boolean rotomShiny = firstRotom.getShiny();

        PENDING_FILLS.put(player.getUUID(), new PendingFill(firstRotom, isFromBlock, blockPos, inventorySlot, target));

        maxigregrze.cobblesafari.platform.Services.PLATFORM.sendPayloadToPlayer(player,
                new maxigregrze.cobblesafari.network.OpenEmptyPhoneConfirmPayload(
                        rotomName, rotomLevel, rotomShiny, isFromBlock));
    }

    public static void handleConfirm(ServerPlayer player, boolean confirmed) {
        PendingFill pending = PENDING_FILLS.remove(player.getUUID());
        if (pending == null) return;
        if (!confirmed) return;

        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Pokemon rotom = pending.rotom();
        if (rotom == null || !party.toGappyList().contains(rotom)) return;

        if (!RotomPhoneConfig.isAllowsShinyPhone() && rotom.getShiny()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "cobblesafari.rotomphone.shiny_not_allowed"), false);
            CobbleSafari.LOGGER.info("{} tried to use a shiny rotom in a rotom phone, but it is not allowed",
                    player.getName().getString());
            return;
        }

        boolean shiny = rotom.getShiny();
        party.remove(rotom);

        maxigregrze.cobblesafari.advancement.ModCriteria.ROTOM_PHONE_MADE.trigger(player);
        if (shiny) {
            maxigregrze.cobblesafari.advancement.ModCriteria.ROTOM_PHONE_SHINY.trigger(player);
        }

        boolean earpiece = pending.target() == FillTarget.EARPIECE;
        ItemStack resultStack = new ItemStack(earpiece ? ModItems.ROTOM_EARPIECE : ModItems.ROTOM_PHONE);
        RotomPhoneItem.setRotomName(resultStack, rotom.getSpecies().getName());
        RotomPhoneItem.setShiny(resultStack, rotom.getShiny());
        RotomPhoneItem.setCurrentSkin(resultStack, "");
        RotomPhoneItem.setSafetyMode(resultStack, false);

        if (pending.isFromBlock()) {
            BlockPos pos = pending.blockPos();
            if (pos != null && player.level().getBlockState(pos).is(maxigregrze.cobblesafari.init.ModBlocks.EMPTYPHONE)) {
                player.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                    player.level(), player.getX(), player.getY() + 0.5, player.getZ(), resultStack);
            player.level().addFreshEntity(itemEntity);
        } else {
            int slot = pending.inventorySlot();
            ItemStack inSlot = player.getInventory().getItem(slot);
            net.minecraft.world.item.Item expectedEmpty = earpiece
                    ? ModItems.EMPTY_EARPIECE
                    : maxigregrze.cobblesafari.init.ModBlocks.EMPTYPHONE.asItem();
            if (inSlot.is(expectedEmpty)) {
                inSlot.shrink(1);
            }
            if (!player.getInventory().add(resultStack)) {
                player.drop(resultStack, false);
            }
        }

        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "cobblesafari.rotomphone.filled"), false);
    }

    private static Pokemon findFirstRotom(PlayerPartyStore party) {
        for (int i = 0; i < 6; i++) {
            Pokemon pokemon = party.get(i);
            if (pokemon != null && "rotom".equalsIgnoreCase(pokemon.getSpecies().getName())) {
                return pokemon;
            }
        }
        return null;
    }

    private record PendingFill(Pokemon rotom, boolean isFromBlock, BlockPos blockPos, int inventorySlot, FillTarget target) {}
}
