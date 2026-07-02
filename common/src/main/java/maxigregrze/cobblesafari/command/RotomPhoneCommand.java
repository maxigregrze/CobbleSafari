package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import maxigregrze.cobblesafari.config.RotomPhoneConfig;
import maxigregrze.cobblesafari.data.RotomPhoneUnlockSavedData;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneConfigSync;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneSkinDefinition;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneSkinRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class RotomPhoneCommand {

    private static final String ARG_USER = "user";
    private static final String ARG_APP = "appId";
    private static final String ARG_SKIN = "skinId";

    private static final SuggestionProvider<CommandSourceStack> APP_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(RotomPhoneConfig.getPhoneApps().keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> SKIN_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    RotomPhoneSkinRegistry.getAllSkins().stream().map(RotomPhoneSkinDefinition::getId).toList(), builder);

    private RotomPhoneCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("rotomphone")
                .then(Commands.literal("app")
                        .then(Commands.literal("unlock")
                                .then(Commands.argument(ARG_USER, EntityArgument.player())
                                        .then(Commands.argument(ARG_APP, StringArgumentType.word())
                                                .suggests(APP_SUGGESTIONS)
                                                .executes(RotomPhoneCommand::appUnlock))))
                        .then(Commands.literal("lock")
                                .then(Commands.argument(ARG_USER, EntityArgument.player())
                                        .then(Commands.argument(ARG_APP, StringArgumentType.word())
                                                .suggests(APP_SUGGESTIONS)
                                                .executes(RotomPhoneCommand::appLock))))
                        .then(Commands.literal("unlockall")
                                .then(Commands.argument(ARG_USER, EntityArgument.player())
                                        .executes(RotomPhoneCommand::appUnlockAll)))
                        .then(Commands.literal("list")
                                .then(Commands.argument(ARG_USER, EntityArgument.player())
                                        .executes(RotomPhoneCommand::appList))))
                .then(Commands.literal("skin")
                        .then(Commands.literal("list")
                                .then(Commands.argument(ARG_USER, EntityArgument.player())
                                        .executes(RotomPhoneCommand::skinList)))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument(ARG_USER, EntityArgument.player())
                                        .then(Commands.argument(ARG_SKIN, StringArgumentType.word())
                                                .suggests(SKIN_SUGGESTIONS)
                                                .executes(RotomPhoneCommand::skinUnlock))))
                        .then(Commands.literal("lock")
                                .then(Commands.argument(ARG_USER, EntityArgument.player())
                                        .then(Commands.argument(ARG_SKIN, StringArgumentType.word())
                                                .suggests(SKIN_SUGGESTIONS)
                                                .executes(RotomPhoneCommand::skinLock)))));
    }

    // ---------------------------------------------------------------- apps

    private static int appUnlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_USER);
        String appId = StringArgumentType.getString(ctx, ARG_APP);
        if (!RotomPhoneConfig.getPhoneApps().containsKey(appId)) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.rotomphone.app.unknown", appId));
            return 0;
        }
        RotomPhoneUnlockSavedData.get(target.server).unlockApp(target.getUUID(), appId);
        RotomPhoneConfigSync.syncToPlayer(target);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.rotomphone.app.unlocked", appId, target.getName()), true);
        return 1;
    }

    private static int appLock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_USER);
        String appId = StringArgumentType.getString(ctx, ARG_APP);
        RotomPhoneUnlockSavedData.get(target.server).lockApp(target.getUUID(), appId);
        RotomPhoneConfigSync.syncToPlayer(target);
        Component note = RotomPhoneConfig.getAppConfig(appId).isEnabledByDefault()
                ? Component.translatable("cobblesafari.command.rotomphone.app.locked_default", appId, target.getName())
                : Component.translatable("cobblesafari.command.rotomphone.app.locked", appId, target.getName());
        ctx.getSource().sendSuccess(() -> note, true);
        return 1;
    }

    private static int appUnlockAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_USER);
        RotomPhoneUnlockSavedData store = RotomPhoneUnlockSavedData.get(target.server);
        int count = 0;
        for (Map.Entry<String, RotomPhoneConfig.PhoneAppConfig> e : RotomPhoneConfig.getPhoneApps().entrySet()) {
            if (!e.getValue().isEnabledByDefault() && store.unlockApp(target.getUUID(), e.getKey())) {
                count++;
            }
        }
        RotomPhoneConfigSync.syncToPlayer(target);
        final int unlocked = count;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.rotomphone.app.unlockall", unlocked, target.getName()), true);
        return 1;
    }

    private static int appList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_USER);
        RotomPhoneUnlockSavedData store = RotomPhoneUnlockSavedData.get(target.server);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.rotomphone.app.list_header", target.getName()), false);
        for (Map.Entry<String, RotomPhoneConfig.PhoneAppConfig> e : RotomPhoneConfig.getPhoneApps().entrySet()) {
            String origin;
            ChatFormatting color;
            if (e.getValue().isEnabledByDefault()) {
                origin = "default";
                color = ChatFormatting.GREEN;
            } else if (store.isAppUnlocked(target.getUUID(), e.getKey())) {
                origin = "unlocked";
                color = ChatFormatting.AQUA;
            } else {
                origin = "locked";
                color = ChatFormatting.GRAY;
            }
            final String id = e.getKey();
            ctx.getSource().sendSuccess(() -> Component.literal(" - " + id + ": " + origin).withStyle(color), false);
        }
        return 1;
    }

    // ---------------------------------------------------------------- skins

    private static int skinUnlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_USER);
        String skinId = StringArgumentType.getString(ctx, ARG_SKIN);
        if (RotomPhoneSkinRegistry.getSkin(skinId) == null) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.rotomphone.skin.unknown", skinId));
            return 0;
        }
        RotomPhoneUnlockSavedData.get(target.server).unlockSkin(target.getUUID(), skinId);
        RotomPhoneConfigSync.syncToPlayer(target);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.rotomphone.skin.unlocked", skinId, target.getName()), true);
        return 1;
    }

    private static int skinLock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_USER);
        String skinId = StringArgumentType.getString(ctx, ARG_SKIN);
        RotomPhoneUnlockSavedData.get(target.server).lockSkin(target.getUUID(), skinId);
        RotomPhoneConfigSync.syncToPlayer(target);
        RotomPhoneSkinDefinition skin = RotomPhoneSkinRegistry.getSkin(skinId);
        boolean stillUnlocked = skin != null && (skin.isUnlockedFromStart()
                || RotomPhoneSkinRegistry.isUnlockedByPlayer(target, skin));
        Component note = stillUnlocked
                ? Component.translatable("cobblesafari.command.rotomphone.skin.locked_other_source", skinId, target.getName())
                : Component.translatable("cobblesafari.command.rotomphone.skin.locked", skinId, target.getName());
        ctx.getSource().sendSuccess(() -> note, true);
        return 1;
    }

    private static int skinList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_USER);
        RotomPhoneUnlockSavedData store = RotomPhoneUnlockSavedData.get(target.server);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.rotomphone.skin.list_header", target.getName()), false);
        for (RotomPhoneSkinDefinition skin : RotomPhoneSkinRegistry.getAllSkins()) {
            String origin;
            ChatFormatting color;
            if (skin.isUnlockedFromStart()) {
                origin = "default";
                color = ChatFormatting.GREEN;
            } else if (store.isSkinUnlocked(target.getUUID(), skin.getId())) {
                origin = "store";
                color = ChatFormatting.AQUA;
            } else if (RotomPhoneSkinRegistry.isUnlockedByPlayer(target, skin)) {
                origin = "advancement";
                color = ChatFormatting.YELLOW;
            } else {
                origin = "locked";
                color = ChatFormatting.GRAY;
            }
            final String id = skin.getId();
            ctx.getSource().sendSuccess(() -> Component.literal(" - " + id + ": " + origin).withStyle(color), false);
        }
        return 1;
    }
}
