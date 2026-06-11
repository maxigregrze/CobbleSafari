package maxigregrze.cobblesafari.command;

import com.cobblemon.mod.common.api.mark.Mark;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import maxigregrze.cobblesafari.data.GtsSavedData;
import maxigregrze.cobblesafari.gts.GenderFilter;
import maxigregrze.cobblesafari.gts.GtsOffer;
import maxigregrze.cobblesafari.gts.GtsService;
import maxigregrze.cobblesafari.gts.GtsSuccess;
import maxigregrze.cobblesafari.gts.GtsTradeCandidate;
import maxigregrze.cobblesafari.gts.GtsUniqueOfferDefinition;
import maxigregrze.cobblesafari.gts.GtsUniqueOfferRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class GtsCommand {
    private static final String ARG_PLAYER = "player";
    private static final String ARG_SLOT = "slot";
    private static final String ARG_SPECIES = "species";
    private static final String ARG_LEVEL_BUCKET = "levelBucket";
    private static final String ARG_GENDER = "gender";
    private static final String ARG_SHINY = "shiny";
    private static final String ARG_ID = "id";
    private static final String MSG_GTS_ERROR = "cobblesafari.command.gts.error";
    private static final String KEY_SUCCESS = "success";
    private static final String ARG_PICK = "pickIndex";
    private static final String ARG_PAGE = "page";
    private static final String ARG_OFFER_ID = "offerId";
    private static final String ARG_SUCCESS_ID = "successId";

    private static final SimpleCommandExceptionType INVALID_LEVEL_BUCKET =
            new SimpleCommandExceptionType(Component.translatable("cobblesafari.command.gts.invalid_level_bucket"));

    /** Same order as official permanent stats (see Cobblemon {@code Stats.PERMANENT}). */
    private static final Stat[] PERMANENT_STATS = {
        Stats.HP,
        Stats.ATTACK,
        Stats.DEFENCE,
        Stats.SPECIAL_ATTACK,
        Stats.SPECIAL_DEFENCE,
        Stats.SPEED
    };

    private GtsCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("gts")
                .then(Commands.literal("list")
                        .requires(s -> s.hasPermission(4))
                        .then(Commands.literal("offers")
                                .executes(ctx -> listOffers(ctx, 1))
                                .then(Commands.argument(ARG_PAGE, IntegerArgumentType.integer(1))
                                        .executes(ctx -> listOffers(ctx, IntegerArgumentType.getInteger(ctx, ARG_PAGE)))))
                        .then(Commands.literal(KEY_SUCCESS)
                                .executes(ctx -> listSuccess(ctx, 1))
                                .then(Commands.argument(ARG_PAGE, IntegerArgumentType.integer(1))
                                        .executes(ctx -> listSuccess(ctx, IntegerArgumentType.getInteger(ctx, ARG_PAGE))))))
                .then(Commands.literal("details")
                        .requires(s -> s.hasPermission(4))
                        .then(Commands.literal("offer")
                                .then(Commands.argument(ARG_ID, IntegerArgumentType.integer(1))
                                        .executes(GtsCommand::detailsOffer)))
                        .then(Commands.literal(KEY_SUCCESS)
                                .then(Commands.argument(ARG_ID, IntegerArgumentType.integer(1))
                                        .executes(GtsCommand::detailsSuccess))))
                .then(Commands.literal("remove")
                        .requires(s -> s.hasPermission(4))
                        .then(Commands.argument(ARG_ID, IntegerArgumentType.integer(1))
                                .executes(GtsCommand::adminRemove)))
                .then(Commands.literal("test-deposit")
                        .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                .then(Commands.argument(ARG_SLOT, IntegerArgumentType.integer(1, 6))
                                        .then(Commands.argument(ARG_SPECIES, StringArgumentType.string())
                                                .then(Commands.argument(ARG_LEVEL_BUCKET, StringArgumentType.word())
                                                        .then(Commands.argument(ARG_GENDER, StringArgumentType.word())
                                                                .then(Commands.argument(ARG_SHINY, StringArgumentType.word())
                                                                        .executes(GtsCommand::testDeposit))))))))
                .then(Commands.literal("test-trade")
                        .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                .then(Commands.argument(ARG_ID, IntegerArgumentType.integer(1))
                                        .executes(GtsCommand::testTrade))))
                .then(Commands.literal("test-tradeconfirm")
                        .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                .then(Commands.argument(ARG_ID, IntegerArgumentType.integer(1))
                                        .then(Commands.argument(ARG_PICK, IntegerArgumentType.integer(1))
                                                .executes(GtsCommand::testTradeConfirm)))))
                .then(Commands.literal("test-retrieve")
                        .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                .then(Commands.argument(ARG_OFFER_ID, IntegerArgumentType.integer(1))
                                        .executes(GtsCommand::testRetrieve))))
                .then(Commands.literal("test-claim")
                        .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                .then(Commands.argument(ARG_SUCCESS_ID, IntegerArgumentType.integer(1))
                                        .executes(GtsCommand::testClaim))))
                .then(Commands.literal("uniqueoffer")
                        .then(Commands.literal("add")
                                .requires(s -> s.hasPermission(4))
                                .then(Commands.argument(ARG_OFFER_ID, StringArgumentType.greedyString())
                                        .suggests(GtsCommand::suggestUniqueOfferIds)
                                        .executes(GtsCommand::uniqueOfferAdd)))
                        .then(Commands.literal("list")
                                .requires(s -> s.hasPermission(4))
                                .executes(ctx -> uniqueOfferList(ctx, 1))
                                .then(Commands.argument(ARG_PAGE, IntegerArgumentType.integer(1))
                                        .executes(
                                                ctx -> uniqueOfferList(
                                                        ctx, IntegerArgumentType.getInteger(ctx, ARG_PAGE)))))
                        .then(Commands.literal("details")
                                .requires(s -> s.hasPermission(4))
                                .then(Commands.argument(ARG_OFFER_ID, StringArgumentType.greedyString())
                                        .suggests(GtsCommand::suggestUniqueOfferIds)
                                        .executes(GtsCommand::uniqueOfferDetails)))
                        .then(Commands.literal("personal")
                                .requires(s -> s.hasPermission(4))
                                .then(Commands.literal("add")
                                        .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                .then(Commands.argument(ARG_OFFER_ID, StringArgumentType.greedyString())
                                                        .suggests(GtsCommand::suggestUniqueOfferIds)
                                                        .executes(GtsCommand::personalAdd))))
                                .then(Commands.literal("list")
                                        .executes(ctx -> personalList(ctx, null, 1))
                                        .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                .executes(
                                                        ctx ->
                                                                personalList(
                                                                        ctx,
                                                                        EntityArgument.getPlayer(ctx, ARG_PLAYER),
                                                                        1))
                                                .then(Commands.argument(ARG_PAGE, IntegerArgumentType.integer(1))
                                                        .executes(
                                                                ctx ->
                                                                        personalList(
                                                                                ctx,
                                                                                EntityArgument.getPlayer(ctx, ARG_PLAYER),
                                                                                IntegerArgumentType.getInteger(
                                                                                        ctx, ARG_PAGE))))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument(ARG_PLAYER, EntityArgument.player())
                                                .then(Commands.argument(ARG_OFFER_ID, StringArgumentType.greedyString())
                                                        .suggests(GtsCommand::suggestUniqueOfferIds)
                                                        .executes(GtsCommand::personalRemove))))));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestUniqueOfferIds(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        java.util.stream.Stream<String> ids = GtsUniqueOfferRegistry.getAll().keySet().stream();
        java.util.stream.Stream<String> tagTokens =
                GtsUniqueOfferRegistry.getAllTags().stream().map(t -> GtsService.TAG_PREFIX + t);
        return SharedSuggestionProvider.suggest(
                java.util.stream.Stream.concat(ids, tagTokens).sorted(), builder);
    }

    private static int parseLevelBucketToken(String raw) throws CommandSyntaxException {
        if (raw == null) {
            throw INVALID_LEVEL_BUCKET.create();
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.equals("any")) {
            return -1;
        }
        try {
            int v = Integer.parseInt(s);
            if (v < 0 || v > 9) {
                throw INVALID_LEVEL_BUCKET.create();
            }
            return v;
        } catch (NumberFormatException e) {
            throw INVALID_LEVEL_BUCKET.create();
        }
    }

    private static int listOffers(CommandContext<CommandSourceStack> ctx, int page) {
        MinecraftServer server = ctx.getSource().getServer();
        GtsSavedData data = GtsSavedData.get(server);
        List<GtsOffer> all = data.getOffers();
        int pageSize = 5;
        int from = (page - 1) * pageSize;
        if (from >= all.size()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.list_offers_empty", page), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.list_offers_header", page), false);
        int to = Math.min(from + pageSize, all.size());
        for (int i = from; i < to; i++) {
            GtsOffer o = all.get(i);
            String dep = speciesNameFromOffer(server, o);
            String wish = o.getWishSpecies();
            String lvl = levelLabel(o.getWishLevelBucket());
            String gen = o.getWishGender().name().toLowerCase(Locale.ROOT);
            final String fd = dep;
            final String fw = wish;
            final String flv = lvl;
            final String fg = gen;
            final int fid = o.getId();
            if (o.isPersonalOffer()) {
                final String templateId = o.getUniqueOfferTemplateId();
                final String fTarget =
                        GtsService.lookupUsername(server, o.getPersonalTargetUuid())
                                .orElse(o.getPersonalTargetUuid().toString());
                ctx.getSource()
                        .sendSuccess(
                                () -> Component.translatable(
                                        "cobblesafari.command.gts.list_offers_line_personal",
                                        fid,
                                        fTarget,
                                        templateId,
                                        fd,
                                        fw,
                                        flv,
                                        fg),
                                false);
            } else if (o.isUniqueOffer()) {
                final String templateId = o.getUniqueOfferTemplateId();
                ctx.getSource()
                        .sendSuccess(
                                () -> Component.translatable(
                                        "cobblesafari.command.gts.list_offers_line_unique",
                                        fid,
                                        templateId,
                                        fd,
                                        fw,
                                        flv,
                                        fg),
                                false);
            } else {
                String user =
                        GtsService.lookupUsername(server, o.getDepositorUuid())
                                .orElse(o.getDepositorUuid().toString());
                final String fu = user;
                ctx.getSource()
                        .sendSuccess(
                                () -> Component.translatable(
                                        "cobblesafari.command.gts.list_offers_line", fid, fu, fd, fw, flv, fg),
                                false);
            }
        }
        return 1;
    }

    private static int listSuccess(CommandContext<CommandSourceStack> ctx, int page) {
        MinecraftServer server = ctx.getSource().getServer();
        GtsSavedData data = GtsSavedData.get(server);
        List<GtsSuccess> all = data.getSuccesses();
        int pageSize = 10;
        int from = (page - 1) * pageSize;
        if (from >= all.size()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.list_success_empty", page), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.list_success_header", page), false);
        int to = Math.min(from + pageSize, all.size());
        for (int i = from; i < to; i++) {
            GtsSuccess s = all.get(i);
            String user = GtsService.lookupUsername(server, s.getRecipientUuid()).orElse(s.getRecipientUuid().toString());
            String sp = speciesNameFromNbt(server, s.getPokemonData());
            final int sid = s.getId();
            final String u = user;
            final String spf = sp;
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.list_success_line", sid, u, spf), false);
        }
        return 1;
    }

    private static int detailsOffer(CommandContext<CommandSourceStack> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, ARG_ID);
        Optional<GtsOffer> opt = GtsSavedData.get(ctx.getSource().getServer()).findOffer(id);
        if (opt.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.details_not_found"));
            return 0;
        }
        GtsOffer o = opt.get();
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.details_header", "offer", id), false);

        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.details_section_requested"), false);
        sendRequestedWishSummary(ctx, o);

        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.details_section_deposited"), false);
        sendPokemonDetails(ctx, o.getPokemonData(), null, id);
        return 1;
    }

    private static int detailsSuccess(CommandContext<CommandSourceStack> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, ARG_ID);
        Optional<GtsSuccess> opt = GtsSavedData.get(ctx.getSource().getServer()).findSuccess(id);
        if (opt.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.details_not_found"));
            return 0;
        }
        sendPokemonDetails(ctx, opt.get().getPokemonData(), KEY_SUCCESS, id);
        return 1;
    }

    /**
     * Prints species summary, level band, and gender filter for the wishlist line (parsed via Cobblemon
     * {@link PokemonProperties}, same as deposit validation).
     */
    private static void sendRequestedWishSummary(CommandContext<CommandSourceStack> ctx, GtsOffer o) {
        String wishLine = o.getWishSpecies();
        try {
            PokemonProperties wishProps = PokemonProperties.Companion.parse(wishLine);
            if (wishProps.getSpecies() == null) {
                ctx.getSource()
                        .sendSuccess(
                                () -> Component.translatable("cobblesafari.command.gts.details_requested_parse_error", wishLine),
                                false);
                return;
            }
            Pokemon wishMon = wishProps.create(null);
            String speciesName = wishMon.getSpecies().getName();
            String lvl = levelLabel(o.getWishLevelBucket());
            String gen = o.getWishGender().name().toLowerCase(Locale.ROOT);
            ctx.getSource()
                    .sendSuccess(
                            () -> Component.translatable(
                                    "cobblesafari.command.gts.details_requested_line", speciesName, lvl, gen, wishLine),
                            false);
        } catch (Exception ex) {
            ctx.getSource()
                    .sendSuccess(
                            () -> Component.translatable("cobblesafari.command.gts.details_requested_parse_error", wishLine),
                            false);
        }
    }

    /**
     * @param kindHeader {@code KEY_SUCCESS} to print the GTS header, or {@code null} when the header was already printed
     *     (e.g. offer details after the wishlist section).
     */
    private static void sendPokemonDetails(
            CommandContext<CommandSourceStack> ctx, net.minecraft.nbt.CompoundTag nbt, String kindHeader, int id) {
        try {
            Pokemon p = Pokemon.Companion.loadFromNBT(ctx.getSource().getServer().registryAccess(), nbt.copy());
            if (kindHeader != null) {
                ctx.getSource()
                        .sendSuccess(() -> Component.translatable("cobblesafari.command.gts.details_header", kindHeader, id), false);
            }
            ctx.getSource()
                    .sendSuccess(
                            () -> Component.translatable(
                                    "cobblesafari.command.gts.details_body",
                                    p.getSpecies().getName(),
                                    p.getLevel(),
                                    Boolean.TRUE.equals(p.getShiny()),
                                    p.getGender().name(),
                                    p.getOriginalTrainerName() != null ? p.getOriginalTrainerName() : "",
                                    p.heldItem().getHoverName().getString()),
                            false);

            IVs ivs = p.getIvs();
            String ivLine = formatIvsSlash(ivs);
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.details_ivs", ivLine), false);

            EVs evs = p.getEvs();
            int evSum = evs.total();
            String evSpread = formatEvSpread(evs);
            ctx.getSource()
                    .sendSuccess(() -> Component.translatable("cobblesafari.command.gts.details_evs", evSum, evSpread), false);

            sendMarksLines(ctx, p);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.details_error"));
        }
    }

    private static String formatIvsSlash(IVs ivs) {
        StringBuilder sb = new StringBuilder();
        for (Stat s : PERMANENT_STATS) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append(ivs.getEffectiveBattleIV(s));
            if (ivs.isHyperTrained(s)) {
                sb.append('*');
            }
        }
        return sb.toString();
    }

    private static String formatEvSpread(EVs evs) {
        List<String> chunks = new ArrayList<>();
        for (Stat s : PERMANENT_STATS) {
            int v = evs.getOrDefault(s);
            if (v != 0) {
                chunks.add(s.getShowdownId().toUpperCase(Locale.ROOT) + v);
            }
        }
        if (chunks.isEmpty()) {
            return "—";
        }
        return String.join(" ", chunks);
    }

    private static void sendMarksLines(CommandContext<CommandSourceStack> ctx, Pokemon p) {
        Set<Mark> owned = p.getMarks();
        if (owned.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.details_marks_none"), false);
            return;
        }
        List<Mark> sorted = new ArrayList<>(owned);
        sorted.sort(Comparator.comparing(m -> m.getIdentifier().toString()));
        Mark active = p.getActiveMark();
        MutableComponent line = Component.translatable("cobblesafari.command.gts.details_marks_owned_label");
        boolean first = true;
        for (Mark m : sorted) {
            if (!first) {
                line.append(Component.literal(", "));
            }
            first = false;
            line.append(Component.literal(m.getIdentifier().toString()));
            if (active != null && active.getIdentifier().equals(m.getIdentifier())) {
                line.append(Component.translatable("cobblesafari.command.gts.details_mark_active_tag"));
            }
        }
        ctx.getSource().sendSuccess(() -> line, false);
    }

    private static int uniqueOfferAdd(CommandContext<CommandSourceStack> ctx) {
        String token = StringArgumentType.getString(ctx, ARG_OFFER_ID).trim();
        MinecraftServer server = ctx.getSource().getServer();
        GtsService.AddUniqueOfferOutcome outcome;
        String tagForError = null;
        if (token.toLowerCase(Locale.ROOT).startsWith(GtsService.TAG_PREFIX)) {
            tagForError = token.substring(GtsService.TAG_PREFIX.length()).trim();
            outcome = GtsService.addUniqueOfferByTag(server, tagForError);
        } else {
            outcome = GtsService.addUniqueOffer(server, token);
        }
        final String resolvedTemplateId =
                outcome.result() == GtsService.AddUniqueOfferResult.SUCCESS
                        ? GtsSavedData.get(server)
                                .findOffer(outcome.runtimeOfferId())
                                .map(GtsOffer::getUniqueOfferTemplateId)
                                .orElse(token)
                        : token;
        return switch (outcome.result()) {
            case SUCCESS -> {
                ctx.getSource()
                        .sendSuccess(
                                () -> Component.translatable(
                                        "cobblesafari.command.gts.uniqueoffer.add_success",
                                        outcome.runtimeOfferId(),
                                        resolvedTemplateId),
                                true);
                yield 1;
            }
            case UNKNOWN_OFFER_ID -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.add_unknown"));
                yield 0;
            }
            case NO_TAG_MATCH -> {
                ctx.getSource()
                        .sendFailure(
                                Component.translatable(
                                        "cobblesafari.command.gts.uniqueoffer.add_no_tag_match", tagForError));
                yield 0;
            }
            case INVALID_GIVEN -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.add_invalid_given"));
                yield 0;
            }
            case BANNED_GIVEN -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.add_banned_given"));
                yield 0;
            }
            case BANNED_WISH -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.add_banned_wish"));
                yield 0;
            }
            case INCOMPATIBLE_GENDER -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.add_incompatible_gender"));
                yield 0;
            }
            case INVALID_LEVEL_BUCKET -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.add_invalid_level"));
                yield 0;
            }
            case ERROR -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.add_error"));
                yield 0;
            }
        };
    }

    private static int uniqueOfferList(CommandContext<CommandSourceStack> ctx, int page) {
        List<GtsUniqueOfferDefinition> defs =
                GtsUniqueOfferRegistry.getAll().values().stream()
                        .sorted(Comparator.comparing(GtsUniqueOfferDefinition::getOfferId))
                        .collect(Collectors.toList());
        int pageSize = 10;
        int from = (page - 1) * pageSize;
        if (from >= defs.size()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.uniqueoffer.list_empty"), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.uniqueoffer.list_header", page), false);
        MinecraftServer server = ctx.getSource().getServer();
        int to = Math.min(from + pageSize, defs.size());
        for (int i = from; i < to; i++) {
            GtsUniqueOfferDefinition def = defs.get(i);
            String givenSpecies = speciesNameFromGivenLine(def.getGivenLine());
            String ot = otPreviewFromGivenLine(def);
            int markCount = def.getGivenMarkIds().size();
            String wishSpecies = wishSpeciesNameFromLine(def.getWishSpeciesLine());
            String lvl = levelLabel(def.getWishLevelBucket());
            String gen = def.getWishGender().name().toLowerCase(Locale.ROOT);
            String shiny = def.getWishShiny().name().toLowerCase(Locale.ROOT);
            final String lineId = def.getOfferId();
            final String gs = givenSpecies;
            final String otName = ot;
            final int mc = markCount;
            final String ws = wishSpecies;
            final String lv = lvl;
            final String g = gen;
            final String sh = shiny;
            ctx.getSource()
                    .sendSuccess(
                            () -> Component.translatable(
                                    "cobblesafari.command.gts.uniqueoffer.list_line",
                                    lineId,
                                    gs,
                                    otName,
                                    mc,
                                    ws,
                                    lv,
                                    g,
                                    sh),
                            false);
        }
        return 1;
    }

    private static int uniqueOfferDetails(CommandContext<CommandSourceStack> ctx) {
        String templateId = StringArgumentType.getString(ctx, ARG_OFFER_ID);
        Optional<GtsUniqueOfferDefinition> opt = GtsUniqueOfferRegistry.get(templateId);
        if (opt.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.details_not_found"));
            return 0;
        }
        GtsUniqueOfferDefinition def = opt.get();
        MinecraftServer server = ctx.getSource().getServer();
        ctx.getSource()
                .sendSuccess(
                        () -> Component.translatable("cobblesafari.command.gts.uniqueoffer.details_header", templateId),
                        false);
        if (!def.getSourcePath().isEmpty()) {
            ctx.getSource()
                    .sendSuccess(
                            () -> Component.translatable(
                                    "cobblesafari.command.gts.uniqueoffer.details_source", def.getSourcePath()),
                            false);
        }
        ctx.getSource()
                .sendSuccess(
                        () -> Component.translatable(
                                "cobblesafari.command.gts.uniqueoffer.details_given_line", def.getGivenLine()),
                        false);
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.details_section_deposited"), false);
        try {
            Pokemon given = GtsService.createGivenPokemon(def);
            CompoundTag nbt = given.saveToNBT(server.registryAccess(), new CompoundTag());
            sendPokemonDetails(ctx, nbt, null, -1);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.details_error"));
            return 0;
        }
        GtsOffer wishPreview =
                new GtsOffer(
                        0,
                        GtsService.UNIQUE_OFFER_DEPOSITOR_UUID,
                        new CompoundTag(),
                        def.getWishSpeciesLine(),
                        def.getWishLevelBucket(),
                        def.getWishGender(),
                        def.getWishShiny(),
                        "",
                        com.cobblemon.mod.common.pokemon.Gender.GENDERLESS,
                        false);
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.details_section_requested"), false);
        sendRequestedWishSummary(ctx, wishPreview);
        return 1;
    }

    private static int personalAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_PLAYER);
        String token = StringArgumentType.getString(ctx, ARG_OFFER_ID).trim();
        MinecraftServer server = ctx.getSource().getServer();
        GtsService.AddPersonalOfferOutcome outcome;
        String tagForError = null;
        if (token.toLowerCase(Locale.ROOT).startsWith(GtsService.TAG_PREFIX)) {
            tagForError = token.substring(GtsService.TAG_PREFIX.length()).trim();
            outcome = GtsService.addPersonalOfferByTag(server, target.getUUID(), tagForError);
        } else {
            outcome = GtsService.addPersonalOffer(server, target.getUUID(), token);
        }
        final String resolvedTemplateId =
                outcome.result() == GtsService.AddPersonalOfferResult.SUCCESS
                        ? GtsSavedData.get(server)
                                .findOffer(outcome.runtimeOfferId())
                                .map(GtsOffer::getUniqueOfferTemplateId)
                                .orElse(token)
                        : token;
        return switch (outcome.result()) {
            case SUCCESS -> {
                ctx.getSource()
                        .sendSuccess(
                                () -> Component.translatable(
                                        "cobblesafari.command.gts.uniqueoffer.personal.add_success",
                                        outcome.runtimeOfferId(),
                                        resolvedTemplateId,
                                        target.getName().getString()),
                                true);
                yield 1;
            }
            case ALREADY_HAS_PERSONAL -> {
                ctx.getSource()
                        .sendFailure(
                                Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.add_already_has"));
                yield 0;
            }
            case UNKNOWN_OFFER_ID -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.add_unknown"));
                yield 0;
            }
            case NO_TAG_MATCH -> {
                ctx.getSource()
                        .sendFailure(
                                Component.translatable(
                                        "cobblesafari.command.gts.uniqueoffer.personal.add_no_tag_match",
                                        tagForError));
                yield 0;
            }
            case INVALID_GIVEN -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.add_invalid_given"));
                yield 0;
            }
            case BANNED_GIVEN -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.add_banned_given"));
                yield 0;
            }
            case BANNED_WISH -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.add_banned_wish"));
                yield 0;
            }
            case INCOMPATIBLE_GENDER -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.add_incompatible_gender"));
                yield 0;
            }
            case INVALID_LEVEL_BUCKET -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.add_invalid_level"));
                yield 0;
            }
            case ERROR -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.add_error"));
                yield 0;
            }
        };
    }

    private static int personalList(CommandContext<CommandSourceStack> ctx, ServerPlayer filterPlayer, int page) {
        MinecraftServer server = ctx.getSource().getServer();
        GtsSavedData data = GtsSavedData.get(server);
        List<GtsOffer> personal =
                data.getOffers().stream()
                        .filter(GtsOffer::isPersonalOffer)
                        .filter(o -> filterPlayer == null || filterPlayer.getUUID().equals(o.getPersonalTargetUuid()))
                        .sorted(Comparator.comparingInt(GtsOffer::getId))
                        .collect(Collectors.toList());
        int pageSize = 10;
        int from = (page - 1) * pageSize;
        if (from >= personal.size()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.list_empty"), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.list_header", page), false);
        int to = Math.min(from + pageSize, personal.size());
        for (int i = from; i < to; i++) {
            GtsOffer o = personal.get(i);
            String targetName =
                    GtsService.lookupUsername(server, o.getPersonalTargetUuid())
                            .orElse(o.getPersonalTargetUuid().toString());
            String given = speciesNameFromOffer(server, o);
            String wish = o.getWishSpecies();
            String lvl = levelLabel(o.getWishLevelBucket());
            String gen = o.getWishGender().name().toLowerCase(Locale.ROOT);
            final int fid = o.getId();
            final String fTarget = targetName;
            final String fTemplate = o.getUniqueOfferTemplateId();
            final String fGiven = given;
            final String fWish = wish;
            final String fLvl = lvl;
            final String fGen = gen;
            ctx.getSource()
                    .sendSuccess(
                            () -> Component.translatable(
                                    "cobblesafari.command.gts.uniqueoffer.personal.list_line",
                                    fid,
                                    fTarget,
                                    fTemplate,
                                    fGiven,
                                    fWish,
                                    fLvl,
                                    fGen),
                            false);
        }
        return 1;
    }

    private static int personalRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_PLAYER);
        String templateId = StringArgumentType.getString(ctx, ARG_OFFER_ID);
        GtsService.RemovePersonalOfferResult r =
                GtsService.removePersonalOffer(ctx.getSource().getServer(), target.getUUID(), templateId);
        return switch (r) {
            case SUCCESS -> {
                ctx.getSource()
                        .sendSuccess(
                                () -> Component.translatable(
                                        "cobblesafari.command.gts.uniqueoffer.personal.remove_success",
                                        templateId,
                                        target.getName().getString()),
                                true);
                yield 1;
            }
            case NOT_FOUND -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.remove_not_found"));
                yield 0;
            }
            case LOCKED -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.uniqueoffer.personal.remove_locked"));
                yield 0;
            }
        };
    }

    private static String speciesNameFromGivenLine(String givenLine) {
        try {
            PokemonProperties props = PokemonProperties.Companion.parse(givenLine);
            if (props.getSpecies() == null) {
                return "?";
            }
            Pokemon p = props.create(null);
            return p.getSpecies().getName();
        } catch (Exception e) {
            return "?";
        }
    }

    private static String otPreviewFromGivenLine(GtsUniqueOfferDefinition def) {
        try {
            Pokemon p = GtsService.createGivenPokemon(def);
            String ot = p.getOriginalTrainerName();
            return ot != null && !ot.isEmpty() ? ot : "?";
        } catch (Exception e) {
            return "?";
        }
    }

    private static String wishSpeciesNameFromLine(String wishLine) {
        try {
            PokemonProperties props = PokemonProperties.Companion.parse(wishLine);
            if (props.getSpecies() == null) {
                return "?";
            }
            Pokemon p = props.create(null);
            return p.getSpecies().getName();
        } catch (Exception e) {
            return "?";
        }
    }

    private static int adminRemove(CommandContext<CommandSourceStack> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, ARG_ID);
        boolean unique =
                GtsSavedData.get(ctx.getSource().getServer()).findOffer(id).map(GtsOffer::isUniqueOffer).orElse(false);
        GtsService.AdminRemoveResult r = GtsService.adminRemoveOffer(ctx.getSource().getServer(), id);
        return switch (r) {
            case SUCCESS -> {
                if (unique) {
                    ctx.getSource()
                            .sendSuccess(
                                    () -> Component.translatable("cobblesafari.command.gts.remove_success_unique", id),
                                    true);
                } else {
                    ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.remove_success", id), true);
                }
                yield 1;
            }
            case NOT_FOUND -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.remove_not_found"));
                yield 0;
            }
            case LOCKED -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.remove_locked"));
                yield 0;
            }
        };
    }

    private static int testDeposit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_PLAYER);
        int opSlot = IntegerArgumentType.getInteger(ctx, ARG_SLOT);
        int internal = opSlot - 1;
        String species = StringArgumentType.getString(ctx, ARG_SPECIES);
        int bucket = parseLevelBucketToken(StringArgumentType.getString(ctx, ARG_LEVEL_BUCKET));
        GenderFilter gf = GenderFilter.parse(StringArgumentType.getString(ctx, ARG_GENDER));
        GtsOffer.ShinyWish shiny = GtsOffer.ShinyWish.parse(StringArgumentType.getString(ctx, ARG_SHINY));
        GtsService.DepositResult r = GtsService.tryDeposit(target, internal, species, bucket, gf, shiny);
        return switch (r) {
            case SUCCESS -> {
                ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.deposit_success", target.getName().getString(), opSlot), true);
                yield 1;
            }
            case EMPTY_SLOT -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.empty_slot", opSlot));
                yield 0;
            }
            case BANNED_DEPOSIT -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.banned_deposit"));
                yield 0;
            }
            case UNKNOWN_WISH_SPECIES -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.unknown_wish_species"));
                yield 0;
            }
            case BANNED_WISH -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.banned_wish"));
                yield 0;
            }
            case INVALID_LEVEL_BUCKET -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.invalid_level_bucket"));
                yield 0;
            }
            case INCOMPATIBLE_GENDER -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.incompatible_gender"));
                yield 0;
            }
            case ALREADY_HAS_OFFER -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.already_has_offer"));
                yield 0;
            }
            case ERROR -> {
                ctx.getSource().sendFailure(Component.translatable(MSG_GTS_ERROR));
                yield 0;
            }
        };
    }

    private static int testTrade(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_PLAYER);
        int id = IntegerArgumentType.getInteger(ctx, ARG_ID);
        GtsService.StartTradeResult res = GtsService.tryStartTrade(target, id);
        return switch (res.kind()) {
            case OK -> {
                ctx.getSource()
                        .sendSuccess(
                                () -> Component.translatable("cobblesafari.command.gts.candidate_header", res.candidates().size()),
                                false);
                int i = 1;
                for (GtsTradeCandidate c : res.candidates()) {
                    final int idx = i++;
                    Pokemon p = resolveCandidatePokemon(target, c);
                    if (p == null) {
                        continue;
                    }
                    final String src = c.source() == GtsTradeCandidate.CandidateSource.PARTY ? "party" : "pc";
                    final int fs = c.source() == GtsTradeCandidate.CandidateSource.PARTY ? c.partySlot() : c.pcBox() * 100 + c.pcSlot();
                    final String name = p.getSpecies().getName();
                    final int lvl = p.getLevel();
                    final String gen = p.getGender().name();
                    ctx.getSource()
                            .sendSuccess(
                                    () -> Component.translatable(
                                            "cobblesafari.command.gts.candidate_line", idx, src, fs, name, lvl, gen),
                                    false);
                }
                yield 1;
            }
            case OFFER_NOT_FOUND -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.offer_not_found"));
                yield 0;
            }
            case OFFER_LOCKED -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.offer_locked"));
                yield 0;
            }
            case OFFER_OWN -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.offer_own"));
                yield 0;
            }
            case NO_MATCHING_POKEMON -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.no_matching_pokemon"));
                yield 0;
            }
            case ERROR -> {
                ctx.getSource().sendFailure(Component.translatable(MSG_GTS_ERROR));
                yield 0;
            }
        };
    }

    private static Pokemon resolveCandidatePokemon(ServerPlayer player, GtsTradeCandidate c) {
        return switch (c.source()) {
            case PARTY -> com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player).get(c.partySlot());
            case PC -> {
                var pc = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getPC(player);
                var boxes = pc.getBoxes();
                if (c.pcBox() < 0 || c.pcBox() >= boxes.size()) {
                    yield null;
                }
                yield boxes.get(c.pcBox()).get(c.pcSlot());
            }
        };
    }

    private static int testTradeConfirm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_PLAYER);
        int id = IntegerArgumentType.getInteger(ctx, ARG_ID);
        int pick = IntegerArgumentType.getInteger(ctx, ARG_PICK);
        GtsService.ConfirmTradeKind k = GtsService.tryConfirmTrade(target, id, pick);
        return switch (k) {
            case SUCCESS -> {
                ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.trade_success", target.getName().getString()), true);
                yield 1;
            }
            case SELECTION_EXPIRED -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.selection_expired"));
                yield 0;
            }
            case CANDIDATE_GONE -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.candidate_gone"));
                yield 0;
            }
            case ERROR -> {
                ctx.getSource().sendFailure(Component.translatable(MSG_GTS_ERROR));
                yield 0;
            }
        };
    }

    private static int testRetrieve(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_PLAYER);
        int oid = IntegerArgumentType.getInteger(ctx, ARG_OFFER_ID);
        GtsService.RetrieveResult r = GtsService.tryRetrieveOwnOffer(target, oid);
        return switch (r) {
            case SUCCESS -> {
                ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.retrieve_success", oid), true);
                yield 1;
            }
            case NOT_FOUND -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.retrieve_not_found"));
                yield 0;
            }
            case NOT_OWNER -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.retrieve_not_owner"));
                yield 0;
            }
            case LOCKED -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.retrieve_locked"));
                yield 0;
            }
            case INVENTORY_FULL -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.retrieve_inventory_full"));
                yield 0;
            }
            case ERROR -> {
                ctx.getSource().sendFailure(Component.translatable(MSG_GTS_ERROR));
                yield 0;
            }
        };
    }

    private static int testClaim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, ARG_PLAYER);
        int sid = IntegerArgumentType.getInteger(ctx, ARG_SUCCESS_ID);
        GtsService.ClaimResult r = GtsService.tryClaimSuccess(target, sid);
        return switch (r) {
            case SUCCESS -> {
                ctx.getSource().sendSuccess(() -> Component.translatable("cobblesafari.command.gts.claim_success", sid), true);
                yield 1;
            }
            case NOT_FOUND -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.claim_not_found"));
                yield 0;
            }
            case NOT_RECIPIENT -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.claim_not_recipient"));
                yield 0;
            }
            case INVENTORY_FULL -> {
                ctx.getSource().sendFailure(Component.translatable("cobblesafari.command.gts.claim_inventory_full"));
                yield 0;
            }
            case ERROR -> {
                ctx.getSource().sendFailure(Component.translatable(MSG_GTS_ERROR));
                yield 0;
            }
        };
    }

    private static String speciesNameFromOffer(MinecraftServer server, GtsOffer o) {
        return speciesNameFromNbt(server, o.getPokemonData());
    }

    private static String speciesNameFromNbt(MinecraftServer server, net.minecraft.nbt.CompoundTag nbt) {
        try {
            Pokemon p = Pokemon.Companion.loadFromNBT(server.registryAccess(), nbt.copy());
            return p.getSpecies().getName();
        } catch (Exception e) {
            return "?";
        }
    }

    private static String levelLabel(int bucket) {
        if (bucket < 0) {
            return "any";
        }
        int lo = bucket * 10 + 1;
        int hi = Math.min(100, (bucket + 1) * 10);
        return lo + "-" + hi;
    }
}
