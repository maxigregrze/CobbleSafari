package maxigregrze.cobblesafari.gts;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.mark.Mark;
import com.cobblemon.mod.common.api.mark.Marks;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.pc.PCBox;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.GtsSettings;
import maxigregrze.cobblesafari.config.WonderTradeSettings;
import maxigregrze.cobblesafari.data.GtsSavedData;
import maxigregrze.cobblesafari.security.TradeNbtSanitizer;
import maxigregrze.cobblesafari.security.WishLineValidator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GtsService {
    public static final UUID UNIQUE_OFFER_DEPOSITOR_UUID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final long SELECTION_TTL_MS = 5L * 60L * 1000L;
    private static final int PC_SLOTS_PER_BOX = 30;
    private static final int MAX_LOCKS_PER_PLAYER = 1;

    private static final Map<UUID, GtsPendingTrade> PENDING_BY_PLAYER = new ConcurrentHashMap<>();

    private GtsService() {}

    public static void onServerStarted(MinecraftServer server) {
        GtsSettings.load();
        GtsDataLoader.load(server);
        GtsSavedData data = GtsSavedData.get(server);
        if (data.getLastDailyResetEpochDay() < 0) {
            data.setLastDailyResetEpochDay(LocalDate.now(ZONE).minusDays(1).toEpochDay());
        }
    }

    public static void tickDailyScheduler(MinecraftServer server) {
        GtsSavedData data = GtsSavedData.get(server);
        LocalDate today = LocalDate.now(ZONE);
        LocalTime now = LocalTime.now(ZONE);
        long todayEpoch = today.toEpochDay();
        long last = data.getLastDailyResetEpochDay();
        int rh = WonderTradeSettings.get().getResetHour();
        if (last < 0) {
            return;
        }
        boolean pastResetHour = now.getHour() >= rh;
        boolean shouldRun = (todayEpoch > last && pastResetHour) || (todayEpoch > last + 1);
        if (!shouldRun) {
            return;
        }
        runDailyReset(server);
        data.setLastDailyResetEpochDay(todayEpoch);
    }

    public static void runDailyReset(MinecraftServer server) {
        GtsSettings cfg = GtsSettings.get();
        GtsSavedData data = GtsSavedData.get(server);
        int best = cfg.getPokemonBestBefore();
        for (GtsOffer offer : new ArrayList<>(data.getOffers())) {
            if (offer.isPersonalOffer()) {
                continue;
            }
            offer.setAge(offer.getAge() + 1);
        }
        for (GtsOffer offer : new ArrayList<>(data.getOffers())) {
            if (offer.isPersonalOffer()) {
                continue;
            }
            if (offer.getAge() > best) {
                if (offer.isUniqueOffer()) {
                    data.removeOffer(offer.getId());
                } else {
                    int sid = data.allocateSuccessId();
                    data.addSuccess(
                            new GtsSuccess(
                                    sid,
                                    offer.getDepositorUuid(),
                                    offer.getPokemonData().copy(),
                                    GtsSuccess.Reason.EXPIRED));
                    data.removeOffer(offer.getId());
                }
            }
        }
        data.setDirty();
    }

    public static void tickGcLocks(MinecraftServer server) {
        long now = System.currentTimeMillis();
        GtsSavedData data = GtsSavedData.get(server);
        for (GtsOffer offer : data.getOffers()) {
            if (offer.isLocked() && offer.getLockExpireEpochMs() > 0L && now > offer.getLockExpireEpochMs()) {
                offer.clearLock();
                data.setDirty();
            }
        }
        for (Map.Entry<UUID, GtsPendingTrade> e : new ArrayList<>(PENDING_BY_PLAYER.entrySet())) {
            if (e.getValue().isExpired()) {
                UUID playerId = e.getKey();
                GtsPendingTrade pending = e.getValue();
                PENDING_BY_PLAYER.remove(playerId);
                data.findOffer(pending.getOfferId()).ifPresent(o -> {
                    if (o.isLocked() && playerId.equals(o.getLockOwnerUuid())) {
                        o.clearLock();
                        data.setDirty();
                    }
                });
            }
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        GtsSettings cfg = GtsSettings.get();
        GtsSavedData data = GtsSavedData.get(player.getServer());
        List<GtsSuccess> waiting = data.findSuccessesByRecipient(player.getUUID());
        if (waiting.isEmpty()) {
            return;
        }
        if (cfg.isTradeNotification()) {
            for (GtsSuccess s : waiting) {
                if (s.isNotified()) {
                    continue;
                }
                String key =
                        switch (s.getReason()) {
                            case TRADED -> "cobblesafari.gts.notify.trade_completed";
                            case EXPIRED -> "cobblesafari.gts.notify.trade_expired";
                            case ADMIN_REMOVED -> "cobblesafari.gts.notify.trade_admin_removed";
                        };
                player.sendSystemMessage(Component.translatable(key, s.getId()));
                s.setNotified(true);
            }
            data.setDirty();
        }
        if (cfg.isTradeReminder()) {
            player.sendSystemMessage(Component.translatable("cobblesafari.gts.notify.reminder", waiting.size()));
        }
    }

    public enum DepositResult {
        SUCCESS,
        EMPTY_SLOT,
        BANNED_DEPOSIT,
        UNKNOWN_WISH_SPECIES,
        BANNED_WISH,
        INVALID_LEVEL_BUCKET,
        INCOMPATIBLE_GENDER,
        ALREADY_HAS_OFFER,
        ERROR
    }

    public enum ValidateSpeciesResult {
        OK,
        INVALID,
        BANNED,
        INCOMPATIBLE_GENDER
    }

    /**
     * Idempotent. Clears any pending trade held by {@code player} and releases the associated offer lock
     * when this player is the lock owner.
     */
    public static void releasePendingFor(ServerPlayer player) {
        GtsSavedData data = GtsSavedData.get(player.getServer());
        GtsPendingTrade prev = PENDING_BY_PLAYER.remove(player.getUUID());
        if (prev == null) {
            return;
        }
        data.findOffer(prev.getOfferId()).ifPresent(o -> {
            if (o.isLocked() && player.getUUID().equals(o.getLockOwnerUuid())) {
                o.clearLock();
                data.setDirty();
            }
        });
    }

    public static ValidateSpeciesResult validateWishSpecies(String line, GenderFilter wishGender) {
        if (!WishLineValidator.isSafe(line)) {
            return ValidateSpeciesResult.INVALID;
        }
        PokemonProperties wishProps = PokemonProperties.Companion.parse(line.trim());
        if (wishProps.getSpecies() == null) {
            return ValidateSpeciesResult.INVALID;
        }
        String wishSpeciesName = wishProps.getSpecies();
        if (wishSpeciesName != null) {
            String ws = wishSpeciesName.toLowerCase(java.util.Locale.ROOT);
            if (GtsSettings.get().isSpeciesBanned(ws)) {
                return ValidateSpeciesResult.BANNED;
            }
        }
        if (!isWishGenderCompatible(line.trim(), wishGender)) {
            return ValidateSpeciesResult.INCOMPATIBLE_GENDER;
        }
        return ValidateSpeciesResult.OK;
    }

    public record SearchResult(int page, int totalPages, List<GtsOffer> offers) {}

    public static SearchResult searchOffers(
            MinecraftServer server,
            int page,
            String speciesFilter,
            GenderFilter genderFilter,
            GtsOffer.ShinyWish shinyFilter) {
        return searchOffers(server, page, speciesFilter, genderFilter, shinyFilter, null);
    }

    /** Player Seek screen — excludes the searching player's own offers. */
    public static SearchResult searchOffersForPlayer(
            ServerPlayer player,
            int page,
            String speciesFilter,
            GenderFilter genderFilter,
            GtsOffer.ShinyWish shinyFilter) {
        return searchOffers(
                player.getServer(), page, speciesFilter, genderFilter, shinyFilter, player.getUUID());
    }

    public static SearchResult searchOffers(
            MinecraftServer server,
            int page,
            String speciesFilter,
            GenderFilter genderFilter,
            GtsOffer.ShinyWish shinyFilter,
            java.util.UUID viewerUuid) {
        GtsSavedData data = GtsSavedData.get(server);
        String s = speciesFilter == null ? "" : speciesFilter.trim().toLowerCase(java.util.Locale.ROOT);
        long now = System.currentTimeMillis();
        List<GtsOffer> all = new ArrayList<>(data.getOffers());
        all.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        List<GtsOffer> filtered = new ArrayList<>();
        for (GtsOffer offer : all) {
            if (viewerUuid != null && viewerUuid.equals(offer.getDepositorUuid())) {
                continue;
            }
            if (offer.isPersonalOffer()
                    && (viewerUuid == null || !viewerUuid.equals(offer.getPersonalTargetUuid()))) {
                continue;
            }
            if (offer.isLocked() && offer.getLockExpireEpochMs() > 0L && now <= offer.getLockExpireEpochMs()) {
                continue;
            }
            if (!matchesSearch(offer, s, genderFilter, shinyFilter)) {
                continue;
            }
            filtered.add(offer);
        }
        int pageSize = 10;
        int totalPages = Math.max(1, (filtered.size() + pageSize - 1) / pageSize);
        int p = Math.max(1, Math.min(page, totalPages));
        int from = (p - 1) * pageSize;
        int to = Math.min(filtered.size(), from + pageSize);
        List<GtsOffer> pageOffers = from >= filtered.size() ? List.of() : filtered.subList(from, to);
        return new SearchResult(p, totalPages, pageOffers);
    }

    private static boolean matchesSearch(
            GtsOffer offer,
            String speciesLower,
            GenderFilter genderFilter,
            GtsOffer.ShinyWish shinyFilter) {
        if (!speciesLower.isEmpty() && !offer.getDepositedSpeciesPath().contains(speciesLower)) {
            return false;
        }
        if (genderFilter != GenderFilter.ANY) {
            Gender want = genderFilter.toCobblemonGenderOrNull();
            if (want != null && offer.getDepositedGender() != want) {
                return false;
            }
        }
        if (shinyFilter != GtsOffer.ShinyWish.ANY) {
            boolean wantShiny = shinyFilter == GtsOffer.ShinyWish.SHINY;
            if (offer.isDepositedShiny() != wantShiny) {
                return false;
            }
        }
        return true;
    }

    public record CandidateView(GtsTradeCandidate ref, CompoundTag nbt) {}

    public record StartTradeCandidateBundle(StartTradeKind kind, List<CandidateView> views) {}

    public static StartTradeCandidateBundle tryStartTradeWithViews(ServerPlayer player, int offerId) {
        StartTradeResult r = tryStartTrade(player, offerId);
        if (r.kind() != StartTradeKind.OK) {
            return new StartTradeCandidateBundle(r.kind(), List.of());
        }
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        List<CandidateView> views = new ArrayList<>();
        int max = 30;
        for (GtsTradeCandidate ref : r.candidates()) {
            if (views.size() >= max) {
                break;
            }
            Pokemon p = resolveCandidate(party, pc, ref);
            if (p != null) {
                views.add(
                        new CandidateView(
                                ref,
                                p.saveToNBT(player.getServer().registryAccess(), new CompoundTag())));
            }
        }
        return new StartTradeCandidateBundle(StartTradeKind.OK, views);
    }

    public static DepositResult tryDeposit(
            ServerPlayer player,
            int internalSlot0To5,
            String wishSpeciesLine,
            int wishLevelBucket,
            GenderFilter wishGender,
            GtsOffer.ShinyWish wishShiny) {
        GtsSettings cfg = GtsSettings.get();
        GtsSavedData data = GtsSavedData.get(player.getServer());
        if (!data.findOffersByDepositor(player.getUUID()).isEmpty()) {
            return DepositResult.ALREADY_HAS_OFFER;
        }
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Pokemon offered = party.get(internalSlot0To5);
        if (offered == null) {
            return DepositResult.EMPTY_SLOT;
        }
        String depositSpeciesId = offered.getSpecies().getResourceIdentifier().toString().toLowerCase();
        String depositPath = offered.getSpecies().getResourceIdentifier().getPath().toLowerCase();
        if (cfg.isSpeciesBanned(depositPath) || cfg.isSpeciesBanned(depositSpeciesId)) {
            return DepositResult.BANNED_DEPOSIT;
        }
        if (wishSpeciesLine == null || wishSpeciesLine.isBlank()) {
            return DepositResult.UNKNOWN_WISH_SPECIES;
        }
        if (!WishLineValidator.isSafe(wishSpeciesLine)) {
            return DepositResult.UNKNOWN_WISH_SPECIES;
        }
        PokemonProperties wishProps = PokemonProperties.Companion.parse(wishSpeciesLine.trim());
        if (wishProps.getSpecies() == null) {
            return DepositResult.UNKNOWN_WISH_SPECIES;
        }
        String wishSpeciesName = wishProps.getSpecies();
        if (wishSpeciesName != null) {
            String ws = wishSpeciesName.toLowerCase(java.util.Locale.ROOT);
            if (cfg.isSpeciesBanned(ws)) {
                return DepositResult.BANNED_WISH;
            }
        }
        if (wishLevelBucket != -1 && (wishLevelBucket < 0 || wishLevelBucket > 9)) {
            return DepositResult.INVALID_LEVEL_BUCKET;
        }
        if (!isWishGenderCompatible(wishSpeciesLine.trim(), wishGender)) {
            return DepositResult.INCOMPATIBLE_GENDER;
        }
        String depPath = offered.getSpecies().getResourceIdentifier().getPath().toLowerCase(java.util.Locale.ROOT);
        Gender depGender = offered.getGender();
        boolean depShiny = offered.getShiny();

        int offerId = data.allocateOfferId();
        CompoundTag nbt = offered.saveToNBT(player.getServer().registryAccess(), new CompoundTag());
        GtsOffer offer =
                new GtsOffer(
                        offerId,
                        player.getUUID(),
                        nbt,
                        wishSpeciesLine.trim(),
                        wishLevelBucket,
                        wishGender,
                        wishShiny == null ? GtsOffer.ShinyWish.ANY : wishShiny,
                        depPath,
                        depGender,
                        depShiny);
        try {
            data.addOffer(offer);
            if (!party.remove(offered)) {
                data.removeOffer(offerId);
                CobbleSafari.LOGGER.error("[GTS] Failed to remove deposited pokemon from party; rolled back offer {}", offerId);
                return DepositResult.ERROR;
            }
            data.setDirty();
            return DepositResult.SUCCESS;
        } catch (Exception ex) {
            CobbleSafari.LOGGER.error("[GTS] Deposit failed", ex);
            data.removeOffer(offerId);
            return DepositResult.ERROR;
        }
    }

    public enum AddUniqueOfferResult {
        SUCCESS,
        UNKNOWN_OFFER_ID,
        INVALID_GIVEN,
        BANNED_GIVEN,
        BANNED_WISH,
        INCOMPATIBLE_GENDER,
        INVALID_LEVEL_BUCKET,
        ERROR
    }

    public record AddUniqueOfferOutcome(AddUniqueOfferResult result, int runtimeOfferId) {
        public static AddUniqueOfferOutcome fail(AddUniqueOfferResult result) {
            return new AddUniqueOfferOutcome(result, -1);
        }

        public static AddUniqueOfferOutcome ok(int runtimeOfferId) {
            return new AddUniqueOfferOutcome(AddUniqueOfferResult.SUCCESS, runtimeOfferId);
        }
    }

    public static AddUniqueOfferOutcome addUniqueOffer(MinecraftServer server, String templateOfferId) {
        return publishUniqueOffer(server, templateOfferId, null);
    }

    /**
     * Builds and publishes a unique offer from a template definition.
     *
     * @param personalTargetUuid when non-null, the offer is bound to that player (personal offer): visible only to
     *     them, never expires; otherwise it is a global unique offer.
     */
    private static AddUniqueOfferOutcome publishUniqueOffer(
            MinecraftServer server, String templateOfferId, UUID personalTargetUuid) {
        Optional<GtsUniqueOfferDefinition> defOpt = GtsUniqueOfferRegistry.get(templateOfferId);
        if (defOpt.isEmpty()) {
            return AddUniqueOfferOutcome.fail(AddUniqueOfferResult.UNKNOWN_OFFER_ID);
        }
        GtsUniqueOfferDefinition def = defOpt.get();
        GtsSettings cfg = GtsSettings.get();
        GtsSavedData data = GtsSavedData.get(server);

        AddUniqueOfferResult wishCheck = validateWishForUniqueOffer(def, cfg);
        if (wishCheck != null) {
            return AddUniqueOfferOutcome.fail(wishCheck);
        }

        Pokemon given;
        try {
            given = createGivenPokemon(def);
        } catch (IllegalStateException e) {
            CobbleSafari.LOGGER.warn("[GTS] publishUniqueOffer invalid given for {}: {}", templateOfferId, e.getMessage());
            return AddUniqueOfferOutcome.fail(AddUniqueOfferResult.INVALID_GIVEN);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[GTS] publishUniqueOffer failed to create given for {}", templateOfferId, e);
            return AddUniqueOfferOutcome.fail(AddUniqueOfferResult.ERROR);
        }

        String depositPath = given.getSpecies().getResourceIdentifier().getPath().toLowerCase(java.util.Locale.ROOT);
        String depositId = given.getSpecies().getResourceIdentifier().toString().toLowerCase(java.util.Locale.ROOT);
        if (cfg.isSpeciesBanned(depositPath) || cfg.isSpeciesBanned(depositId)) {
            return AddUniqueOfferOutcome.fail(AddUniqueOfferResult.BANNED_GIVEN);
        }

        int offerId = data.allocateOfferId();
        CompoundTag nbt = given.saveToNBT(server.registryAccess(), new CompoundTag());
        TradeNbtSanitizer.sanitize(nbt, cfg::isHeldItemBanned);
        GtsOffer offer =
                new GtsOffer(
                        offerId,
                        UNIQUE_OFFER_DEPOSITOR_UUID,
                        nbt,
                        def.getWishSpeciesLine(),
                        def.getWishLevelBucket(),
                        def.getWishGender(),
                        def.getWishShiny(),
                        depositPath,
                        given.getGender(),
                        given.getShiny(),
                        true,
                        def.getOfferId(),
                        personalTargetUuid);
        data.addOffer(offer);
        data.setDirty();
        return AddUniqueOfferOutcome.ok(offerId);
    }

    public enum AddPersonalOfferResult {
        SUCCESS,
        UNKNOWN_OFFER_ID,
        INVALID_GIVEN,
        BANNED_GIVEN,
        BANNED_WISH,
        INCOMPATIBLE_GENDER,
        INVALID_LEVEL_BUCKET,
        ALREADY_HAS_PERSONAL,
        ERROR
    }

    public record AddPersonalOfferOutcome(AddPersonalOfferResult result, int runtimeOfferId) {
        public static AddPersonalOfferOutcome fail(AddPersonalOfferResult result) {
            return new AddPersonalOfferOutcome(result, -1);
        }

        public static AddPersonalOfferOutcome ok(int runtimeOfferId) {
            return new AddPersonalOfferOutcome(AddPersonalOfferResult.SUCCESS, runtimeOfferId);
        }
    }

    /**
     * Public API: grants a personal (player-bound) unique offer built from a template. Usable by admin commands and by
     * external systems (e.g. quest rewards) without requiring the target to be online — only the UUID is stored.
     *
     * <p>At most one personal offer per {@code (targetUuid, templateOfferId)} pair may exist.
     */
    public static AddPersonalOfferOutcome addPersonalOffer(
            MinecraftServer server, UUID targetUuid, String templateOfferId) {
        GtsSavedData data = GtsSavedData.get(server);
        boolean duplicate =
                data.getOffers().stream()
                        .anyMatch(
                                o ->
                                        o.isPersonalOffer()
                                                && targetUuid.equals(o.getPersonalTargetUuid())
                                                && templateOfferId.equals(o.getUniqueOfferTemplateId()));
        if (duplicate) {
            return AddPersonalOfferOutcome.fail(AddPersonalOfferResult.ALREADY_HAS_PERSONAL);
        }
        AddUniqueOfferOutcome outcome = publishUniqueOffer(server, templateOfferId, targetUuid);
        AddPersonalOfferResult mapped =
                switch (outcome.result()) {
                    case SUCCESS -> AddPersonalOfferResult.SUCCESS;
                    case UNKNOWN_OFFER_ID -> AddPersonalOfferResult.UNKNOWN_OFFER_ID;
                    case INVALID_GIVEN -> AddPersonalOfferResult.INVALID_GIVEN;
                    case BANNED_GIVEN -> AddPersonalOfferResult.BANNED_GIVEN;
                    case BANNED_WISH -> AddPersonalOfferResult.BANNED_WISH;
                    case INCOMPATIBLE_GENDER -> AddPersonalOfferResult.INCOMPATIBLE_GENDER;
                    case INVALID_LEVEL_BUCKET -> AddPersonalOfferResult.INVALID_LEVEL_BUCKET;
                    case ERROR -> AddPersonalOfferResult.ERROR;
                };
        if (mapped == AddPersonalOfferResult.SUCCESS) {
            return AddPersonalOfferOutcome.ok(outcome.runtimeOfferId());
        }
        return AddPersonalOfferOutcome.fail(mapped);
    }

    public enum RemovePersonalOfferResult {
        SUCCESS,
        NOT_FOUND,
        LOCKED
    }

    /**
     * Public API: removes the personal offer bound to {@code (targetUuid, templateOfferId)}. No {@code GtsSuccess} is
     * produced (personal offers are sinks).
     */
    public static RemovePersonalOfferResult removePersonalOffer(
            MinecraftServer server, UUID targetUuid, String templateOfferId) {
        GtsSavedData data = GtsSavedData.get(server);
        Optional<GtsOffer> opt =
                data.getOffers().stream()
                        .filter(
                                o ->
                                        o.isPersonalOffer()
                                                && targetUuid.equals(o.getPersonalTargetUuid())
                                                && templateOfferId.equals(o.getUniqueOfferTemplateId()))
                        .findFirst();
        if (opt.isEmpty()) {
            return RemovePersonalOfferResult.NOT_FOUND;
        }
        GtsOffer offer = opt.get();
        long now = System.currentTimeMillis();
        if (offer.isLocked() && (offer.getLockExpireEpochMs() == 0L || now <= offer.getLockExpireEpochMs())) {
            return RemovePersonalOfferResult.LOCKED;
        }
        data.removeOffer(offer.getId());
        data.setDirty();
        return RemovePersonalOfferResult.SUCCESS;
    }

    /** @return error result, or {@code null} if wish is valid */
    private static AddUniqueOfferResult validateWishForUniqueOffer(GtsUniqueOfferDefinition def, GtsSettings cfg) {
        String wishLine = def.getWishSpeciesLine();
        if (!WishLineValidator.isSafe(wishLine)) {
            return AddUniqueOfferResult.ERROR;
        }
        PokemonProperties wishProps = PokemonProperties.Companion.parse(wishLine.trim());
        if (wishProps.getSpecies() == null) {
            return AddUniqueOfferResult.ERROR;
        }
        String wishSpeciesName = wishProps.getSpecies();
        if (wishSpeciesName != null) {
            String ws = wishSpeciesName.toLowerCase(java.util.Locale.ROOT);
            if (cfg.isSpeciesBanned(ws)) {
                return AddUniqueOfferResult.BANNED_WISH;
            }
        }
        if (def.getWishLevelBucket() != -1 && (def.getWishLevelBucket() < 0 || def.getWishLevelBucket() > 9)) {
            return AddUniqueOfferResult.INVALID_LEVEL_BUCKET;
        }
        if (!isWishGenderCompatible(wishLine.trim(), def.getWishGender())) {
            return AddUniqueOfferResult.INCOMPATIBLE_GENDER;
        }
        return null;
    }

    public static Pokemon createGivenPokemon(GtsUniqueOfferDefinition def) {
        PokemonProperties props = PokemonProperties.Companion.parse(def.getGivenLine());
        if (props.getSpecies() == null) {
            throw new IllegalStateException("no species in given line");
        }
        Pokemon pokemon = props.create(null);
        for (ResourceLocation markId : def.getGivenMarkIds()) {
            Mark mark = Marks.getByIdentifier(markId);
            if (mark != null) {
                pokemon.exchangeMark(mark, true);
            }
        }
        ResourceLocation activeId = def.getGivenActiveMarkId();
        if (activeId != null) {
            Mark active = Marks.getByIdentifier(activeId);
            if (active != null) {
                pokemon.setActiveMark(active);
            }
        }
        return pokemon;
    }

    public static boolean isWishGenderCompatible(String wishSpeciesLine, GenderFilter wishGender) {
        if (wishGender == GenderFilter.ANY) {
            return true;
        }
        PokemonProperties wishProps = PokemonProperties.Companion.parse(wishSpeciesLine);
        Pokemon temp = wishProps.create(null);
        float ratio = temp.getForm().getMaleRatio();
        Gender g = wishGender.toCobblemonGenderOrNull();
        if (g == null) {
            return true;
        }
        if (ratio == -1f) {
            return g == Gender.GENDERLESS;
        }
        if (ratio == 0f) {
            return g == Gender.FEMALE;
        }
        if (ratio == 1f) {
            return g == Gender.MALE;
        }
        return g == Gender.MALE || g == Gender.FEMALE;
    }

    public enum StartTradeKind {
        OK,
        OFFER_NOT_FOUND,
        OFFER_LOCKED,
        OFFER_OWN,
        NO_MATCHING_POKEMON,
        ERROR
    }

    public record StartTradeResult(StartTradeKind kind, List<GtsTradeCandidate> candidates) {
        public static StartTradeResult ok(List<GtsTradeCandidate> c) {
            return new StartTradeResult(StartTradeKind.OK, c);
        }

        public static StartTradeResult of(StartTradeKind k) {
            return new StartTradeResult(k, List.of());
        }
    }

    private static boolean candidateMatchesWish(
            Pokemon candidate,
            PokemonProperties wishProps,
            int wishLevelBucket,
            GenderFilter wishGender,
            GtsOffer.ShinyWish wishShiny) {
        if (!wishProps.matches(candidate)) {
            return false;
        }
        if (wishLevelBucket != -1) {
            int lvl = Math.max(1, candidate.getLevel());
            int bucket = Math.min(9, Math.floorDiv(lvl - 1, 10));
            if (bucket != wishLevelBucket) {
                return false;
            }
        }
        if (wishGender != GenderFilter.ANY) {
            Gender want = wishGender.toCobblemonGenderOrNull();
            if (want != null && candidate.getGender() != want) {
                return false;
            }
        }
        if (wishShiny != null && wishShiny != GtsOffer.ShinyWish.ANY) {
            boolean wantShiny = wishShiny == GtsOffer.ShinyWish.SHINY;
            if (candidate.getShiny() != wantShiny) {
                return false;
            }
        }
        return true;
    }

    public static StartTradeResult tryStartTrade(ServerPlayer player, int offerId) {
        releasePendingFor(player);

        GtsSavedData data = GtsSavedData.get(player.getServer());

        long heldLocks = data.getOffers().stream()
                .filter(o -> o.isLocked() && player.getUUID().equals(o.getLockOwnerUuid()))
                .count();
        if (heldLocks >= MAX_LOCKS_PER_PLAYER) {
            CobbleSafari.LOGGER.info(
                    "[GTS] Player {} attempted to acquire a second concurrent lock; defensive rollback applied",
                    player.getUUID());
            data.getOffers().stream()
                    .filter(o -> o.isLocked() && player.getUUID().equals(o.getLockOwnerUuid()))
                    .forEach(GtsOffer::clearLock);
            data.setDirty();
        }

        Optional<GtsOffer> opt = data.findOffer(offerId);
        if (opt.isEmpty()) {
            return StartTradeResult.of(StartTradeKind.OFFER_NOT_FOUND);
        }
        GtsOffer offer = opt.get();
        if (offer.getDepositorUuid().equals(player.getUUID())) {
            return StartTradeResult.of(StartTradeKind.OFFER_OWN);
        }
        if (offer.isPersonalOffer() && !player.getUUID().equals(offer.getPersonalTargetUuid())) {
            return StartTradeResult.of(StartTradeKind.OFFER_NOT_FOUND);
        }
        long now = System.currentTimeMillis();
        if (offer.isLocked() && offer.getLockExpireEpochMs() > 0L && now > offer.getLockExpireEpochMs()) {
            offer.clearLock();
            data.setDirty();
        }
        if (offer.isLocked()) {
            return StartTradeResult.of(StartTradeKind.OFFER_LOCKED);
        }
        PokemonProperties wishProps = PokemonProperties.Companion.parse(offer.getWishSpecies());
        if (wishProps.getSpecies() == null) {
            return StartTradeResult.of(StartTradeKind.ERROR);
        }
        offer.setLocked(true);
        offer.setLockOwnerUuid(player.getUUID());
        offer.setLockExpireEpochMs(now + SELECTION_TTL_MS);
        data.setDirty();

        List<GtsTradeCandidate> found = new ArrayList<>();
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        for (int slot = 0; slot < 6; slot++) {
            Pokemon p = party.get(slot);
            if (p != null
                    && candidateMatchesWish(
                            p,
                            wishProps,
                            offer.getWishLevelBucket(),
                            offer.getWishGender(),
                            offer.getWishShiny())) {
                found.add(new GtsTradeCandidate(GtsTradeCandidate.CandidateSource.PARTY, slot, -1, -1, p.getUuid()));
            }
        }
        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        List<PCBox> boxes = pc.getBoxes();
        for (int bi = 0; bi < boxes.size(); bi++) {
            PCBox box = boxes.get(bi);
            for (int si = 0; si < PC_SLOTS_PER_BOX; si++) {
                Pokemon p = box.get(si);
                if (p != null
                        && candidateMatchesWish(
                                p,
                                wishProps,
                                offer.getWishLevelBucket(),
                                offer.getWishGender(),
                                offer.getWishShiny())) {
                    found.add(new GtsTradeCandidate(GtsTradeCandidate.CandidateSource.PC, -1, bi, si, p.getUuid()));
                }
            }
        }
        if (found.isEmpty()) {
            offer.clearLock();
            data.setDirty();
            return StartTradeResult.of(StartTradeKind.NO_MATCHING_POKEMON);
        }
        PENDING_BY_PLAYER.put(player.getUUID(), new GtsPendingTrade(offerId, found, now + SELECTION_TTL_MS));
        return StartTradeResult.ok(found);
    }

    public enum ConfirmTradeKind {
        SUCCESS,
        SELECTION_EXPIRED,
        CANDIDATE_GONE,
        ERROR
    }

    public static Optional<CompoundTag> peekPendingCandidateNbt(
            ServerPlayer player, int offerId, int pickIndex1Based) {
        GtsPendingTrade pending = PENDING_BY_PLAYER.get(player.getUUID());
        if (pending == null || pending.isExpired() || pending.getOfferId() != offerId) {
            return Optional.empty();
        }
        int idx = pickIndex1Based - 1;
        if (idx < 0 || idx >= pending.getCandidates().size()) {
            return Optional.empty();
        }
        GtsTradeCandidate ref = pending.getCandidates().get(idx);
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        Pokemon p = resolveCandidate(party, pc, ref);
        if (p == null) {
            return Optional.empty();
        }
        return Optional.of(p.saveToNBT(player.getServer().registryAccess(), new CompoundTag()));
    }

    public static ConfirmTradeKind tryConfirmTrade(ServerPlayer player, int offerId, int pickIndex1Based) {
        GtsSettings cfg = GtsSettings.get();
        GtsSavedData data = GtsSavedData.get(player.getServer());
        GtsPendingTrade pending = PENDING_BY_PLAYER.get(player.getUUID());
        if (pending == null || pending.isExpired() || pending.getOfferId() != offerId) {
            data.findOffer(offerId).ifPresent(o -> {
                if (o.isLocked() && player.getUUID().equals(o.getLockOwnerUuid())) {
                    o.clearLock();
                    data.setDirty();
                }
            });
            PENDING_BY_PLAYER.remove(player.getUUID());
            return ConfirmTradeKind.SELECTION_EXPIRED;
        }
        int idx = pickIndex1Based - 1;
        if (idx < 0 || idx >= pending.getCandidates().size()) {
            return ConfirmTradeKind.SELECTION_EXPIRED;
        }
        GtsTradeCandidate ref = pending.getCandidates().get(idx);
        Optional<GtsOffer> offerOpt = data.findOffer(offerId);
        if (offerOpt.isEmpty()) {
            PENDING_BY_PLAYER.remove(player.getUUID());
            return ConfirmTradeKind.ERROR;
        }
        GtsOffer offer = offerOpt.get();
        if (offer.isPersonalOffer() && !player.getUUID().equals(offer.getPersonalTargetUuid())) {
            PENDING_BY_PLAYER.remove(player.getUUID());
            return ConfirmTradeKind.SELECTION_EXPIRED;
        }
        long now = System.currentTimeMillis();
        if (!offer.isLocked() || !player.getUUID().equals(offer.getLockOwnerUuid())) {
            PENDING_BY_PLAYER.remove(player.getUUID());
            return ConfirmTradeKind.SELECTION_EXPIRED;
        }
        if (offer.getLockExpireEpochMs() > 0L && now > offer.getLockExpireEpochMs()) {
            offer.clearLock();
            data.setDirty();
            PENDING_BY_PLAYER.remove(player.getUUID());
            return ConfirmTradeKind.SELECTION_EXPIRED;
        }
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        Pokemon candidate = resolveCandidate(party, pc, ref);
        if (candidate == null || !candidate.getUuid().equals(ref.pokemonUuid())) {
            offer.clearLock();
            data.setDirty();
            PENDING_BY_PLAYER.remove(player.getUUID());
            return ConfirmTradeKind.CANDIDATE_GONE;
        }
        CompoundTag receivedRawNbt = offer.getPokemonData().copy();
        TradeNbtSanitizer.sanitize(receivedRawNbt, cfg::isHeldItemBanned);
        Pokemon received;
        try {
            received = Pokemon.Companion.loadFromNBT(player.getServer().registryAccess(), receivedRawNbt);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[GTS] Failed to load offer pokemon", e);
            offer.clearLock();
            data.setDirty();
            PENDING_BY_PLAYER.remove(player.getUUID());
            return ConfirmTradeKind.ERROR;
        }
        received.setFriendship(received.getForm().getBaseFriendship(), true);
        CompoundTag givenNbt = candidate.saveToNBT(player.getServer().registryAccess(), new CompoundTag());
        TradeNbtSanitizer.sanitize(givenNbt, cfg::isHeldItemBanned);
        boolean uniqueOffer = offer.isUniqueOffer();
        int sid = uniqueOffer ? -1 : data.allocateSuccessId();
        GtsSuccess success =
                uniqueOffer
                        ? null
                        : new GtsSuccess(sid, offer.getDepositorUuid(), givenNbt, GtsSuccess.Reason.TRADED);
        try {
            if (!uniqueOffer) {
                data.addSuccess(success);
            }
            data.removeOffer(offerId);
            if (!removeCandidateFromStore(party, pc, ref, candidate)) {
                if (!uniqueOffer) {
                    data.removeSuccess(sid);
                }
                data.addOffer(offer);
                offer.clearLock();
                data.setDirty();
                PENDING_BY_PLAYER.remove(player.getUUID());
                CobbleSafari.LOGGER.error("[GTS] Rollback: could not remove candidate from player {}", player.getUUID());
                return ConfirmTradeKind.ERROR;
            }
            if (!party.add(received) && !pc.add(received)) {
                CobbleSafari.LOGGER.error("[GTS] Party and PC full after trade; rolling back");
                restoreCandidateAt(party, pc, ref, candidate);
                if (!uniqueOffer) {
                    data.removeSuccess(sid);
                }
                data.addOffer(offer);
                offer.clearLock();
                data.setDirty();
                PENDING_BY_PLAYER.remove(player.getUUID());
                return ConfirmTradeKind.ERROR;
            }
            offer.clearLock();
            PENDING_BY_PLAYER.remove(player.getUUID());
            data.setDirty();
            int gtsTrades = maxigregrze.cobblesafari.init.ModStats.awardAndGet(
                    player, maxigregrze.cobblesafari.init.ModStats.GTS_TRADES);
            maxigregrze.cobblesafari.advancement.ModCriteria.GTS_TRADE_CONFIRMED.trigger(player, gtsTrades);
            if (uniqueOffer) {
                CobbleSafari.LOGGER.info(
                        "[GTS] Unique offer #{} template={} personalTarget={} completed; received pokemon discarded",
                        offerId,
                        offer.getUniqueOfferTemplateId(),
                        offer.isPersonalOffer() ? offer.getPersonalTargetUuid() : "none");
            } else if (cfg.isTradeNotification()) {
                ServerPlayer depositor = player.getServer().getPlayerList().getPlayer(offer.getDepositorUuid());
                if (depositor != null) {
                    depositor.sendSystemMessage(
                            Component.translatable(
                                    "cobblesafari.gts.notify.trade_completed",
                                    received.getSpecies().getName(),
                                    candidate.getSpecies().getName()));
                }
            }
            return ConfirmTradeKind.SUCCESS;
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[GTS] Trade confirm failed", e);
            return ConfirmTradeKind.ERROR;
        }
    }

    private static void restoreCandidateAt(PlayerPartyStore party, PCStore pc, GtsTradeCandidate ref, Pokemon candidate) {
        switch (ref.source()) {
            case PARTY -> party.set(ref.partySlot(), candidate);
            case PC -> pc.set(new PCPosition(ref.pcBox(), ref.pcSlot()), candidate);
        }
    }

    private static boolean removeCandidateFromStore(
            PlayerPartyStore party, PCStore pc, GtsTradeCandidate ref, Pokemon candidate) {
        return switch (ref.source()) {
            case PARTY -> party.remove(candidate);
            case PC -> pc.remove(candidate);
        };
    }

    private static Pokemon resolveCandidate(PlayerPartyStore party, PCStore pc, GtsTradeCandidate ref) {
        return switch (ref.source()) {
            case PARTY -> {
                Pokemon p = party.get(ref.partySlot());
                yield (p != null && p.getUuid().equals(ref.pokemonUuid())) ? p : null;
            }
            case PC -> {
                if (ref.pcBox() < 0 || ref.pcSlot() < 0) {
                    yield null;
                }
                List<PCBox> boxes = pc.getBoxes();
                if (ref.pcBox() >= boxes.size()) {
                    yield null;
                }
                Pokemon p = boxes.get(ref.pcBox()).get(ref.pcSlot());
                yield (p != null && p.getUuid().equals(ref.pokemonUuid())) ? p : null;
            }
        };
    }

    public enum RetrieveResult {
        SUCCESS,
        NOT_FOUND,
        NOT_OWNER,
        LOCKED,
        INVENTORY_FULL,
        ERROR
    }

    public static RetrieveResult tryRetrieveOwnOffer(ServerPlayer player, int offerId) {
        GtsSavedData data = GtsSavedData.get(player.getServer());
        Optional<GtsOffer> opt = data.findOffer(offerId);
        if (opt.isEmpty()) {
            return RetrieveResult.NOT_FOUND;
        }
        GtsOffer offer = opt.get();
        if (!offer.getDepositorUuid().equals(player.getUUID())) {
            return RetrieveResult.NOT_OWNER;
        }
        long now = System.currentTimeMillis();
        if (offer.isLocked() && (offer.getLockExpireEpochMs() == 0L || now <= offer.getLockExpireEpochMs())) {
            return RetrieveResult.LOCKED;
        }
        if (offer.isLocked() && now > offer.getLockExpireEpochMs()) {
            offer.clearLock();
        }
        Pokemon restored;
        try {
            restored = Pokemon.Companion.loadFromNBT(player.getServer().registryAccess(), offer.getPokemonData().copy());
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[GTS] retrieve load failed", e);
            return RetrieveResult.ERROR;
        }
        data.removeOffer(offerId);
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        if (!party.add(restored) && !pc.add(restored)) {
            data.addOffer(offer);
            data.setDirty();
            return RetrieveResult.INVENTORY_FULL;
        }
        data.setDirty();
        return RetrieveResult.SUCCESS;
    }

    public enum ClaimResult {
        SUCCESS,
        NOT_FOUND,
        NOT_RECIPIENT,
        INVENTORY_FULL,
        ERROR
    }

    public static ClaimResult tryClaimSuccess(ServerPlayer player, int successId) {
        GtsSavedData data = GtsSavedData.get(player.getServer());
        Optional<GtsSuccess> opt = data.findSuccess(successId);
        if (opt.isEmpty()) {
            return ClaimResult.NOT_FOUND;
        }
        GtsSuccess row = opt.get();
        if (!row.getRecipientUuid().equals(player.getUUID())) {
            return ClaimResult.NOT_RECIPIENT;
        }
        Pokemon p;
        try {
            p = Pokemon.Companion.loadFromNBT(player.getServer().registryAccess(), row.getPokemonData().copy());
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[GTS] claim load failed", e);
            return ClaimResult.ERROR;
        }
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        if (!party.add(p) && !pc.add(p)) {
            return ClaimResult.INVENTORY_FULL;
        }
        data.removeSuccess(successId);
        data.setDirty();
        if (row.getReason() == GtsSuccess.Reason.TRADED) {
            int gtsTrades = maxigregrze.cobblesafari.init.ModStats.awardAndGet(
                    player, maxigregrze.cobblesafari.init.ModStats.GTS_TRADES);
            maxigregrze.cobblesafari.advancement.ModCriteria.GTS_TRADE_DEPOSIT_SOLD.trigger(player, gtsTrades);
        }
        return ClaimResult.SUCCESS;
    }

    public enum AdminRemoveResult {
        SUCCESS,
        NOT_FOUND,
        LOCKED
    }

    public static AdminRemoveResult adminRemoveOffer(MinecraftServer server, int offerId) {
        GtsSavedData data = GtsSavedData.get(server);
        Optional<GtsOffer> opt = data.findOffer(offerId);
        if (opt.isEmpty()) {
            return AdminRemoveResult.NOT_FOUND;
        }
        GtsOffer offer = opt.get();
        long now = System.currentTimeMillis();
        if (offer.isLocked() && (offer.getLockExpireEpochMs() == 0L || now <= offer.getLockExpireEpochMs())) {
            return AdminRemoveResult.LOCKED;
        }
        if (offer.isLocked()) {
            offer.clearLock();
        }
        if (offer.isUniqueOffer()) {
            data.removeOffer(offerId);
        } else {
            int sid = data.allocateSuccessId();
            data.addSuccess(
                    new GtsSuccess(
                            sid,
                            offer.getDepositorUuid(),
                            offer.getPokemonData().copy(),
                            GtsSuccess.Reason.ADMIN_REMOVED));
            data.removeOffer(offerId);
        }
        data.setDirty();
        return AdminRemoveResult.SUCCESS;
    }

    public static Optional<String> lookupUsername(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return Optional.of(online.getGameProfile().getName());
        }
        return server.getProfileCache().get(id).map(com.mojang.authlib.GameProfile::getName);
    }
}
