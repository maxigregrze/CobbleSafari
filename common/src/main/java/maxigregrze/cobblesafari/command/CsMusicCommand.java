package maxigregrze.cobblesafari.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import maxigregrze.cobblesafari.csmusic.CsMusicArea;
import maxigregrze.cobblesafari.csmusic.CsMusicAreaStore;
import maxigregrze.cobblesafari.csmusic.CsMusicBox;
import maxigregrze.cobblesafari.csmusic.CsMusicDefinition;
import maxigregrze.cobblesafari.csmusic.CsMusicRegistry;
import maxigregrze.cobblesafari.csmusic.DimensionalMusicManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class CsMusicCommand {

    private static final String ARG_AREA = "areaid";
    private static final String ARG_MUSIC = "csmusicId";
    private static final String ARG_FIRST = "first";
    private static final String ARG_SECOND = "second";
    private static final String ARG_INDEX = "index";
    private static final String ARG_DIMENSION = "dimension";
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9._-]+");

    private static final SuggestionProvider<CommandSourceStack> MUSIC_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(CsMusicRegistry.all().keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> AREA_SUGGESTIONS =
            (ctx, builder) -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player == null) {
                    return builder.buildFuture();
                }
                return SharedSuggestionProvider.suggest(
                        CsMusicAreaStore.areasIn(player.serverLevel()).stream().map(CsMusicArea::id).toList(),
                        builder);
            };

    private CsMusicCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("csmusic")
                .then(Commands.literal("list").executes(CsMusicCommand::listMusic))
                .then(Commands.literal("current").executes(CsMusicCommand::current))
                .then(Commands.literal("area")
                        .then(Commands.literal("create")
                                .then(Commands.argument(ARG_AREA, StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .then(Commands.argument(ARG_MUSIC, StringArgumentType.string())
                                                .suggests(MUSIC_SUGGESTIONS)
                                                .executes(CsMusicCommand::areaCreateHere)
                                                .then(Commands.argument(ARG_DIMENSION, DimensionArgument.dimension())
                                                        .executes(CsMusicCommand::areaCreateInDim)))))
                        .then(Commands.literal("addvolume")
                                .then(Commands.argument(ARG_AREA, StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .then(Commands.argument(ARG_FIRST, BlockPosArgument.blockPos())
                                                .then(Commands.argument(ARG_SECOND, BlockPosArgument.blockPos())
                                                        .executes(CsMusicCommand::areaAddVolume)))))
                        .then(Commands.literal("removevolume")
                                .then(Commands.argument(ARG_AREA, StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .then(Commands.argument(ARG_INDEX, IntegerArgumentType.integer(0))
                                                .executes(CsMusicCommand::areaRemoveVolume))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument(ARG_AREA, StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .executes(CsMusicCommand::areaRemove)))
                        .then(Commands.literal("toggle")
                                .then(Commands.argument(ARG_AREA, StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .executes(CsMusicCommand::areaToggleHere)
                                        .then(Commands.argument(ARG_DIMENSION, DimensionArgument.dimension())
                                                .executes(CsMusicCommand::areaToggleInDim))))
                        .then(Commands.literal("list").executes(CsMusicCommand::areaList))
                        .then(Commands.literal("info")
                                .then(Commands.argument(ARG_AREA, StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .executes(CsMusicCommand::areaInfoHere)
                                        .then(Commands.argument(ARG_DIMENSION, DimensionArgument.dimension())
                                                .executes(CsMusicCommand::areaInfoInDim))))
                        .then(Commands.literal("reload").executes(CsMusicCommand::areaReload)));
    }

    private static int listMusic(CommandContext<CommandSourceStack> ctx) {
        Map<String, CsMusicDefinition> all = CsMusicRegistry.all();
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csmusic.list.header", all.size()),
                false);
        if (all.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.csmusic.current.none"),
                    false);
            return 0;
        }
        for (CsMusicDefinition def : all.values()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.csmusic.list.entry", def.id(), def.priority()),
                    false);
        }
        return 1;
    }

    private static int current(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        List<DimensionalMusicManager.SourceInfo> sources = DimensionalMusicManager.describeSourcesFor(player);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csmusic.current.header", player.getGameProfile().getName()),
                false);
        if (sources.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.csmusic.current.none"),
                    false);
            return 0;
        }
        for (DimensionalMusicManager.SourceInfo info : sources) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.csmusic.current.entry",
                            info.source(),
                            info.csmusicId(),
                            info.priority(),
                            info.winner()
                                    ? Component.translatable("cobblesafari.command.csmusic.current.winner")
                                    : Component.empty()),
                    false);
        }
        return 1;
    }

    private static int areaCreateHere(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        return player == null ? 0 : areaCreate(ctx, player.serverLevel());
    }

    private static int areaCreateInDim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return areaCreate(ctx, DimensionArgument.getDimension(ctx, ARG_DIMENSION));
    }

    private static int areaCreate(CommandContext<CommandSourceStack> ctx, ServerLevel level) {
        String areaId = StringArgumentType.getString(ctx, ARG_AREA);
        if (!ID_PATTERN.matcher(areaId).matches()) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.csmusic.area.invalid_id", areaId));
            return 0;
        }
        if (CsMusicAreaStore.get(level, areaId) != null) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.csmusic.area.exists", areaId));
            return 0;
        }
        String musicId = StringArgumentType.getString(ctx, ARG_MUSIC);
        String dimId = level.dimension().location().toString();
        CsMusicArea area = new CsMusicArea(areaId, musicId, false, List.of());
        CsMusicAreaStore.put(level, area);
        CsMusicAreaStore.save(ctx.getSource().getServer(), level);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csmusic.area.created", areaId, musicId, dimId),
                true);
        return 1;
    }

    private static int areaAddVolume(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        String areaId = StringArgumentType.getString(ctx, ARG_AREA);
        CsMusicArea area = requireArea(ctx, player.serverLevel(), areaId);
        if (area == null) {
            return 0;
        }
        BlockPos first = BlockPosArgument.getBlockPos(ctx, ARG_FIRST);
        BlockPos second = BlockPosArgument.getBlockPos(ctx, ARG_SECOND);
        CsMusicBox box = CsMusicBox.of(first, second);
        List<CsMusicBox> boxes = new ArrayList<>(area.boxes());
        boxes.add(box);
        CsMusicArea updated = area.withBoxes(boxes);
        ServerLevel level = player.serverLevel();
        CsMusicAreaStore.put(level, updated);
        CsMusicAreaStore.save(ctx.getSource().getServer(), level);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "cobblesafari.command.csmusic.area.added_volume",
                        formatPos(box.minX(), box.minY(), box.minZ()),
                        formatPos(box.maxX(), box.maxY(), box.maxZ()),
                        areaId,
                        boxes.size()),
                true);
        return 1;
    }

    private static int areaRemoveVolume(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        String areaId = StringArgumentType.getString(ctx, ARG_AREA);
        CsMusicArea area = requireArea(ctx, player.serverLevel(), areaId);
        if (area == null) {
            return 0;
        }
        int index = IntegerArgumentType.getInteger(ctx, ARG_INDEX);
        if (index < 0 || index >= area.boxes().size()) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.csmusic.area.box_index_out_of_range", index));
            return 0;
        }
        List<CsMusicBox> boxes = new ArrayList<>(area.boxes());
        boxes.remove(index);
        ServerLevel level = player.serverLevel();
        CsMusicAreaStore.put(level, area.withBoxes(boxes));
        CsMusicAreaStore.save(ctx.getSource().getServer(), level);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csmusic.area.removed_volume", index, areaId),
                true);
        return 1;
    }

    private static int areaRemove(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        String areaId = StringArgumentType.getString(ctx, ARG_AREA);
        if (CsMusicAreaStore.get(player.serverLevel(), areaId) == null) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.csmusic.area.not_found", areaId));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        CsMusicAreaStore.remove(level, areaId);
        CsMusicAreaStore.save(ctx.getSource().getServer(), level);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csmusic.area.removed", areaId),
                true);
        return 1;
    }

    private static int areaToggleHere(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        return player == null ? 0 : areaToggle(ctx, player.serverLevel());
    }

    private static int areaToggleInDim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return areaToggle(ctx, DimensionArgument.getDimension(ctx, ARG_DIMENSION));
    }

    private static int areaToggle(CommandContext<CommandSourceStack> ctx, ServerLevel level) {
        String areaId = StringArgumentType.getString(ctx, ARG_AREA);
        CsMusicArea area = requireArea(ctx, level, areaId);
        if (area == null) {
            return 0;
        }
        CsMusicArea updated = area.withActivated(!area.activated());
        CsMusicAreaStore.put(level, updated);
        CsMusicAreaStore.save(ctx.getSource().getServer(), level);
        Component state = Component.translatable(updated.activated()
                ? "cobblesafari.command.csmusic.area.toggled.on"
                : "cobblesafari.command.csmusic.area.toggled.off");
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csmusic.area.toggled", areaId, state),
                true);
        return 1;
    }

    private static int areaList(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        String dimId = level.dimension().location().toString();
        List<CsMusicArea> areas = new ArrayList<>(CsMusicAreaStore.areasIn(level));
        areas.sort(Comparator.comparing(CsMusicArea::id));
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csmusic.area.list.header", dimId, areas.size()),
                false);
        for (CsMusicArea area : areas) {
            CsMusicDefinition def = CsMusicRegistry.get(area.musicId()).orElse(null);
            int musicPriority = def != null ? def.priority() : CsMusicDefinition.DEFAULT_PRIORITY;
            Component status = area.activated()
                    ? Component.translatable("cobblesafari.command.csmusic.area.toggled.on")
                    : Component.translatable("cobblesafari.command.csmusic.area.toggled.off");
            int boxCount = area.boxes().size();
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.csmusic.area.list.entry",
                            area.id(),
                            area.musicId(),
                            status,
                            musicPriority,
                            boxCount),
                    false);
        }
        return 1;
    }

    private static int areaInfoHere(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        return player == null ? 0 : areaInfo(ctx, player.serverLevel());
    }

    private static int areaInfoInDim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return areaInfo(ctx, DimensionArgument.getDimension(ctx, ARG_DIMENSION));
    }

    private static int areaInfo(CommandContext<CommandSourceStack> ctx, ServerLevel level) {
        String areaId = StringArgumentType.getString(ctx, ARG_AREA);
        CsMusicArea area = requireArea(ctx, level, areaId);
        if (area == null) {
            return 0;
        }
        Component status = area.activated()
                ? Component.translatable("cobblesafari.command.csmusic.area.toggled.on")
                : Component.translatable("cobblesafari.command.csmusic.area.toggled.off");
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "cobblesafari.command.csmusic.area.info.header",
                        areaId,
                        status,
                        area.musicId()),
                false);
        for (int i = 0; i < area.boxes().size(); i++) {
            CsMusicBox box = area.boxes().get(i);
            int index = i;
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.csmusic.area.info.box",
                            index,
                            formatPos(box.minX(), box.minY(), box.minZ()),
                            formatPos(box.maxX(), box.maxY(), box.maxZ())),
                    false);
        }
        return 1;
    }

    private static int areaReload(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        CsMusicAreaStore.reload(ctx.getSource().getServer(), level);
        int count = CsMusicAreaStore.areasIn(level).size();
        String dimId = level.dimension().location().toString();
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csmusic.area.reloaded", count, dimId),
                true);
        return 1;
    }

    private static ServerPlayer requirePlayer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.csmusic.area.player_only"));
        }
        return player;
    }

    private static CsMusicArea requireArea(CommandContext<CommandSourceStack> ctx, ServerLevel level, String areaId) {
        CsMusicArea area = CsMusicAreaStore.get(level, areaId);
        if (area == null) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.csmusic.area.not_found", areaId));
        }
        return area;
    }

    private static String formatPos(int x, int y, int z) {
        return "[" + x + ", " + y + ", " + z + "]";
    }
}
