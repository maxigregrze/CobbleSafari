package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import maxigregrze.cobblesafari.data.UnionRoomSavedData;
import maxigregrze.cobblesafari.unionroom.UnionRoomManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class UnionRoomCommand {

    private static final String MSG_PLAYER_ONLY = "This command must be run by a player";

    private static final SuggestionProvider<CommandSourceStack> INSTANCE_TYPE_SUGGESTIONS =
            (ctx, builder) -> {
                builder.suggest("room");
                builder.suggest("plaza");
                return builder.buildFuture();
            };

    private UnionRoomCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("union")
                .then(Commands.literal("create")
                        .executes(ctx -> executeCreate(ctx, "room"))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(INSTANCE_TYPE_SUGGESTIONS)
                                .executes(ctx -> {
                                    String type = StringArgumentType.getString(ctx, "type");
                                    if (!"plaza".equals(type)) {
                                        type = "room";
                                    }
                                    return executeCreate(ctx, type);
                                })))
                .then(Commands.literal("join")
                        .then(Commands.argument("d1", IntegerArgumentType.integer(1, 6))
                                .then(Commands.argument("d2", IntegerArgumentType.integer(1, 6))
                                        .then(Commands.argument("d3", IntegerArgumentType.integer(1, 6))
                                                .then(Commands.argument("d4", IntegerArgumentType.integer(1, 6))
                                                        .executes(UnionRoomCommand::executeJoin))))))
                .then(Commands.literal("list")
                        .requires(source -> source.hasPermission(4))
                        .executes(ctx -> executeList(ctx, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> executeList(ctx, IntegerArgumentType.getInteger(ctx, "page")))))
                .then(Commands.literal("instances")
                        .requires(source -> source.hasPermission(4))
                        .executes(ctx -> executeInstances(ctx, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> executeInstances(ctx, IntegerArgumentType.getInteger(ctx, "page")))))
                .then(Commands.literal("forcejoin")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(UnionRoomCommand::executeForceJoin)))
                .then(Commands.literal("disband")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                .executes(UnionRoomCommand::executeDisband)
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(UnionRoomCommand::executeDisbandWithReason))));
    }

    private static int executeCreate(CommandContext<CommandSourceStack> ctx, String instanceType) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal(MSG_PLAYER_ONLY));
            return 0;
        }
        return UnionRoomManager.createSession(player, instanceType) == UnionRoomManager.CreateResult.OK ? 1 : 0;
    }

    private static int executeJoin(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal(MSG_PLAYER_ONLY));
            return 0;
        }
        int[] code = {
                IntegerArgumentType.getInteger(ctx, "d1"),
                IntegerArgumentType.getInteger(ctx, "d2"),
                IntegerArgumentType.getInteger(ctx, "d3"),
                IntegerArgumentType.getInteger(ctx, "d4")
        };
        return UnionRoomManager.joinSession(player, code) == UnionRoomManager.JoinResult.OK ? 1 : 0;
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx, int page) {
        MinecraftServer server = ctx.getSource().getServer();
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.unionroom.error.dimension_not_found"));
            return 0;
        }
        List<UnionRoomSavedData.SessionData> sessions = data.getAllSessions();
        int pageSize = 10;
        int totalPages = Math.max(1, (sessions.size() + pageSize - 1) / pageSize);
        int effectivePage = Math.min(page, totalPages);
        int start = (effectivePage - 1) * pageSize;
        if (sessions.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.unionroom.list.empty"), false);
            return 1;
        }
        final int headerPage = effectivePage;
        final int headerTotal = totalPages;
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.unionroom.list.header", headerPage, headerTotal), false);
        for (int i = start; i < Math.min(start + pageSize, sessions.size()); i++) {
            UnionRoomSavedData.SessionData session = sessions.get(i);
            String hostName = resolveUsername(server, session.hostUUID);
            String codeStr = session.code[0] + "-" + session.code[1] + "-" + session.code[2] + "-" + session.code[3];
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.unionroom.list.entry", session.instanceId, hostName, codeStr),
                    false);
        }
        return 1;
    }

    private static int executeInstances(CommandContext<CommandSourceStack> ctx, int page) {
        MinecraftServer server = ctx.getSource().getServer();
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.unionroom.error.dimension_not_found"));
            return 0;
        }
        List<UnionRoomSavedData.InstanceData> instances = data.getInstances();
        int pageSize = 10;
        int totalPages = Math.max(1, (instances.size() + pageSize - 1) / pageSize);
        int effectivePage = Math.min(page, totalPages);
        int start = (effectivePage - 1) * pageSize;
        if (instances.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.unionroom.instances.empty"), false);
            return 1;
        }
        final int headerPage = effectivePage;
        final int headerTotal = totalPages;
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.unionroom.instances.header", headerPage, headerTotal), false);
        for (int i = start; i < Math.min(start + pageSize, instances.size()); i++) {
            UnionRoomSavedData.InstanceData inst = instances.get(i);
            String status = inst.occupied ? "OCCUPIED" : "EMPTY";
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.unionroom.instances.entry", inst.id, inst.structurePos.getX(),
                            inst.structurePos.getY(), inst.structurePos.getZ(), status),
                    false);
        }
        return 1;
    }

    private static int executeForceJoin(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal(MSG_PLAYER_ONLY));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (UnionRoomManager.forceJoinInstance(player, id)) {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.unionroom.forcejoin.success", id), false);
            return 1;
        }
        return 0;
    }

    private static int executeDisband(CommandContext<CommandSourceStack> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        UnionRoomManager.disbandSession(ctx.getSource().getServer(), id, null);
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.unionroom.disband.success", id), true);
        return 1;
    }

    private static int executeDisbandWithReason(CommandContext<CommandSourceStack> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        String reason = StringArgumentType.getString(ctx, "reason");
        UnionRoomManager.disbandSession(ctx.getSource().getServer(), id, reason);
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.unionroom.disband.success", id), true);
        return 1;
    }

    private static String resolveUsername(MinecraftServer server, java.util.UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        Optional<com.mojang.authlib.GameProfile> profile = server.getProfileCache().get(uuid);
        return profile.map(com.mojang.authlib.GameProfile::getName).orElseGet(uuid::toString);
    }
}
