package maxigregrze.cobblesafari.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DimensionalBanData {

    public Map<String, DimensionRestrictions> dimensions = new HashMap<>();

    public DimensionalBanData() {
        DimensionRestrictions domeRestrictions = new DimensionRestrictions();
        domeRestrictions.allowBattle = false;
        domeRestrictions.bannedItems.add("cobblemon:poke_ball");
        domeRestrictions.bannedItems.add("cobblemon:great_ball");
        domeRestrictions.bannedItems.add("cobblemon:ultra_ball");
        domeRestrictions.bannedItems.add("cobblemon:master_ball");
        domeRestrictions.bannedItems.add("cobblemon:premier_ball");
        domeRestrictions.bannedItems.add("cobblemon:cherish_ball");
        domeRestrictions.bannedItems.add("cobblemon:dive_ball");
        domeRestrictions.bannedItems.add("cobblemon:dusk_ball");
        domeRestrictions.bannedItems.add("cobblemon:fast_ball");
        domeRestrictions.bannedItems.add("cobblemon:friend_ball");
        domeRestrictions.bannedItems.add("cobblemon:heal_ball");
        domeRestrictions.bannedItems.add("cobblemon:heavy_ball");
        domeRestrictions.bannedItems.add("cobblemon:level_ball");
        domeRestrictions.bannedItems.add("cobblemon:love_ball");
        domeRestrictions.bannedItems.add("cobblemon:lure_ball");
        domeRestrictions.bannedItems.add("cobblemon:luxury_ball");
        domeRestrictions.bannedItems.add("cobblemon:moon_ball");
        domeRestrictions.bannedItems.add("cobblemon:nest_ball");
        domeRestrictions.bannedItems.add("cobblemon:net_ball");
        domeRestrictions.bannedItems.add("cobblemon:park_ball");
        domeRestrictions.bannedItems.add("cobblemon:quick_ball");
        domeRestrictions.bannedItems.add("cobblemon:repeat_ball");
        domeRestrictions.bannedItems.add("cobblemon:sport_ball");
        domeRestrictions.bannedItems.add("cobblemon:timer_ball");
        domeRestrictions.bannedItems.add("cobblemon:dream_ball");
        domeRestrictions.bannedItems.add("cobblemon:beast_ball");
        domeRestrictions.bannedItems.add("cobblemon:ancient_poke_ball");
        domeRestrictions.bannedItems.add("cobblemon:ancient_great_ball");
        domeRestrictions.bannedItems.add("cobblemon:ancient_ultra_ball");
        domeRestrictions.bannedItems.add("cobblemon:ancient_heavy_ball");
        domeRestrictions.bannedItems.add("cobblemon:ancient_leaden_ball");
        domeRestrictions.bannedItems.add("cobblemon:ancient_gigaton_ball");
        domeRestrictions.bannedItems.add("cobblemon:ancient_feather_ball");
        domeRestrictions.bannedItems.add("cobblemon:ancient_wing_ball");
        domeRestrictions.bannedItems.add("cobblemon:ancient_jet_ball");
        domeRestrictions.bannedItems.add("cobblemon:ancient_origin_ball");
        domeRestrictions.bannedItems.add("cobblemon:ancient_strange_ball");
        dimensions.put("cobblesafari:domedimension", domeRestrictions);

        DimensionRestrictions dungeonJumpRestrictions = new DimensionRestrictions();
        dungeonJumpRestrictions.allowBattle = false;
        dungeonJumpRestrictions.allowBlockBreaking = true;
        dungeonJumpRestrictions.allowBlockPlacing = false;
        dimensions.put("cobblesafari:dungeon_jump", dungeonJumpRestrictions);

        

        DimensionRestrictions dungeonUndergroundRestrictions = new DimensionRestrictions();
        dungeonUndergroundRestrictions.allowBattle = true;
        dungeonUndergroundRestrictions.allowBlockBreaking = false;
        dungeonUndergroundRestrictions.allowBlockPlacing = false;
        dungeonUndergroundRestrictions.bannedItems.add("minecraft:bone_meal");
        dimensions.put("cobblesafari:dungeon_underground", dungeonUndergroundRestrictions);
    }

    public static class DimensionRestrictions {
        public List<String> bannedItems = new ArrayList<>();
        public List<String> bannedBlocks = new ArrayList<>();
        public boolean allowBattle = true;
        public boolean allowBlockBreaking = true;
        public boolean allowBlockPlacing = true;

        public DimensionRestrictions() {}
    }
}
