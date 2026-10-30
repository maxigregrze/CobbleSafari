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
        dimensions.put("cobblesafari:domedimension", domeRestrictions);

        DimensionRestrictions dungeonJumpRestrictions = new DimensionRestrictions();
        dungeonJumpRestrictions.allowBattle = false;
        dungeonJumpRestrictions.allowBlockBreaking = false;
        dungeonJumpRestrictions.allowBlockPlacing = false;
        dungeonJumpRestrictions.forbidMounting = true;
        // Bell Tower is a jumping dungeon: the Roto-Fall guard cancels every fall and Roto-Glide hands
        // out a free second storey, which is precisely what the tower is meant to charge for.
        dungeonJumpRestrictions.allowRotomSafety = false;
        dungeonJumpRestrictions.forbidElytra = true;
        dimensions.put("cobblesafari:dungeon_jump", dungeonJumpRestrictions);

        

        DimensionRestrictions dungeonUndergroundRestrictions = new DimensionRestrictions();
        dungeonUndergroundRestrictions.allowBattle = true;
        dungeonUndergroundRestrictions.allowBlockBreaking = false;
        dungeonUndergroundRestrictions.allowBlockPlacing = false;
        dungeonUndergroundRestrictions.bannedItems.add("minecraft:bone_meal");
        dimensions.put("cobblesafari:dungeon_underground", dungeonUndergroundRestrictions);

        DimensionRestrictions unionRoomRestrictions = new DimensionRestrictions();
        unionRoomRestrictions.allowBattle = true;
        unionRoomRestrictions.allowBlockBreaking = false;
        unionRoomRestrictions.allowBlockPlacing = false;
        dimensions.put("cobblesafari:unionroom", unionRoomRestrictions);

        DimensionRestrictions dungeonDistortionRestrictions = new DimensionRestrictions();
        dungeonDistortionRestrictions.allowBattle = true;
        dungeonDistortionRestrictions.allowBlockBreaking = false;
        dungeonDistortionRestrictions.allowBlockPlacing = false;
        dungeonDistortionRestrictions.bannedItems.add("minecraft:firework_rocket");
        dungeonDistortionRestrictions.forbidMounting = true;
        dungeonDistortionRestrictions.forbidElytra = true;
        dimensions.put("cobblesafari:dungeon_distortion", dungeonDistortionRestrictions);

        // Action plan 183, S20 - "the lawless pocket". A dimension shipped without its own entry
        // inherits nothing: item use, block breaking, summoning and mounting would all be allowed
        // in a world whose parent forbids them, and no server owner would think to add it. Both
        // new dimensions get the dungeon_jump profile, the strictest already in place.
        DimensionRestrictions dungeonMegalopolisRestrictions = new DimensionRestrictions();
        dungeonMegalopolisRestrictions.allowBattle = true;
        dungeonMegalopolisRestrictions.allowBlockBreaking = false;
        dungeonMegalopolisRestrictions.allowBlockPlacing = false;
        dungeonMegalopolisRestrictions.forbidMounting = true;
        dungeonMegalopolisRestrictions.forbidElytra = true;
        dimensions.put("cobblesafari:dungeon_megalopolis", dungeonMegalopolisRestrictions);

        dimensions.put("cobblesafari:dungeon_ultraspace", defaultDungeonRestrictions());

        // Action plan 187, § 4.9 - the same reasoning as S20 above, for the four sub-dimensions
        // that own their world. Their floors are scenery seen from a structure nobody may dig out
        // of, so they carry exactly the ultraspace profile.
        dimensions.put("cobblesafari:dungeon_ultra_jungle", defaultDungeonRestrictions());
        dimensions.put("cobblesafari:dungeon_ultra_desert", defaultDungeonRestrictions());
        dimensions.put("cobblesafari:dungeon_ultra_plant", defaultDungeonRestrictions());
        dimensions.put("cobblesafari:dungeon_ultra_crater", defaultDungeonRestrictions());

        // Action plan 196, § 5.5 - Hyperspace Lumiose takes the Megalopolis profile. Obstacles are
        // cleared programmatically, so allowBlockBreaking = false does not stand in their way.
        dimensions.put("cobblesafari:dungeon_paris", defaultDungeonRestrictions());
    }

    /**
     * The strict dungeon profile: battles allowed, nothing built or broken, no mounting and no
     * elytra (action plan 198, § 15). It is the {@code dungeon_jump} profile of plan 183 S20, and
     * the default handed to any dimension registered through {@code DungeonRegistrationAPI} that
     * does not already have an entry.
     *
     * <p>Returns a fresh instance every call on purpose: the restrictions object is mutable and
     * server owners edit it per dimension, so sharing one between entries would make an edit to any
     * of them an edit to all.</p>
     */
    public static DimensionRestrictions defaultDungeonRestrictions() {
        DimensionRestrictions restrictions = new DimensionRestrictions();
        restrictions.allowBattle = true;
        restrictions.allowBlockBreaking = false;
        restrictions.allowBlockPlacing = false;
        restrictions.forbidMounting = true;
        restrictions.forbidElytra = true;
        return restrictions;
    }

    public static class DimensionRestrictions {
        public List<String> bannedItems = new ArrayList<>();
        public List<String> bannedBlocks = new ArrayList<>();
        public boolean allowBattle = true;
        public boolean allowBlockBreaking = true;
        public boolean allowBlockPlacing = true;
        public boolean forbidMounting = false;
        /**
         * Rotom Phone safety features (Roto-Fall guard + Roto-Glide double jump). The phone itself
         * stays fully usable and both toggles stay operable in its settings screen; only their effect
         * is suppressed. Permissive by default, like the three allow* flags above.
         */
        public boolean allowRotomSafety = true;
        /**
         * Elytra flight of any kind - vanilla elytra, the full Ultra Recon set, another mod's
         * chestplate elytra - and with it firework propulsion, which only works while gliding
         * (action plan 198, § 15). Permissive by default, like {@code forbidMounting}.
         */
        public boolean forbidElytra = false;

        public DimensionRestrictions() {
            // Required no-arg constructor for GSON deserialization.
        }
    }
}
