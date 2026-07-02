package maxigregrze.cobblesafari.item;

import maxigregrze.cobblesafari.config.RotomPhoneConfig;
import maxigregrze.cobblesafari.data.RotomPhoneUnlockSavedData;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneConfigSync;
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
 * Consumable that unlocks a Rotom Phone app for the player. The target is set at registration:
 * a specific app id, or {@link #ALL} to unlock every app that is not enabled by default.
 */
public class RotomAppUnlockItem extends Item {

    public static final String ALL = "*";

    private final String targetApp;

    public RotomAppUnlockItem(Properties properties, String targetApp) {
        super(properties);
        this.targetApp = targetApp;
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
        for (String appId : resolveTargets()) {
            if (RotomPhoneConfig.getPhoneApps().containsKey(appId)) {
                any |= store.unlockApp(serverPlayer.getUUID(), appId);
            }
        }

        if (!any) {
            serverPlayer.displayClientMessage(
                    Component.translatable("item.cobblesafari.rotom_app_unlock.already"), true);
            return InteractionResultHolder.fail(stack);
        }

        RotomPhoneConfigSync.syncToPlayer(serverPlayer);
        serverPlayer.displayClientMessage(
                Component.translatable("item.cobblesafari.rotom_app_unlock.success"), true);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private List<String> resolveTargets() {
        if (!ALL.equals(targetApp)) {
            return List.of(targetApp);
        }
        List<String> out = new ArrayList<>();
        RotomPhoneConfig.getPhoneApps().forEach((id, cfg) -> {
            if (!cfg.isEnabledByDefault()) {
                out.add(id);
            }
        });
        return out;
    }
}
