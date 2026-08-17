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
import maxigregrze.cobblesafari.csmusic.CsMusicEvalContext;
import maxigregrze.cobblesafari.csmusic.CsMusicRegistry;
import maxigregrze.cobblesafari.csmusic.CsMusicStructureTracker;
import maxigregrze.cobblesafari.csmusic.DimensionalMusicManager;
import maxigregrze.cobblesafari.config.CsMusicSettingsConfig;
import maxigregrze.cobblesafari.network.SetCsMusicPayload;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class CsMusicCommand {

    private static final String ARG_AREA = "areaid";
    private static final String ARG_MUSIC = "csmusicId";
    private static final String ARG_FIRST = "first";
    private static final String ARG_SECOND = "second";
    private static final String ARG_INDEX = "index";
    private static final String ARG_DIMENSION = "dimension";
    private static final String ARG_SEEK = "seekMs";
    private static final String ARG_PRIORITY = "priority";
    private static final String ARG_TAG = "tag";
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9._-]+");
    /** Value accepted in the {@code <csmusicId>} slot to mean "no music" (geometry-only area). */
    private static final String NO_MUSIC = "-";

    private static final SuggestionProvider<CommandSourceStack> MUSIC_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(CsMusicRegistry.all().keySet(), builder);
    /** Same as {@link #MUSIC_SUGGESTIONS} plus the {@code -} sentinel, for create / setmusic. */
    private static final SuggestionProvider<CommandSourceStack> MUSIC_OR_NONE_SUGGESTIONS =
            (ctx, builder) -> {
                List<String> options = new ArrayList<>(CsMusicRegistry.all().keySet());
                options.add(NO_MUSIC);
                return SharedSuggestionProvider.suggest(options, builder);
            };
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
    /** Tags already used somewhere in the dimension - for {@code area tag add}. */
    private static final SuggestionProvider<CommandSourceStack> DIMENSION_TAG_SUGGESTIONS =
            (ctx, builder) -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player == null) {
                    return builder.buildFuture();
                }
                Set<String> tags = new TreeSet<>();
                for (CsMusicArea area : CsMusicAreaStore.areasIn(player.serverLevel())) {
                    tags.addAll(area.tags());
                }
                return SharedSuggestionProvider.suggest(tags, builder);
            };
    /** Tags actually carried by the targeted area - for {@code area tag remove}. */
    private static final SuggestionProvider<CommandSourceStack> AREA_TAG_SUGGESTIONS =
            (ctx, builder) -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player == null) {
                    return builder.buildFuture();
                }
                CsMusicArea area = CsMusicAreaStore.get(
                        player.serverLevel(), StringArgumentType.getString(ctx, ARG_AREA));
                return area == null
                        ? builder.buildFuture()
                        : SharedSuggestionProvider.suggest(area.tags(), builder);
            };

    private CsMusicCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("csmusic")
                .then(Commands.literal("list").executes(CsMusicCommand::listMusic))
                .then(Commands.literal("current").executes(CsMusicCommand::current))
                .then(Commands.literal("structures").executes(CsMusicCommand::structuresHere))
                // Debug commands disabled (test-only). Handlers retained below for re-enabling.
                /*
                .then(Commands.literal("debug")
                        .then(Commands.literal("play")
                                .then(Commands.argument(ARG_MUSIC, StringArgumentType.greedyString())
                                        .suggests(MUSIC_SUGGESTIONS)
                                        .executes(ctx -> debugPlay(ctx, 0))))
                        .then(Commands.literal("playat")
                                .then(Commands.argument(ARG_SEEK, IntegerArgumentType.integer(0))
                                        .then(Commands.argument(ARG_MUSIC, StringArgumentType.greedyString())
                                                .suggests(MUSIC_SUGGESTIONS)
                                                .executes(ctx -> debugPlay(ctx, IntegerArgumentType.getInteger(ctx, ARG_SEEK))))))
                        .then(Commands.literal("crossfade")
                                .then(Commands.argument(ARG_MUSIC, StringArgumentType.greedyString())
                                        .suggests(MUSIC_SUGGESTIONS)
                                        .executes(CsMusicCommand::debugCrossfade)))
                        .then(Commands.literal("stop").executes(CsMusicCommand::debugStop)))
                */
                .then(Commands.literal("area")
                        .then(Commands.literal("create")
                                .then(Commands.argument(ARG_AREA, StringArgumentType.word())
                                        // No music id at all: geometry-only area in the current
                                        // dimension. The '-' sentinel below covers the same intent
                                        // for a remote dimension, which this short form can't reach.
                                        .executes(CsMusicCommand::areaCreateHereNoMusic)
                                        .then(Commands.argument(ARG_MUSIC, StringArgumentType.string())
                                                .suggests(MUSIC_OR_NONE_SUGGESTIONS)
                                                .executes(CsMusicCommand::areaCreateHere)
                                                .then(Commands.argument(ARG_DIMENSION, DimensionArgument.dimension())
                                                        .executes(CsMusicCommand::areaCreateInDim)))))
                        .then(Commands.literal("setmusic")
                                .then(Commands.argument(ARG_AREA, StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .then(Commands.argument(ARG_MUSIC, StringArgumentType.string())
                                                .suggests(MUSIC_OR_NONE_SUGGESTIONS)
                                                .executes(CsMusicCommand::areaSetMusic))))
                        .then(Commands.literal("tag")
                                .then(Commands.literal("add")
                                        .then(Commands.argument(ARG_AREA, StringArgumentType.word())
                                                .suggests(AREA_SUGGESTIONS)
                                                .then(Commands.argument(ARG_TAG, StringArgumentType.word())
                                                        .suggests(DIMENSION_TAG_SUGGESTIONS)
                                                        .executes(CsMusicCommand::areaTagAdd))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument(ARG_AREA, StringArgumentType.word())
                                                .suggests(AREA_SUGGESTIONS)
                                                .then(Commands.argument(ARG_TAG, StringArgumentType.word())
                                                        .suggests(AREA_TAG_SUGGESTIONS)
                                                        .executes(CsMusicCommand::areaTagRemove)))))
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
                        .then(Commands.literal("setpriority")
                                .then(Commands.argument(ARG_AREA, StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .then(Commands.argument(ARG_PRIORITY, IntegerArgumentType.integer(1))
                                                .executes(CsMusicCommand::areaSetPriority))))
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
                    () -> Component.translatable("cobblesafari.command.csmusic.list.entry", def.id()),
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
        // Areas first: a rule carrying an area axis that never fires leaves no trace in the source
        // list, so this line is what tells a typo'd id from a misplaced box or a disabled area.
        List<CsMusicArea> areasHere = DimensionalMusicManager.areasHereFor(player);
        Component areasLabel = areasHere.isEmpty()
                ? Component.translatable("cobblesafari.command.csmusic.current.areas.none")
                : Component.literal(areasHere.stream().map(CsMusicCommand::describeArea)
                        .collect(Collectors.joining(", ")));
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csmusic.current.areas", areasLabel),
                false);
        // Resolved time and weather, then the two inputs the weather comes from: the dimension's sky,
        // and what this biome lets fall at this altitude. Reading only the resolved state cannot tell
        // "it is not raining" from "it is raining, but not in this biome".
        CsMusicEvalContext env = new CsMusicEvalContext(player);
        ServerLevel level = player.serverLevel();
        long time = env.dayTime();
        Component fixed = level.dimensionType().fixedTime().isPresent()
                ? Component.translatable("cobblesafari.command.csmusic.current.env.fixed")
                : Component.empty();
        String sky = level.isThundering() ? "thunder" : (level.isRaining() ? "rain" : "clear");
        String precip = env.biome().value()
                .getPrecipitationAt(player.blockPosition()).name().toLowerCase(Locale.ROOT);
        Component weather = Component.literal(env.weather().name().toLowerCase(Locale.ROOT));
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csmusic.current.env",
                        time, fixed, weather, sky, precip),
                false);
        if (sources.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.csmusic.current.none"),
                    false);
            return 0;
        }
        for (DimensionalMusicManager.SourceInfo info : sources) {
            // A tag pool cannot name the playing track: the draw is client-side (plan 172 § 3.7).
            // Report the pool itself instead - id, size and repeat are what an author can act on.
            Component label = info.poolSize() > 0
                    ? Component.translatable("cobblesafari.command.csmusic.current.pool",
                            info.csmusicId(), info.poolSize(), info.repeat())
                    : Component.literal(info.csmusicId());
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.csmusic.current.entry",
                            info.source(),
                            label,
                            info.priority(),
                            info.winner()
                                    ? Component.translatable("cobblesafari.command.csmusic.current.winner")
                                    : Component.empty()),
                    false);
        }
        return 1;
    }

    /**
     * Lists the naturally generated structures around the player, with both "inside" answers:
     * {@code bounds} (what {@code when.structure} actually tests) and {@code piece}. Reads fresh,
     * bypassing the {@link CsMusicStructureTracker} cache, so it reports the current truth.
     */
    private static int structuresHere(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        BlockPos pos = player.blockPosition();
        List<CsMusicStructureTracker.StructureInfo> found =
                CsMusicStructureTracker.describeAt(player.serverLevel(), pos);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "cobblesafari.command.csmusic.structures.header",
                pos.getX(), pos.getY(), pos.getZ(), found.size()), false);
        if (found.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("cobblesafari.command.csmusic.current.none"), false);
            return 0;
        }
        for (CsMusicStructureTracker.StructureInfo info : found) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "cobblesafari.command.csmusic.structures.entry",
                    info.id(), info.tags(), info.insideBounds(), info.insidePiece()), false);
            for (String pieceId : info.pieceIds()) {
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "cobblesafari.command.csmusic.structures.piece", pieceId), false);
            }
        }
        return 1;
    }

    // --- Debug (Pass 1 testing) ------------------------------------------------------------------

    private static int debugPlay(CommandContext<CommandSourceStack> ctx, int seekMs) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        CsMusicDefinition def = requireDef(ctx);
        if (def == null) {
            return 0;
        }
        DimensionalMusicManager.debugPlay(player, def, SetCsMusicPayload.MODE_CUT, seekMs);
        ctx.getSource().sendSuccess(
                () -> Component.literal("[csmusic debug] play " + def.id()
                        + (seekMs > 0 ? " @ " + seekMs + " ms" : "")),
                false);
        return 1;
    }

    private static int debugCrossfade(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        CsMusicDefinition def = requireDef(ctx);
        if (def == null) {
            return 0;
        }
        DimensionalMusicManager.debugPlay(player, def, SetCsMusicPayload.MODE_CROSSFADE, 0);
        ctx.getSource().sendSuccess(
                () -> Component.literal("[csmusic debug] crossfade -> " + def.id()),
                false);
        return 1;
    }

    private static int debugStop(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        DimensionalMusicManager.debugStop(player);
        ctx.getSource().sendSuccess(() -> Component.literal("[csmusic debug] stop"), false);
        return 1;
    }

    private static CsMusicDefinition requireDef(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, ARG_MUSIC).trim();
        CsMusicDefinition def = CsMusicRegistry.get(id).orElse(null);
        if (def == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown csmusic id: " + id));
        }
        return def;
    }

    private static int areaCreateHereNoMusic(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        return player == null ? 0 : areaCreate(ctx, player.serverLevel(), null);
    }

    private static int areaCreateHere(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        return player == null ? 0 : areaCreate(ctx, player.serverLevel(), musicArg(ctx));
    }

    private static int areaCreateInDim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return areaCreate(ctx, DimensionArgument.getDimension(ctx, ARG_DIMENSION), musicArg(ctx));
    }

    private static int areaCreate(CommandContext<CommandSourceStack> ctx, ServerLevel level, String musicId) {
        String areaId = StringArgumentType.getString(ctx, ARG_AREA);
        if (!ID_PATTERN.matcher(areaId).matches()) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.csmusic.area.invalid_id", areaId));
            return 0;
        }
        if (CsMusicAreaStore.get(level, areaId) != null) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.csmusic.area.exists", areaId));
            return 0;
        }
        String dimId = level.dimension().location().toString();
        CsMusicArea area = new CsMusicArea(areaId, musicId, Set.of(), false, defaultAreaPriority(), List.of());
        CsMusicAreaStore.put(level, area);
        CsMusicAreaStore.save(ctx.getSource().getServer(), level);
        Component music = musicLabel(musicId);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("cobblesafari.command.csmusic.area.created", areaId, music, dimId),
                true);
        return 1;
    }

    private static int areaSetMusic(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        String areaId = StringArgumentType.getString(ctx, ARG_AREA);
        CsMusicArea area = requireArea(ctx, level, areaId);
        if (area == null) {
            return 0;
        }
        String musicId = musicArg(ctx);
        CsMusicAreaStore.put(level, area.withMusic(musicId));
        CsMusicAreaStore.save(ctx.getSource().getServer(), level);
        ctx.getSource().sendSuccess(
                () -> musicId == null
                        ? Component.translatable("cobblesafari.command.csmusic.area.music_cleared", areaId)
                        : Component.translatable("cobblesafari.command.csmusic.area.music_set", areaId, musicId),
                true);
        return 1;
    }

    private static int areaTagAdd(CommandContext<CommandSourceStack> ctx) {
        return areaTagEdit(ctx, true);
    }

    private static int areaTagRemove(CommandContext<CommandSourceStack> ctx) {
        return areaTagEdit(ctx, false);
    }

    private static int areaTagEdit(CommandContext<CommandSourceStack> ctx, boolean add) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        String areaId = StringArgumentType.getString(ctx, ARG_AREA);
        CsMusicArea area = requireArea(ctx, level, areaId);
        if (area == null) {
            return 0;
        }
        String tag = StringArgumentType.getString(ctx, ARG_TAG).trim().toLowerCase(Locale.ROOT);
        if (!ID_PATTERN.matcher(tag).matches()) {
            ctx.getSource().sendFailure(
                    Component.translatable("cobblesafari.command.csmusic.area.invalid_tag", tag));
            return 0;
        }
        if (!add && !area.hasTag(tag)) {
            ctx.getSource().sendFailure(
                    Component.translatable("cobblesafari.command.csmusic.area.tag_absent", areaId, tag));
            return 0;
        }
        Set<String> tags = new LinkedHashSet<>(area.tags());
        if (add) {
            tags.add(tag); // adding an already-present tag is a no-op that still reports success
        } else {
            tags.remove(tag);
        }
        CsMusicAreaStore.put(level, area.withTags(tags));
        CsMusicAreaStore.save(ctx.getSource().getServer(), level);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(add
                        ? "cobblesafari.command.csmusic.area.tag_added"
                        : "cobblesafari.command.csmusic.area.tag_removed", tag, areaId),
                true);
        return 1;
    }

    /** Reads the {@code <csmusicId>} argument, mapping the {@code -} sentinel to "no music". */
    private static String musicArg(CommandContext<CommandSourceStack> ctx) {
        String raw = StringArgumentType.getString(ctx, ARG_MUSIC).trim();
        return raw.isEmpty() || NO_MUSIC.equals(raw) ? null : raw;
    }

    private static Component musicLabel(String musicId) {
        return musicId == null
                ? Component.translatable("cobblesafari.command.csmusic.area.no_music")
                : Component.literal(musicId);
    }

    private static int areaSetPriority(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        String areaId = StringArgumentType.getString(ctx, ARG_AREA);
        ServerLevel level = player.serverLevel();
        CsMusicArea area = requireArea(ctx, level, areaId);
        if (area == null) {
            return 0;
        }
        int priority = IntegerArgumentType.getInteger(ctx, ARG_PRIORITY);
        CsMusicAreaStore.put(level, area.withPriority(priority));
        CsMusicAreaStore.save(ctx.getSource().getServer(), level);
        ctx.getSource().sendSuccess(
                () -> Component.literal("[csmusic] area '" + areaId + "' priority = " + priority), true);
        return 1;
    }

    private static int defaultAreaPriority() {
        return Math.max(1, CsMusicSettingsConfig.data.defaultAreaPriority);
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
            int areaPriority = area.priority();
            Component status = area.activated()
                    ? Component.translatable("cobblesafari.command.csmusic.area.toggled.on")
                    : Component.translatable("cobblesafari.command.csmusic.area.toggled.off");
            int boxCount = area.boxes().size();
            Component tags = area.tags().isEmpty()
                    ? Component.empty()
                    : Component.literal(" " + String.join(", ", area.tags()));
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.csmusic.area.list.entry",
                            area.id(),
                            musicLabel(area.musicId()),
                            status,
                            areaPriority,
                            boxCount,
                            tags),
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
                        musicLabel(area.musicId())),
                false);
        if (!area.tags().isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "cobblesafari.command.csmusic.area.info.tags",
                            String.join(", ", area.tags())),
                    false);
        }
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

    /** {@code lumiose_city [town, hub]} - id plus tags, for the {@code current} header line. */
    private static String describeArea(CsMusicArea area) {
        return area.tags().isEmpty()
                ? area.id()
                : area.id() + " [" + String.join(", ", area.tags()) + "]";
    }

    private static String formatPos(int x, int y, int z) {
        return "[" + x + ", " + y + ", " + z + "]";
    }
}
