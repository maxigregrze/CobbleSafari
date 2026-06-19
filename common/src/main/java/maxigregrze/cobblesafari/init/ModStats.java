package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

/**
 * Custom statistics shown under the vanilla Statistics → Custom tab.
 * Registered from {@link CobbleSafari#initRegistries()} before the registry freeze.
 */
public final class ModStats {

    private ModStats() {}

    public static final ResourceLocation TIME_IN_SAFARI = id("time_in_safari");
    public static final ResourceLocation POKEMON_CAUGHT_SAFARI = id("pokemon_caught_safari");
    public static final ResourceLocation POKEMON_FLED_SAFARI = id("pokemon_fled_safari");
    public static final ResourceLocation BAIT_USED_SAFARI = id("bait_used_safari");
    public static final ResourceLocation MUD_BALLS_USED_SAFARI = id("mud_balls_used_safari");
    public static final ResourceLocation HOOPA_PORTALS_EXPLORED = id("hoopa_portals_explored");
    public static final ResourceLocation HOOPA_SAVES = id("hoopa_saves");
    public static final ResourceLocation TIME_IN_DISTORTION = id("time_in_distortion");
    public static final ResourceLocation DARK_ONE_TRADES = id("dark_one_trades");
    public static final ResourceLocation TIME_IN_UNDERGROUND = id("time_in_underground");
    public static final ResourceLocation DIGSITES_USED = id("digsites_used");
    public static final ResourceLocation PERFECT_EXCAVATIONS = id("perfect_excavations");
    public static final ResourceLocation FAILED_EXCAVATIONS = id("failed_excavations");
    public static final ResourceLocation ROTO_GLIDE_JUMPS = id("roto_glide_jumps");
    public static final ResourceLocation WONDER_TRADES = id("wonder_trades");
    public static final ResourceLocation GTS_TRADES = id("gts_trades");
    public static final ResourceLocation UNION_ROOMS_CREATED = id("union_rooms_created");
    public static final ResourceLocation UNION_ROOMS_JOINED = id("union_rooms_joined");
    public static final ResourceLocation UNION_ROOM_WRONG_CODES = id("union_room_wrong_codes");
    public static final ResourceLocation TIME_IN_UNION_ROOM = id("time_in_union_room");
    public static final ResourceLocation CSTRADER_TRADES = id("cstrader_trades");
    public static final ResourceLocation CSBOSS_BATTLES_ATTEMPTED = id("csboss_battles_attempted");
    public static final ResourceLocation CSBOSS_BATTLES_WON = id("csboss_battles_won");
    public static final ResourceLocation DISTORTION_POKEMON_DEFEATED = id("distortion_pokemon_defeated");

    public static void register() {
        timeStat(TIME_IN_SAFARI);
        countStat(POKEMON_CAUGHT_SAFARI);
        countStat(POKEMON_FLED_SAFARI);
        countStat(BAIT_USED_SAFARI);
        countStat(MUD_BALLS_USED_SAFARI);
        countStat(HOOPA_PORTALS_EXPLORED);
        countStat(HOOPA_SAVES);
        timeStat(TIME_IN_DISTORTION);
        countStat(DARK_ONE_TRADES);
        timeStat(TIME_IN_UNDERGROUND);
        countStat(DIGSITES_USED);
        countStat(PERFECT_EXCAVATIONS);
        countStat(FAILED_EXCAVATIONS);
        countStat(ROTO_GLIDE_JUMPS);
        countStat(WONDER_TRADES);
        countStat(GTS_TRADES);
        countStat(UNION_ROOMS_CREATED);
        countStat(UNION_ROOMS_JOINED);
        countStat(UNION_ROOM_WRONG_CODES);
        timeStat(TIME_IN_UNION_ROOM);
        countStat(CSTRADER_TRADES);
        countStat(CSBOSS_BATTLES_ATTEMPTED);
        countStat(CSBOSS_BATTLES_WON);
        countStat(DISTORTION_POKEMON_DEFEATED);

        CobbleSafari.LOGGER.info("CobbleSafari >> Custom statistics registered!");
    }

    /** Increment the stat by 1 for the player. */
    public static void award(ServerPlayer player, ResourceLocation statId) {
        player.awardStat(Stats.CUSTOM.get(statId));
    }

    /** Increment the stat by 1 and return the new total. */
    public static int awardAndGet(ServerPlayer player, ResourceLocation statId) {
        award(player, statId);
        return value(player, statId);
    }

    /** Increment the stat by {@code amount} and return the new total. */
    public static int addAndGet(ServerPlayer player, ResourceLocation statId, int amount) {
        player.awardStat(Stats.CUSTOM.get(statId), amount);
        return value(player, statId);
    }

    public static int value(ServerPlayer player, ResourceLocation statId) {
        return player.getStats().getValue(Stats.CUSTOM.get(statId));
    }

    private static void countStat(ResourceLocation r) {
        Registry.register(BuiltInRegistries.CUSTOM_STAT, r, r);
        Stats.CUSTOM.get(r, StatFormatter.DEFAULT);
    }

    private static void timeStat(ResourceLocation r) {
        Registry.register(BuiltInRegistries.CUSTOM_STAT, r, r);
        Stats.CUSTOM.get(r, StatFormatter.TIME);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, path);
    }
}
