package maxigregrze.cobblesafari.network;

import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.data.GtsSavedData;
import maxigregrze.cobblesafari.gts.GenderFilter;
import maxigregrze.cobblesafari.gts.GtsOffer;
import maxigregrze.cobblesafari.gts.GtsService;
import maxigregrze.cobblesafari.gts.GtsSuccess;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.security.RateLimiter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GtsAppServerHandler {

    /** Minimum gap between duplicate mutation payloads (guards against double-submit). */
    private static final long MUTATION_DEBOUNCE_MS = 2_000L;
    private static final Map<UUID, Long> LAST_DEPOSIT_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_RETRIEVE_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_CLAIM_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_CONFIRM_TRADE_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_START_TRADE_MS = new ConcurrentHashMap<>();

    private GtsAppServerHandler() {}

    /** Releases per-player debounce ledger entries; call on disconnect to avoid unbounded growth. */
    public static void clear(UUID playerId) {
        LAST_DEPOSIT_MS.remove(playerId);
        LAST_RETRIEVE_MS.remove(playerId);
        LAST_CLAIM_MS.remove(playerId);
        LAST_CONFIRM_TRADE_MS.remove(playerId);
        LAST_START_TRADE_MS.remove(playerId);
    }

    private static boolean registerMutationAttempt(Map<UUID, Long> ledger, UUID playerId) {
        long now = System.currentTimeMillis();
        Long last = ledger.get(playerId);
        if (last != null && now - last < MUTATION_DEBOUNCE_MS) {
            return false;
        }
        ledger.put(playerId, now);
        return true;
    }

    private static void clearMutationAttempt(Map<UUID, Long> ledger, UUID playerId) {
        ledger.remove(playerId);
    }

    private static boolean checkReadRateLimit(UUID playerId, int action) {
        long gap = switch (action) {
            case GtsAppPayload.ACTION_REQUEST_STATE -> 250L;
            case GtsAppPayload.ACTION_VALIDATE_SPECIES -> 500L;
            case GtsAppPayload.ACTION_SEARCH -> 500L;
            case GtsAppPayload.ACTION_START_TRADE -> 1_000L;
            default -> 0L;
        };
        if (gap == 0L) {
            return true;
        }
        return RateLimiter.allow(playerId, RateLimiter.key(RateLimiter.SERVICE_GTS, action), gap);
    }

    public static void handle(ServerPlayer player, GtsAppPayload payload) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (!checkReadRateLimit(player.getUUID(), payload.actionType())) {
            return;
        }
        switch (payload.actionType()) {
            case GtsAppPayload.ACTION_REQUEST_STATE -> sendBegin(player, "");
            case GtsAppPayload.ACTION_VALIDATE_SPECIES -> doValidate(player, payload);
            case GtsAppPayload.ACTION_DEPOSIT -> doDeposit(player, payload);
            case GtsAppPayload.ACTION_RETRIEVE -> doRetrieve(player, payload);
            case GtsAppPayload.ACTION_CLAIM -> doClaim(player, payload);
            case GtsAppPayload.ACTION_SEARCH -> doSearch(player, payload);
            case GtsAppPayload.ACTION_START_TRADE -> doStartTrade(player, payload);
            case GtsAppPayload.ACTION_CONFIRM_TRADE -> doConfirmTrade(player, payload);
            case GtsAppPayload.ACTION_ABORT_TRADE -> doAbortTrade(player);
            default -> {
            }
        }
    }

    private static void doValidate(ServerPlayer player, GtsAppPayload pl) {
        GenderFilter gf = GenderFilter.parse(pl.stringArg2());
        GtsService.ValidateSpeciesResult r =
                GtsService.validateWishSpecies(pl.stringArg1(), gf);
        send(
                player,
                new GtsAppResultPayload(
                        GtsAppResultPayload.SUB_VALIDATE_RESULT,
                        0,
                        -1,
                        0,
                        -1,
                        r.name(),
                        "",
                        1,
                        1,
                        List.of(),
                        "",
                        List.of(),
                        new CompoundTag(),
                        new CompoundTag(),
                        ""));
    }

    private static void doDeposit(ServerPlayer player, GtsAppPayload pl) {
        int slot = pl.intArg1();
        if (slot < 0 || slot > 5) {
            sendBegin(player, "gui.cobblesafari.rotomphone.gts.error.empty_slot");
            return;
        }
        if (!registerMutationAttempt(LAST_DEPOSIT_MS, player.getUUID())) {
            return;
        }
        GenderFilter gf = GenderFilter.parse(pl.stringArg2());
        GtsOffer.ShinyWish shiny = GtsOffer.ShinyWish.parse(pl.stringArg3());
        GtsService.DepositResult r =
                GtsService.tryDeposit(player, slot, pl.stringArg1(), pl.intArg2(), gf, shiny);
        if (r != GtsService.DepositResult.SUCCESS) {
            clearMutationAttempt(LAST_DEPOSIT_MS, player.getUUID());
        }
        if (r == GtsService.DepositResult.SUCCESS) {
            GtsSavedData data = GtsSavedData.get(player.getServer());
            int ownId = resolveOwnOfferId(data, player.getUUID(), -1);
            CompoundTag offered = new CompoundTag();
            data.findOffer(ownId).ifPresent(o -> offered.merge(o.getPokemonData().copy()));
            send(
                    player,
                    new GtsAppResultPayload(
                            GtsAppResultPayload.SUB_DEPOSIT,
                            data.getOffers().size(),
                            ownId,
                            0,
                            -1,
                            "",
                            "SUCCESS",
                            1,
                            1,
                            List.of(),
                            "",
                            List.of(),
                            offered,
                            new CompoundTag(),
                            ""));
        } else {
            sendBegin(player, depositErrorKey(r));
        }
    }

    private static String depositErrorKey(GtsService.DepositResult r) {
        return switch (r) {
            case EMPTY_SLOT -> "gui.cobblesafari.rotomphone.gts.error.empty_slot";
            case BANNED_DEPOSIT -> "gui.cobblesafari.rotomphone.gts.error.banned_deposit";
            case UNKNOWN_WISH_SPECIES -> "gui.cobblesafari.rotomphone.gts.error.unknown_wish_species";
            case BANNED_WISH -> "gui.cobblesafari.rotomphone.gts.error.banned";
            case INVALID_LEVEL_BUCKET -> "gui.cobblesafari.rotomphone.gts.error.invalid_level_bucket";
            case INCOMPATIBLE_GENDER -> "gui.cobblesafari.rotomphone.gts.error.incompatible_gender";
            case ALREADY_HAS_OFFER -> "gui.cobblesafari.rotomphone.gts.error.already_has_offer";
            default -> "gui.cobblesafari.rotomphone.gts.error.error";
        };
    }

    private static void doRetrieve(ServerPlayer player, GtsAppPayload pl) {
        if (!registerMutationAttempt(LAST_RETRIEVE_MS, player.getUUID())) {
            return;
        }
        int preferredId = pl.intArg1();
        GtsSavedData data = GtsSavedData.get(player.getServer());

        if (preferredId >= 0) {
            Optional<GtsOffer> opt = data.findOffer(preferredId);
            if (opt.isEmpty() || !opt.get().getDepositorUuid().equals(player.getUUID())) {
                clearMutationAttempt(LAST_RETRIEVE_MS, player.getUUID());
                sendBegin(player, "gui.cobblesafari.rotomphone.gts.error.offer_not_found");
                return;
            }
        }

        int offerId = resolveOwnOfferId(data, player.getUUID(), preferredId);
        CompoundTag pokemonNbt = new CompoundTag();
        data.findOffer(offerId).ifPresent(o -> pokemonNbt.merge(o.getPokemonData().copy()));
        GtsService.RetrieveResult r = GtsService.tryRetrieveOwnOffer(player, offerId);
        if (r != GtsService.RetrieveResult.SUCCESS) {
            clearMutationAttempt(LAST_RETRIEVE_MS, player.getUUID());
        }
        if (r == GtsService.RetrieveResult.SUCCESS) {
            send(
                    player,
                    new GtsAppResultPayload(
                            GtsAppResultPayload.SUB_RETRIEVAL,
                            data.getOffers().size(),
                            -1,
                            0,
                            -1,
                            "",
                            "SUCCESS",
                            1,
                            1,
                            List.of(),
                            "",
                            List.of(),
                            pokemonNbt,
                            new CompoundTag(),
                            ""));
        } else {
            sendBegin(player, retrieveErrorKey(r));
        }
    }

    private static String retrieveErrorKey(GtsService.RetrieveResult r) {
        return switch (r) {
            case NOT_FOUND -> "gui.cobblesafari.rotomphone.gts.error.offer_not_found";
            case NOT_OWNER -> "gui.cobblesafari.rotomphone.gts.error.not_owner";
            case LOCKED -> "gui.cobblesafari.rotomphone.gts.error.offer_locked";
            case INVENTORY_FULL -> "gui.cobblesafari.rotomphone.gts.error.inventory_full";
            default -> "gui.cobblesafari.rotomphone.gts.error.error";
        };
    }

    private static void doClaim(ServerPlayer player, GtsAppPayload pl) {
        if (!registerMutationAttempt(LAST_CLAIM_MS, player.getUUID())) {
            return;
        }
        int successId = pl.intArg1();
        GtsSavedData data = GtsSavedData.get(player.getServer());
        Optional<GtsSuccess> opt = data.findSuccess(successId);
        CompoundTag pokemonNbt = new CompoundTag();
        if (opt.isPresent()) {
            pokemonNbt = opt.get().getPokemonData().copy();
        }
        GtsService.ClaimResult r = GtsService.tryClaimSuccess(player, successId);
        if (r != GtsService.ClaimResult.SUCCESS) {
            clearMutationAttempt(LAST_CLAIM_MS, player.getUUID());
        }
        if (r == GtsService.ClaimResult.SUCCESS) {
            send(
                    player,
                    new GtsAppResultPayload(
                            GtsAppResultPayload.SUB_RECEIVE,
                            0,
                            -1,
                            0,
                            -1,
                            "",
                            "SUCCESS",
                            1,
                            1,
                            List.of(),
                            "",
                            List.of(),
                            new CompoundTag(),
                            pokemonNbt,
                            ""));
        } else {
            sendBegin(player, claimErrorKey(r));
        }
    }

    private static String claimErrorKey(GtsService.ClaimResult r) {
        return switch (r) {
            case NOT_FOUND -> "gui.cobblesafari.rotomphone.gts.error.offer_not_found";
            case NOT_RECIPIENT -> "gui.cobblesafari.rotomphone.gts.error.not_recipient";
            case INVENTORY_FULL -> "gui.cobblesafari.rotomphone.gts.error.inventory_full";
            default -> "gui.cobblesafari.rotomphone.gts.error.error";
        };
    }

    private static void doSearch(ServerPlayer player, GtsAppPayload pl) {
        int page = Math.max(1, pl.intArg1());
        GenderFilter gf = GenderFilter.parse(pl.stringArg2());
        GtsOffer.ShinyWish shiny = GtsOffer.ShinyWish.parse(pl.stringArg3());
        GtsService.SearchResult result =
                GtsService.searchOffersForPlayer(player, page, pl.stringArg1(), gf, shiny);
        List<GtsAppResultPayload.SearchEntry> entries = new ArrayList<>();
        for (GtsOffer offer : result.offers()) {
            entries.add(
                    new GtsAppResultPayload.SearchEntry(
                            offer.getId(),
                            offer.getPokemonData().copy(),
                            offer.getWishSpecies(),
                            offer.getWishLevelBucket(),
                            offer.getWishGender().name(),
                            offer.getWishShiny().name()));
        }
        send(
                player,
                new GtsAppResultPayload(
                        GtsAppResultPayload.SUB_SEARCH_RESULT,
                        0,
                        -1,
                        0,
                        -1,
                        "",
                        "",
                        result.page(),
                        result.totalPages(),
                        entries,
                        "",
                        List.of(),
                        new CompoundTag(),
                        new CompoundTag(),
                        ""));
    }

    private static void doAbortTrade(ServerPlayer player) {
        GtsService.releasePendingFor(player);
    }

    private static void doStartTrade(ServerPlayer player, GtsAppPayload pl) {
        if (!registerMutationAttempt(LAST_START_TRADE_MS, player.getUUID())) {
            return;
        }
        int offerId = pl.intArg1();
        GtsService.StartTradeCandidateBundle bundle = GtsService.tryStartTradeWithViews(player, offerId);
        if (bundle.kind() != GtsService.StartTradeKind.OK) {
            clearMutationAttempt(LAST_START_TRADE_MS, player.getUUID());
            sendBegin(player, startTradeErrorKey(bundle.kind()));
            return;
        }
        List<CompoundTag> nbts = new ArrayList<>();
        for (GtsService.CandidateView v : bundle.views()) {
            nbts.add(v.nbt().copy());
        }
        send(
                player,
                new GtsAppResultPayload(
                        GtsAppResultPayload.SUB_START_TRADE_RESULT,
                        0,
                        -1,
                        0,
                        -1,
                        "",
                        "",
                        1,
                        1,
                        List.of(),
                        "OK",
                        nbts,
                        new CompoundTag(),
                        new CompoundTag(),
                        ""));
    }

    private static String startTradeErrorKey(GtsService.StartTradeKind k) {
        return switch (k) {
            case OFFER_NOT_FOUND -> "gui.cobblesafari.rotomphone.gts.error.offer_not_found";
            case OFFER_LOCKED -> "gui.cobblesafari.rotomphone.gts.error.offer_locked";
            case OFFER_OWN -> "gui.cobblesafari.rotomphone.gts.error.offer_own";
            case NO_MATCHING_POKEMON -> "gui.cobblesafari.rotomphone.gts.error.no_matching_pokemon";
            default -> "gui.cobblesafari.rotomphone.gts.error.error";
        };
    }

    private static void doConfirmTrade(ServerPlayer player, GtsAppPayload pl) {
        if (!registerMutationAttempt(LAST_CONFIRM_TRADE_MS, player.getUUID())) {
            return;
        }
        int offerId = pl.intArg1();
        int pick = pl.intArg2();
        GtsSavedData data = GtsSavedData.get(player.getServer());
        Optional<GtsOffer> offerOpt = data.findOffer(offerId);
        CompoundTag offeredNbt = new CompoundTag();
        if (offerOpt.isPresent()) {
            offeredNbt = offerOpt.get().getPokemonData().copy();
        }
        CompoundTag givenNbt =
                GtsService.peekPendingCandidateNbt(player, offerId, pick).orElse(new CompoundTag());
        GtsService.ConfirmTradeKind r = GtsService.tryConfirmTrade(player, offerId, pick);
        if (r != GtsService.ConfirmTradeKind.SUCCESS) {
            clearMutationAttempt(LAST_CONFIRM_TRADE_MS, player.getUUID());
        }
        if (r == GtsService.ConfirmTradeKind.SUCCESS) {
            send(
                    player,
                    new GtsAppResultPayload(
                            GtsAppResultPayload.SUB_TRADE,
                            0,
                            -1,
                            0,
                            -1,
                            "",
                            "SUCCESS",
                            1,
                            1,
                            List.of(),
                            "",
                            List.of(),
                            givenNbt,
                            offeredNbt,
                            ""));
        } else {
            sendBegin(player, confirmErrorKey(r));
        }
    }

    private static String confirmErrorKey(GtsService.ConfirmTradeKind r) {
        return switch (r) {
            case SELECTION_EXPIRED -> "gui.cobblesafari.rotomphone.gts.error.selection_expired";
            case CANDIDATE_GONE -> "gui.cobblesafari.rotomphone.gts.error.candidate_gone";
            default -> "gui.cobblesafari.rotomphone.gts.error.error";
        };
    }

    private static void sendBegin(ServerPlayer player, String errorKey) {
        GtsSavedData data = GtsSavedData.get(player.getServer());
        int total =
                (int)
                        data.getOffers().stream()
                                .filter(
                                        o ->
                                                !o.isPersonalOffer()
                                                        || player.getUUID().equals(o.getPersonalTargetUuid()))
                                .count();
        int ownId = resolveOwnOfferId(data, player.getUUID(), -1);
        List<GtsSuccess> waiting = data.findSuccessesByRecipient(player.getUUID());
        int sCount = waiting.size();
        int oldestSid = waiting.isEmpty() ? -1 : waiting.get(0).getId();
        send(
                player,
                new GtsAppResultPayload(
                        errorKey == null || errorKey.isEmpty()
                                ? GtsAppResultPayload.SUB_BEGIN
                                : GtsAppResultPayload.SUB_ERROR,
                        total,
                        ownId,
                        sCount,
                        oldestSid,
                        "",
                        "",
                        1,
                        1,
                        List.of(),
                        "",
                        List.of(),
                        new CompoundTag(),
                        new CompoundTag(),
                        errorKey == null ? "" : errorKey));
    }

    private static void send(ServerPlayer player, GtsAppResultPayload payload) {
        Services.PLATFORM.sendPayloadToPlayer(player, payload);
    }

    private static int resolveOwnOfferId(GtsSavedData data, UUID depositor, int preferredId) {
        if (preferredId >= 0) {
            Optional<GtsOffer> opt = data.findOffer(preferredId);
            if (opt.isPresent() && opt.get().getDepositorUuid().equals(depositor)) {
                return preferredId;
            }
        }
        return data.findOffersByDepositor(depositor).stream()
                .max(Comparator.comparingInt(GtsOffer::getId))
                .map(GtsOffer::getId)
                .orElse(-1);
    }
}
