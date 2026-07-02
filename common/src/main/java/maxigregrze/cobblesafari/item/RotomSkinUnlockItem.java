package maxigregrze.cobblesafari.item;

import maxigregrze.cobblesafari.data.RotomPhoneUnlockSavedData;
import maxigregrze.cobblesafari.init.ModComponents;
import maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneClientCache;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneConfigSync;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneSkinDefinition;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneSkinRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumable that unlocks Rotom Phone skin(s) for the player.
 *
 * <p>Two flavours, chosen at registration:
 * <ul>
 *   <li><b>dynamic</b> ({@code targetSkin == null}) — a single registered item whose target skin id is
 *       carried per-stack in the {@link ModComponents#SKIN_UNLOCK_TARGET} component. This sidesteps the
 *       registry freeze entirely, so it works for any skin including datapack-added ones. The disc only
 *       grants skins whose definition has {@code addUnlockItem == true}.</li>
 *   <li><b>all</b> ({@code targetSkin ==} {@link #ALL}) — unlocks every registered skin at once.</li>
 * </ul>
 */
public class RotomSkinUnlockItem extends Item {

    public static final String ALL = "*";

    /** {@code null} = dynamic (read the component); {@link #ALL} = unlock everything. */
    private final String targetSkin;

    public RotomSkinUnlockItem(Properties properties, String targetSkin) {
        super(properties);
        this.targetSkin = targetSkin;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        RotomPhoneUnlockSavedData store = RotomPhoneUnlockSavedData.get(serverPlayer.server);
        if (store == null) {
            return InteractionResultHolder.fail(stack);
        }

        boolean any = false;
        for (String skinId : resolveTargets(stack)) {
            any |= store.unlockSkin(serverPlayer.getUUID(), skinId);
        }

        if (!any) {
            serverPlayer.displayClientMessage(
                    Component.translatable("item.cobblesafari.rotom_skin_unlock.already"), true);
            return InteractionResultHolder.fail(stack);
        }

        RotomPhoneConfigSync.syncToPlayer(serverPlayer);
        serverPlayer.displayClientMessage(
                Component.translatable("item.cobblesafari.rotom_skin_unlock.success"), true);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** Server-side resolution of which skin id(s) this stack grants. */
    private List<String> resolveTargets(ItemStack stack) {
        if (ALL.equals(targetSkin)) {
            List<String> out = new ArrayList<>();
            for (RotomPhoneSkinDefinition skin : RotomPhoneSkinRegistry.getAllSkins()) {
                out.add(skin.getId());
            }
            return out;
        }
        String id = stack.get(ModComponents.SKIN_UNLOCK_TARGET);
        if (id == null || id.isEmpty()) {
            return List.of();
        }
        RotomPhoneSkinDefinition skin = RotomPhoneSkinRegistry.getSkin(id);
        // The dynamic disc only grants skins explicitly flagged as disc-obtainable.
        if (skin == null || !skin.addUnlockItem()) {
            return List.of();
        }
        return List.of(id);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (ALL.equals(targetSkin)) {
            return super.getName(stack);
        }
        String id = stack.get(ModComponents.SKIN_UNLOCK_TARGET);
        if (id == null || id.isEmpty()) {
            return super.getName(stack);
        }
        return Component.translatable("item.cobblesafari.rotom_skin_unlock.targeted", displayNameFor(id));
    }

    /** Client-facing skin display name from the synced cache, falling back to the raw id. */
    private static String displayNameFor(String skinId) {
        for (RotomPhoneConfigSyncPayload.SkinData sd : RotomPhoneClientCache.getCachedSkins()) {
            if (sd.id().equals(skinId)) {
                return sd.displayName();
            }
        }
        return skinId;
    }

    /**
     * Item tint for the dynamic disc. Two layers: layer 0 is the untinted base, layer 1 is an overlay
     * recolored to the target skin's hex color. Any other index is left untinted. {@code -1} = no tint.
     */
    public static int computeTint(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) {
            return -1;
        }
        String id = stack.get(ModComponents.SKIN_UNLOCK_TARGET);
        if (id == null || id.isEmpty()) {
            return -1;
        }
        for (RotomPhoneConfigSyncPayload.SkinData sd : RotomPhoneClientCache.getCachedSkins()) {
            if (sd.id().equals(id)) {
                try {
                    return 0xFF000000 | Integer.parseInt(sd.color(), 16);
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }
}
