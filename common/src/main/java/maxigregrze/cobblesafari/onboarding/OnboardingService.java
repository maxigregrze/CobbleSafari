package maxigregrze.cobblesafari.onboarding;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.config.WonderTradeSettings;
import maxigregrze.cobblesafari.dungeon.PortalSpawnConfig;
import maxigregrze.cobblesafari.network.ObstacleConfigSyncPayload;
import maxigregrze.cobblesafari.network.OpenOnboardingPayload;
import maxigregrze.cobblesafari.network.SaveOnboardingPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The admin onboarding (action plan 198): the one-time login invitation, the screen's initial
 * state, and applying a save to the server configs.
 */
public final class OnboardingService {

    /** Invitation, command and save all require this, singleplayer included (decision 4.1). */
    public static final int PERMISSION_LEVEL = 2;

    private static final String MEGA_SHOWDOWN_MOD_ID = "mega_showdown";

    private OnboardingService() {}

    /**
     * Called once a login is complete (the deferred join queue, plan 190 J1). The permission test
     * comes first so an ordinary player never spends the invitation.
     */
    public static void onLoginComplete(ServerPlayer player) {
        if (!player.hasPermissions(PERMISSION_LEVEL) || !MiscConfig.consumeAskForOnboarding()) {
            return;
        }
        player.sendSystemMessage(Component.translatable("cobblesafari.onboarding.chat.first_time",
                OnboardingChat.runLink(Component.translatable("cobblesafari.onboarding.chat.first_time.link"))));
    }

    /** Opens the screen with the boxes pre-filled by the server (section 7.2). */
    public static void open(ServerPlayer player) {
        Services.PLATFORM.sendPayloadToPlayer(player, new OpenOnboardingPayload(initialChoices()));
    }

    /**
     * Saved values once the onboarding has been saved. Before that, Mega Showdown pre-ticks both its
     * boxes - written only if the admin clicks Save (decision 7.2).
     */
    private static OnboardingChoices initialChoices() {
        boolean saved = MiscConfig.hasSavedOnboarding();
        boolean megaShowdownPreset = !saved && Services.PLATFORM.isModLoaded(MEGA_SHOWDOWN_MOD_ID);
        MiscConfig.OnboardingState state = MiscConfig.getOnboardingState();
        return new OnboardingChoices(
                saved ? state.legendariesAndMythicals : megaShowdownPreset,
                state.ultraBeasts,
                state.paradox,
                state.missingRegular,
                PortalSpawnConfig.isEnabled(),
                PortalSpawnConfig.isNotificationEnabled(),
                MiscConfig.isBalloonEnabled(),
                megaShowdownPreset || MiscConfig.isShardsClustersEnabled());
    }

    public static void handleSave(ServerPlayer player, SaveOnboardingPayload payload) {
        // Re-checked here: a modified client can send the payload without running the command.
        if (!player.hasPermissions(PERMISSION_LEVEL)) {
            CobbleSafari.LOGGER.warn("[Onboarding] Save refused for {}: permission below {}",
                    player.getName().getString(), PERMISSION_LEVEL);
            return;
        }
        apply(player.getServer(), payload.choices());
        player.sendSystemMessage(OnboardingChat.rerunHint("cobblesafari.onboarding.chat.saved"));
        CobbleSafari.LOGGER.info("[Onboarding] Settings saved by {}: {}", player.getName().getString(), payload.choices());
    }

    private static void apply(MinecraftServer server, OnboardingChoices choices) {
        MiscConfig.OnboardingState state = new MiscConfig.OnboardingState(choices.legendariesAndMythicals(),
                choices.ultraBeasts(), choices.paradox(), choices.missingRegular());
        boolean shardsChanged = MiscConfig.isShardsClustersEnabled() != choices.megaShowdown();

        MiscConfig.setOnboardingState(state);
        MiscConfig.setBalloonEnabled(choices.rewardBalloons());
        MiscConfig.setShardsClustersEnabled(choices.megaShowdown());
        MiscConfig.save();

        PortalSpawnConfig.setEnabled(choices.portalSpawns());
        PortalSpawnConfig.setNotificationEnabled(choices.portalNotification());
        PortalSpawnConfig.save();

        WonderTradeSettings.get().applyOnboardingPools(state);
        WonderTradeSettings.save();

        // Clients keep a copy of the cluster switch for shapes and visibility (section 8.5).
        if (shardsChanged) {
            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                ObstacleConfigSyncPayload.syncToPlayer(online);
            }
        }
    }
}
