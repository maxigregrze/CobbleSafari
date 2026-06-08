package maxigregrze.cobblesafari.wondertrade;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.mark.Mark;
import com.cobblemon.mod.common.api.mark.Marks;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.WonderTradeSettings;
import maxigregrze.cobblesafari.data.WonderTradeSavedData;
import maxigregrze.cobblesafari.security.TradeNbtSanitizer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class WonderTradeService {
    public static final ResourceLocation RIBBON_WONDER_ID = ResourceLocation.fromNamespaceAndPath("cobblesafari", "ribbon_wonder");

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final Stat[] IV_STATS = {
            Stats.HP,
            Stats.ATTACK,
            Stats.DEFENCE,
            Stats.SPECIAL_ATTACK,
            Stats.SPECIAL_DEFENCE,
            Stats.SPEED
    };

    private static final int HARD_LEVEL_MIN = 1;
    private static final int HARD_LEVEL_MAX = 100;
    private static final int HARD_EV_PER_STAT_MAX = 252;
    private static final int HARD_EV_TOTAL_MAX = 510;

    private WonderTradeService() {}

    public enum OfferedValidation {
        OK,
        BANNED_SPECIES,
        BANNED_HELD_ITEM,
        LEVEL_OUT_OF_BOUNDS,
        EV_OVERFLOW
    }

    private static OfferedValidation validateOffered(Pokemon offered, WonderTradeSettings cfg) {
        String path = offered.getSpecies().getResourceIdentifier().getPath().toLowerCase(Locale.ROOT);
        String full = offered.getSpecies().getResourceIdentifier().toString().toLowerCase(Locale.ROOT);
        if (cfg.isSpeciesBanned(path) || cfg.isSpeciesBanned(full)) {
            return OfferedValidation.BANNED_SPECIES;
        }
        String heldItemId = resolveHeldItemId(offered);
        if (!heldItemId.isEmpty() && cfg.isHeldItemBanned(heldItemId)) {
            return OfferedValidation.BANNED_HELD_ITEM;
        }
        int lvl = offered.getLevel();
        if (lvl < HARD_LEVEL_MIN || lvl > HARD_LEVEL_MAX) {
            return OfferedValidation.LEVEL_OUT_OF_BOUNDS;
        }
        int evSum = 0;
        for (Stat s : IV_STATS) {
            int v = offered.getEvs().getOrDefault(s);
            if (v < 0 || v > HARD_EV_PER_STAT_MAX) {
                return OfferedValidation.EV_OVERFLOW;
            }
            evSum += v;
        }
        if (evSum > HARD_EV_TOTAL_MAX) {
            return OfferedValidation.EV_OVERFLOW;
        }
        return OfferedValidation.OK;
    }

    private static String resolveHeldItemId(Pokemon offered) {
        ItemStack held = offered.heldItem();
        if (held.isEmpty()) {
            return "";
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(held.getItem());
        return key.toString().toLowerCase(Locale.ROOT);
    }

    public static void onServerStarted(MinecraftServer server) {
        WonderTradeSettings.load();
        WonderTradeDataLoader.load(server);
        WonderTradeSavedData data = WonderTradeSavedData.get(server);
        if (data.getLastDailyResetEpochDay() < 0) {
            data.setLastDailyResetEpochDay(LocalDate.now(ZONE).minusDays(1).toEpochDay());
        }
        runAutofillIfNeeded(server);
    }

    public static void reloadDatapacks(MinecraftServer server) {
        WonderTradeDataLoader.load(server);
    }

    public static void runAutofillIfNeeded(MinecraftServer server) {
        WonderTradeSettings cfg = WonderTradeSettings.get();
        if (!cfg.isAutoFill()) {
            return;
        }
        WonderTradeSavedData data = WonderTradeSavedData.get(server);
        int target = cfg.getAutoFillAmount();
        int deficit = target - data.getPoolSize();
        if (deficit <= 0) {
            return;
        }
        RandomSource random = server.overworld().getRandom();
        for (int i = 0; i < deficit; i++) {
            WonderTradePoolEntry entry = tryGeneratePoolEntry(server, random);
            if (entry != null) {
                data.addPoolEntry(entry);
            }
        }
    }

    private static WonderTradePoolEntry tryGeneratePoolEntry(MinecraftServer server, RandomSource random) {
        WonderTradeSettings cfg = WonderTradeSettings.get();
        WonderTradeSavedData saved = WonderTradeSavedData.get(server);
        List<WonderTradeSettings.WeightedPoolEntry> weights;
        if (saved.hasActiveEvent()) {
            WonderTradeEventDefinition ev = WonderTradeEventRegistry.get(saved.getActiveEventId()).orElse(null);
            weights = ev != null ? ev.getEventPools() : cfg.getAutoFillFromPools();
        } else {
            weights = cfg.getAutoFillFromPools();
        }
        String groupId = pickWeightedGroupId(weights, random);
        if (groupId == null) {
            return null;
        }
        PokemonGroupDefinition group = WonderTradeGroupRegistry.get(groupId).orElse(null);
        if (group == null || group.getPopulation().isEmpty()) {
            CobbleSafari.LOGGER.warn("[WonderTrade] Autofill skipped: unknown or empty group {}", groupId);
            return null;
        }
        String speciesLine = group.getPopulation().get(random.nextInt(group.getPopulation().size()));
        int level = random.nextIntBetweenInclusive(cfg.getMinLevel(), cfg.getMaxLevel());
        String propsLine = speciesLine + " level=" + level;
        PokemonProperties props = PokemonProperties.Companion.parse(propsLine);
        if (props.getSpecies() == null) {
            CobbleSafari.LOGGER.warn("[WonderTrade] Autofill skipped: no species for line {}", speciesLine);
            return null;
        }
        Pokemon pokemon = props.create(null);
        if (random.nextDouble() < cfg.getPerfectChance()) {
            for (Stat stat : IV_STATS) {
                pokemon.getIvs().set(stat, cfg.getMaxIV());
            }
        } else {
            for (Stat stat : IV_STATS) {
                int iv = random.nextIntBetweenInclusive(cfg.getMinIV(), cfg.getMaxIV());
                pokemon.getIvs().set(stat, iv);
            }
        }
        if (random.nextDouble() < cfg.getRibbonChance()) {
            Mark mark = Marks.getByIdentifier(RIBBON_WONDER_ID);
            if (mark != null) {
                pokemon.exchangeMark(mark, true);
                pokemon.setActiveMark(mark);
            }
        }
        CompoundTag nbt = pokemon.saveToNBT(server.registryAccess(), new CompoundTag());
        return new WonderTradePoolEntry(nbt, 0, true);
    }

    private static String pickWeightedGroupId(List<WonderTradeSettings.WeightedPoolEntry> weights, RandomSource random) {
        int sum = 0;
        for (WonderTradeSettings.WeightedPoolEntry e : weights) {
            if (e.weight > 0) {
                sum += e.weight;
            }
        }
        if (sum <= 0) {
            return null;
        }
        int roll = random.nextIntBetweenInclusive(1, sum);
        int acc = 0;
        WonderTradeSettings.WeightedPoolEntry lastPositive = null;
        for (WonderTradeSettings.WeightedPoolEntry e : weights) {
            if (e.weight <= 0) {
                continue;
            }
            lastPositive = e;
            acc += e.weight;
            if (roll <= acc) {
                return e.groupId;
            }
        }
        return lastPositive != null ? lastPositive.groupId : null;
    }

    public static void runDailyReset(MinecraftServer server) {
        WonderTradeSettings cfg = WonderTradeSettings.get();
        WonderTradeSavedData data = WonderTradeSavedData.get(server);
        data.clearTradeCredits();

        if (cfg.isDoPokemonExpire()) {
            int best = cfg.getPokemonBestBefore();
            for (WonderTradePoolEntry e : new ArrayList<>(data.getPool())) {
                e.incrementTimeSinceDeposit();
            }
            data.getPool().removeIf(e -> e.getTimeSinceDeposit() > best);
            data.setDirty();
        }

        if (data.hasActiveEvent()) {
            if (data.getActiveEventMode() == 1) {
                data.clearEvent();
            } else if (data.getActiveEventMode() == 2) {
                data.decrementEventDaysLeft();
                if (data.getActiveEventDaysLeft() <= 0) {
                    data.clearEvent();
                }
            }
        }

        runAutofillIfNeeded(server);
        data.setDirty();
    }

    public static void tickDailyScheduler(MinecraftServer server) {
        WonderTradeSettings cfg = WonderTradeSettings.get();
        WonderTradeSavedData data = WonderTradeSavedData.get(server);
        LocalDate today = LocalDate.now(ZONE);
        LocalTime now = LocalTime.now(ZONE);
        long todayEpoch = today.toEpochDay();
        long last = data.getLastDailyResetEpochDay();
        int rh = cfg.getResetHour();

        if (last < 0) {
            return;
        }

        boolean pastResetHour = now.getHour() > rh || (now.getHour() == rh);
        boolean shouldRun = (todayEpoch > last && pastResetHour) || (todayEpoch > last + 1);

        if (!shouldRun) {
            return;
        }

        runDailyReset(server);
        data.setLastDailyResetEpochDay(todayEpoch);
    }

    public static int getRemainingCredits(ServerPlayer player) {
        WonderTradeSettings cfg = WonderTradeSettings.get();
        if (cfg.isUnlimitedDailyTrades()) {
            return Integer.MAX_VALUE;
        }
        WonderTradeSavedData data = WonderTradeSavedData.get(player.getServer());
        int v = data.getCredits(player.getUUID());
        if (v == Integer.MIN_VALUE) {
            return cfg.getDailyTrades();
        }
        return v;
    }

    private static void consumeCredit(ServerPlayer player) {
        WonderTradeSettings cfg = WonderTradeSettings.get();
        if (cfg.isUnlimitedDailyTrades()) {
            return;
        }
        WonderTradeSavedData data = WonderTradeSavedData.get(player.getServer());
        int cur = getRemainingCredits(player);
        if (cur <= 0) {
            return;
        }
        data.setCredits(player.getUUID(), cur - 1);
    }

    public record TradeResultDetailed(TradeResult result, CompoundTag offeredNbt, CompoundTag receivedNbt) {
        public static TradeResultDetailed failure(TradeResult r) {
            return new TradeResultDetailed(r, new CompoundTag(), new CompoundTag());
        }
    }

    public static int getEventDaysLeftForGui(MinecraftServer server) {
        WonderTradeSavedData data = WonderTradeSavedData.get(server);
        if (!data.hasActiveEvent()) {
            return 0;
        }
        if (data.getActiveEventMode() == 1) {
            return 1;
        }
        return Math.max(0, data.getActiveEventDaysLeft());
    }

    public static long getNextResetEpochSeconds() {
        WonderTradeSettings cfg = WonderTradeSettings.get();
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime next = now.withHour(cfg.getResetHour()).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return next.toEpochSecond();
    }

    public static TradeResultDetailed tryTrade(ServerPlayer player, int slot0To5) {
        WonderTradeSettings cfg = WonderTradeSettings.get();
        WonderTradeSavedData data = WonderTradeSavedData.get(player.getServer());
        if (!cfg.isUnlimitedDailyTrades() && getRemainingCredits(player) <= 0) {
            return TradeResultDetailed.failure(TradeResult.NO_CREDITS);
        }
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Pokemon offered = party.get(slot0To5);
        if (offered == null) {
            return TradeResultDetailed.failure(TradeResult.EMPTY_SLOT);
        }
        if (data.getPoolSize() <= 0) {
            return TradeResultDetailed.failure(TradeResult.POOL_EMPTY);
        }

        OfferedValidation ov = validateOffered(offered, cfg);
        if (ov != OfferedValidation.OK) {
            return TradeResultDetailed.failure(
                    switch (ov) {
                        case BANNED_SPECIES -> TradeResult.BANNED_DEPOSIT;
                        case BANNED_HELD_ITEM -> TradeResult.BANNED_HELD_ITEM;
                        case LEVEL_OUT_OF_BOUNDS -> TradeResult.LEVEL_OUT_OF_BOUNDS;
                        case EV_OVERFLOW -> TradeResult.EV_OVERFLOW;
                        default -> TradeResult.ERROR;
                    });
        }

        int idx = player.getServer().overworld().getRandom().nextInt(data.getPoolSize());
        WonderTradePoolEntry poolEntry = data.getPool().get(idx);

        CompoundTag incomingNbt;
        try {
            incomingNbt = poolEntry.getPokemonData().copy();
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[WonderTrade] Failed to copy pool NBT", e);
            return TradeResultDetailed.failure(TradeResult.ERROR);
        }

        TradeNbtSanitizer.sanitize(incomingNbt, cfg::isHeldItemBanned);

        Pokemon received;
        try {
            received = Pokemon.Companion.loadFromNBT(player.getServer().registryAccess(), incomingNbt);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[WonderTrade] Failed to load pool pokemon", e);
            return TradeResultDetailed.failure(TradeResult.ERROR);
        }

        received.setFriendship(received.getForm().getBaseFriendship(), true);

        CompoundTag offeredNbt = offered.saveToNBT(player.getServer().registryAccess(), new CompoundTag());
        WonderTradePoolEntry replacement = new WonderTradePoolEntry(offeredNbt.copy(), 0, false);
        CompoundTag receivedNbt = received.saveToNBT(player.getServer().registryAccess(), new CompoundTag());

        WonderTradePoolEntry previousAtIdx = data.getPool().get(idx);
        boolean poolMutated = false;
        boolean partyMutated = false;
        try {
            data.setPoolEntry(idx, replacement);
            poolMutated = true;
            party.set(slot0To5, received);
            partyMutated = true;
            consumeCredit(player);
            data.setDirty();
            int wonderTrades = maxigregrze.cobblesafari.init.ModStats.awardAndGet(
                    player, maxigregrze.cobblesafari.init.ModStats.WONDER_TRADES);
            maxigregrze.cobblesafari.advancement.ModCriteria.WONDER_TRADE.trigger(player, wonderTrades);
            return new TradeResultDetailed(TradeResult.SUCCESS, offeredNbt, receivedNbt);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[WonderTrade] Trade aborted, rolling back", e);
            if (partyMutated) {
                try {
                    party.set(slot0To5, offered);
                } catch (Exception ignored) {
                    // Best-effort rollback; original failure already logged above.
                }
            }
            if (poolMutated) {
                try {
                    data.setPoolEntry(idx, previousAtIdx);
                } catch (Exception ignored) {
                    // Best-effort rollback; original failure already logged above.
                }
            }
            data.setDirty();
            return TradeResultDetailed.failure(TradeResult.ERROR);
        }
    }

    public enum TradeResult {
        SUCCESS,
        EMPTY_SLOT,
        POOL_EMPTY,
        NO_CREDITS,
        BANNED_DEPOSIT,
        BANNED_HELD_ITEM,
        LEVEL_OUT_OF_BOUNDS,
        EV_OVERFLOW,
        ERROR
    }

    public static boolean startEvent(MinecraftServer server, String eventId, Integer durationDaysOrNull) {
        if (WonderTradeEventRegistry.get(eventId).isEmpty()) {
            return false;
        }
        WonderTradeSavedData data = WonderTradeSavedData.get(server);
        int d = durationDaysOrNull != null ? durationDaysOrNull : 0;
        if (d <= 0) {
            data.startEventNextReset(eventId);
        } else {
            data.startEventCountdown(eventId, d);
        }
        runAutofillIfNeeded(server);
        return true;
    }

    public static void stopEvent(MinecraftServer server) {
        WonderTradeSavedData.get(server).clearEvent();
        WonderTradeSavedData.get(server).setDirty();
    }

    /**
     * Clears the pool (entirely or only system-generated entries), then reapplies autofill if enabled.
     */
    public static PoolResetSummary resetPoolAndRefill(MinecraftServer server, boolean onlyGenerated) {
        WonderTradeSavedData data = WonderTradeSavedData.get(server);
        int removed;
        if (onlyGenerated) {
            removed = (int) data.getPool().stream().filter(WonderTradePoolEntry::isSystemGenerated).count();
            data.getPool().removeIf(WonderTradePoolEntry::isSystemGenerated);
            data.setDirty();
        } else {
            removed = data.getPoolSize();
            data.clearPool();
        }
        runAutofillIfNeeded(server);
        return new PoolResetSummary(removed, data.getPoolSize());
    }

    /** Effective credit value (never {@link Integer#MIN_VALUE}). */
    public static int getEffectiveTicketCount(MinecraftServer server, UUID playerId) {
        WonderTradeSettings cfg = WonderTradeSettings.get();
        if (cfg.isUnlimitedDailyTrades()) {
            return Integer.MAX_VALUE;
        }
        WonderTradeSavedData data = WonderTradeSavedData.get(server);
        int v = data.getCredits(playerId);
        if (v == Integer.MIN_VALUE) {
            return cfg.getDailyTrades();
        }
        return v;
    }

    public static void setPlayerTickets(MinecraftServer server, UUID playerId, int amount) {
        WonderTradeSavedData.get(server).setCredits(playerId, Math.max(0, amount));
    }

    public static void addPlayerTickets(MinecraftServer server, UUID playerId, int delta) {
        WonderTradeSettings cfg = WonderTradeSettings.get();
        if (cfg.isUnlimitedDailyTrades()) {
            return;
        }
        WonderTradeSavedData data = WonderTradeSavedData.get(server);
        int base = data.getCredits(playerId);
        if (base == Integer.MIN_VALUE) {
            base = cfg.getDailyTrades();
        }
        data.setCredits(playerId, Math.max(0, base + delta));
    }

    public static void removePlayerTickets(MinecraftServer server, UUID playerId, int amount) {
        WonderTradeSettings cfg = WonderTradeSettings.get();
        if (cfg.isUnlimitedDailyTrades()) {
            return;
        }
        WonderTradeSavedData data = WonderTradeSavedData.get(server);
        int base = data.getCredits(playerId);
        if (base == Integer.MIN_VALUE) {
            base = cfg.getDailyTrades();
        }
        data.setCredits(playerId, Math.max(0, base - amount));
    }

    public record PoolResetSummary(int removedEntries, int poolSizeAfter) {}
}
