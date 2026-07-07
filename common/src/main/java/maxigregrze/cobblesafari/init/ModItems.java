package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.item.BaitItem;
import maxigregrze.cobblesafari.item.BalmItem;
import maxigregrze.cobblesafari.item.CreativeEggItem;
import maxigregrze.cobblesafari.item.CreativeFlagItem;
import maxigregrze.cobblesafari.item.IncenseItem;
import maxigregrze.cobblesafari.item.LuckyMiningHelmetItem;
import maxigregrze.cobblesafari.item.MudBallItem;
import maxigregrze.cobblesafari.item.RotomAppUnlockItem;
import maxigregrze.cobblesafari.item.RotomPhoneItem;
import maxigregrze.cobblesafari.item.RotomSkinUnlockItem;
import maxigregrze.cobblesafari.item.TinkhammerItem;
import maxigregrze.cobblesafari.item.WonderTradeTicketItem;
import maxigregrze.cobblesafari.platform.Services;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import maxigregrze.cobblesafari.item.donut.DonutFlavorComponent;
import maxigregrze.cobblesafari.item.donut.DonutItem;
import maxigregrze.cobblesafari.item.donut.DonutMainFlavor;
import maxigregrze.cobblesafari.item.donut.DungeonDonutItem;
import maxigregrze.cobblesafari.item.donut.SurpriseDonutItem;
import maxigregrze.cobblesafari.item.hyperberries.HyperBerryEnigmaItem;
import maxigregrze.cobblesafari.item.hyperberries.HyperBerryEVItem;
import maxigregrze.cobblesafari.item.hyperberries.HyperBerryIVItem;
import maxigregrze.cobblesafari.item.hyperberries.HyperBerryStarfItem;
import maxigregrze.cobblesafari.item.donut.DungeonIngredientItem;
import maxigregrze.cobblesafari.item.redchain.RedChainCoreItem;
import maxigregrze.cobblesafari.item.redchain.RedChainItem;
import maxigregrze.cobblesafari.item.redchainrandom.RedChainFragmentItem;
import maxigregrze.cobblesafari.item.redchainrandom.RedChainRandomBallItem;
import maxigregrze.cobblesafari.item.redchainrandom.RedChainRandomEVItem;
import maxigregrze.cobblesafari.item.redchainrandom.RedChainRandomGenderItem;
import maxigregrze.cobblesafari.item.redchainrandom.RedChainRandomIVItem;
import maxigregrze.cobblesafari.item.redchainrandom.RedChainRandomLevelItem;
import maxigregrze.cobblesafari.item.redchainrandom.RedChainRandomShinyItem;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ModItems {

    private ModItems() {}

    private static final String ID_LUCKY_MINING_HELMET = "lucky_mining_helmet";

    public static final String[] BIOME_TYPES = {
        "bug", "dark", "dragon", "electric", "fairy", "fighting",
        "fire", "flying", "ghost", "grass", "ground", "ice",
        "normal", "poison", "psychic", "rock", "steel", "water"
    };

    public static final Item WILD_EGG_BASE = new Item(new Item.Properties());
    public static final Map<String, Item> WILD_EGGS = new LinkedHashMap<>();

    // Hyperspace boats (plan 140 § 16). stacksTo(1), like vanilla boats.
    public static final Item HYPERSPACE_BOAT = new maxigregrze.cobblesafari.item.HyperspaceBoatItem(
            maxigregrze.cobblesafari.entity.HyperspaceBoatEntity::new, new Item.Properties().stacksTo(1));
    public static final Item HYPERSPACE_CHEST_BOAT = new maxigregrze.cobblesafari.item.HyperspaceBoatItem(
            maxigregrze.cobblesafari.entity.HyperspaceChestBoatEntity::new, new Item.Properties().stacksTo(1));

    public static final Item AURORA_DIAL = new Item(new Item.Properties());
    public static final Item BLOOD_DIAL = new Item(new Item.Properties());
    public static final Item BLUE_DIAL = new Item(new Item.Properties());
    public static final Item HARVEST_DIAL = new Item(new Item.Properties());
    public static final Item MOON_CALENDAR = new Item(new Item.Properties());
    public static final Item PC = new Item(new Item.Properties());

    public static Item SHINY_INCENSE;
    public static Item SUPER_SHINY_INCENSE;
    public static Item ULTRA_SHINY_INCENSE;
    public static Item REPEL;

    public static Item PERFUME_UNCOMMON;
    public static Item PERFUME_RARE;
    public static Item PERFUME_ULTRARARE;

    public static final Item SPHERE_BLUE_S = new Item(new Item.Properties());
    public static final Item SPHERE_BLUE_L = new Item(new Item.Properties());
    public static final Item SPHERE_RED_S = new Item(new Item.Properties());
    public static final Item SPHERE_RED_L = new Item(new Item.Properties());
    public static final Item SPHERE_GREEN_S = new Item(new Item.Properties());
    public static final Item SPHERE_GREEN_L = new Item(new Item.Properties());
    public static final Item SPHERE_PALE_S = new Item(new Item.Properties());
    public static final Item SPHERE_PALE_L = new Item(new Item.Properties());
    public static final Item SPHERE_PRISM_S = new Item(new Item.Properties());
    public static final Item SPHERE_PRISM_L = new Item(new Item.Properties());

    public static final Item HEART_SCALE = new Item(new Item.Properties());
    public static final Item STAR_PIECE = new Item(new Item.Properties());

    public static final Item FOSSIL_RANDOM = new Item(new Item.Properties());
    public static final Item FOSSIL_PERFECT = new Item(new Item.Properties());
    public static final Item FOSSIL_IV_MAX = new Item(new Item.Properties());
    public static final Item FOSSIL_HIDDEN_ABILITY = new Item(new Item.Properties());
    public static final Item FOSSIL_SHINY = new Item(new Item.Properties());

    public static final Item TICKET_SAFARI = new Item(new Item.Properties());
    public static final Item TICKET_DUNGEON = new Item(new Item.Properties());
    public static final Item TICKET_WONDERTRADE = new WonderTradeTicketItem(new Item.Properties());

    public static final Item FLAG_REGULAR = new Item(new Item.Properties().stacksTo(1));
    public static final Item FLAG_BRONZE = new Item(new Item.Properties().stacksTo(1));
    public static final Item FLAG_SILVER = new Item(new Item.Properties().stacksTo(1));
    public static final Item FLAG_GOLD = new Item(new Item.Properties().stacksTo(1));
    public static final Item FLAG_PLATINUM = new Item(new Item.Properties().stacksTo(1));
    public static final Item FLAG_CREATIVE = new CreativeFlagItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final Item EGG_CREATIVE = new CreativeEggItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final Item ROTOM_PHONE = new RotomPhoneItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    // Rotie Earpiece — accessory variant of the Rotom Phone (worn in the Accessories "hat" slot).
    // Both items are always registered; recipe + creative-tab visibility are gated on Accessories.
    public static final Item EMPTY_EARPIECE = new maxigregrze.cobblesafari.item.EmptyEarpieceItem(
            new Item.Properties().stacksTo(16));
    public static final Item ROTOM_EARPIECE = new maxigregrze.cobblesafari.item.RotomEarpieceItem(
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    // Rotom Phone app-unlock consumables (target app set at registration; "*" = every non-default app).
    public static final Item ROTOM_APP_UNLOCK_GTS = new RotomAppUnlockItem(new Item.Properties().stacksTo(16), "gtsApp");
    public static final Item ROTOM_APP_UNLOCK_WONDER = new RotomAppUnlockItem(new Item.Properties().stacksTo(16), "wonderApp");
    public static final Item ROTOM_APP_UNLOCK_UNION = new RotomAppUnlockItem(new Item.Properties().stacksTo(16), "unionApp");
    public static final Item ROTOM_APP_UNLOCK_SKIN = new RotomAppUnlockItem(new Item.Properties().stacksTo(16), "skinApp");
    public static final Item ROTOM_APP_UNLOCK_SETTINGS = new RotomAppUnlockItem(new Item.Properties().stacksTo(16), "settingsApp");
    public static final Item ROTOM_APP_UNLOCK_ALL = new RotomAppUnlockItem(
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE), RotomAppUnlockItem.ALL);
    // Dynamic skin-unlock disc: target skin id carried per-stack in the SKIN_UNLOCK_TARGET component.
    public static final Item ROTOM_SKIN_UNLOCK = new RotomSkinUnlockItem(new Item.Properties().stacksTo(16), null);
    public static final Item ROTOM_SKIN_UNLOCK_ALL = new RotomSkinUnlockItem(
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE), RotomSkinUnlockItem.ALL);

    /**
     * Ids of the mod's bundled skins flagged {@code addUnlockItem} — one dynamic disc per id is shown in
     * the creative tab (each stack tagged with the SKIN_UNLOCK_TARGET component). Populated at init from
     * the bundled skin JSONs; empty when no bundled skin is disc-obtainable (so no blank disc is shown).
     */
    public static final List<String> SKIN_UNLOCK_TARGETS = new ArrayList<>();

    public static final Item MUD_BALL = new MudBallItem(new Item.Properties().stacksTo(64));
    public static final Item TINKAGEAR = new Item(new Item.Properties());
    public static final Item TINKHAMMER = new TinkhammerItem(
            new Item.Properties().attributes(DiggerItem.createAttributes(Tiers.DIAMOND, 5.0F, -3.0F)));
    public static final Item BAIT = new BaitItem(new Item.Properties().stacksTo(64));
    public static final Item BALM = new BalmItem(new Item.Properties().stacksTo(16));
    public static final Item BALM_DISTORTION = new BalmItem(new Item.Properties().stacksTo(16));

    public static final Item REDCHAIN_RANDOM_BALL = new RedChainRandomBallItem(new Item.Properties().stacksTo(16), "redchain_random_ball");
    public static final Item REDCHAIN_RANDOM_EV = new RedChainRandomEVItem(new Item.Properties().stacksTo(16), "redchain_random_ev");
    public static final Item REDCHAIN_RANDOM_GENDER = new RedChainRandomGenderItem(new Item.Properties().stacksTo(16), "redchain_random_gender");
    public static final Item REDCHAIN_RANDOM_IV = new RedChainRandomIVItem(new Item.Properties().stacksTo(16), "redchain_random_iv");
    public static final Item REDCHAIN_RANDOM_LEVEL = new RedChainRandomLevelItem(new Item.Properties().stacksTo(16), "redchain_random_level");
    public static final Item REDCHAIN_RANDOM_SHINY = new RedChainRandomShinyItem(new Item.Properties().stacksTo(16), "redchain_random_shiny");
    public static final Item REDCHAIN_FRAGMENT = new RedChainFragmentItem(new Item.Properties().stacksTo(64));
    public static final Item RED_CHAIN_CORE = new RedChainCoreItem(new Item.Properties().stacksTo(64));
    public static final Item RED_CHAIN = new RedChainItem(new Item.Properties().stacksTo(16));

    public static final Item HYPERBERRY_ENIGMA = new HyperBerryEnigmaItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_enigma");
    public static final Item HYPERBERRY_TAMATO = new HyperBerryEVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_tamato", Stats.SPEED);
    public static final Item HYPERBERRY_GREPA = new HyperBerryEVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_grepa", Stats.SPECIAL_DEFENCE);
    public static final Item HYPERBERRY_HONDEW = new HyperBerryEVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_hondew", Stats.SPECIAL_ATTACK);
    public static final Item HYPERBERRY_QUALOT = new HyperBerryEVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_qualot", Stats.DEFENCE);
    public static final Item HYPERBERRY_KELPSY = new HyperBerryEVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_kelpsy", Stats.ATTACK);
    public static final Item HYPERBERRY_POMEG = new HyperBerryEVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_pomeg", Stats.HP);
    public static final Item HYPERBERRY_SALAC = new HyperBerryIVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_salac", Stats.SPEED);
    public static final Item HYPERBERRY_APICOT = new HyperBerryIVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_apicot", Stats.SPECIAL_DEFENCE);
    public static final Item HYPERBERRY_PETAYA = new HyperBerryIVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_petaya", Stats.SPECIAL_ATTACK);
    public static final Item HYPERBERRY_GANLON = new HyperBerryIVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_ganlon", Stats.DEFENCE);
    public static final Item HYPERBERRY_LIECHI = new HyperBerryIVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_liechi", Stats.ATTACK);
    public static final Item HYPERBERRY_STARF = new HyperBerryStarfItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_starf");
    public static final Item HYPERBERRY_LANSAT = new HyperBerryIVItem(new Item.Properties().rarity(Rarity.RARE), "hyperberry_lansat", Stats.HP);
    // Donut system
    public static final Item DONUT_MIX = new Item(new Item.Properties());
    public static final Item BUTTER_LUMINOSIAN = new Item(new Item.Properties());
    public static final Item BUTTER_GREAT = new Item(new Item.Properties());
    public static final Item BUTTER_AMAZING = new Item(new Item.Properties());
    public static final Item BUTTER_SUPREME = new Item(new Item.Properties());
    public static final Item BUTTER_HYPERSPACE = new Item(new Item.Properties());
    private static final FoodProperties DONUT_FOOD_PROPERTIES = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.25f)
            .alwaysEdible()
            .build();

    public static final Item DONUT = new DonutItem(new Item.Properties().stacksTo(16).food(DONUT_FOOD_PROPERTIES));
    public static final Item DONUT_RANDOM = new SurpriseDonutItem(new Item.Properties().stacksTo(16), null, 0);
    public static final Map<String, Item> DONUT_RANDOMS = new LinkedHashMap<>();

    static {
        for (DonutMainFlavor flavor : DonutMainFlavor.values()) {
            for (int t = 0; t <= DonutFlavorComponent.MAX_TIER; t++) {
                DONUT_RANDOMS.put(t + "_" + flavor.getSerializedName(),
                        new SurpriseDonutItem(new Item.Properties().stacksTo(16), flavor, t));
            }
        }
    }
    public static final Item INGREDIENT_DUNGEON_PORTAL = new DungeonIngredientItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final Item INGREDIENT_DUNGEON_PARIS = new DungeonIngredientItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final Item INGREDIENT_DUNGEON_DISTORTION = new DungeonIngredientItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final Item INGREDIENT_DUNGEON_UNDERGROUND = new DungeonIngredientItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final Item DONUT_DUNGEON_PORTAL = new DungeonDonutItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), null);
    public static final Item DONUT_DUNGEON_PARIS = new DungeonDonutItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), null);
    public static final Item DONUT_DUNGEON_DISTORTION = new DungeonDonutItem(
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), "cobblesafari:dungeon_distortion");
    public static final Item DONUT_DUNGEON_UNDERGROUND = new DungeonDonutItem(
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), "cobblesafari:dungeon_underground");

    public static Item LUCKY_MINING_HELMET;

    public static final List<Item> BATCH_ITEMS = new ArrayList<>();

    static {
        for (String type : BIOME_TYPES) {
            WILD_EGGS.put(type, new Item(new Item.Properties()));
        }
    }

    public static Item getWildEgg(String biomeType) {
        if (biomeType == null || biomeType.isEmpty()) {
            return WILD_EGG_BASE;
        }
        return WILD_EGGS.getOrDefault(biomeType, WILD_EGG_BASE);
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, name), item);
    }

    /**
     * Scans the mod's bundled skin definitions and records the ids flagged {@code "addUnlockItem": true}
     * into {@link #SKIN_UNLOCK_TARGETS}, sorted. Runs at mod-init (both sides) so the creative tab can
     * list one dynamic disc per disc-obtainable bundled skin, deterministically and without a server sync.
     */
    private static void scanSkinUnlockTargets() {
        SKIN_UNLOCK_TARGETS.clear();
        Path dir = Services.PLATFORM.getBundledResourceDir("data/" + CobbleSafari.MOD_ID + "/rotomphone_skins");
        if (dir == null) {
            return;
        }
        try (Stream<Path> files = Files.walk(dir, 1)) {
            files.filter(p -> p.getFileName() != null && p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(ModItems::collectSkinUnlockTarget);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("[RotomPhone] Failed to scan bundled skins for unlock discs", e);
        }
        CobbleSafari.LOGGER.info("Found {} disc-obtainable rotomphone skins", SKIN_UNLOCK_TARGETS.size());
    }

    private static void collectSkinUnlockTarget(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("addUnlockItem") && json.get("addUnlockItem").getAsBoolean() && json.has("id")) {
                SKIN_UNLOCK_TARGETS.add(json.get("id").getAsString());
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[RotomPhone] Failed to read bundled skin {}", file, e);
        }
    }

    public static void register() {
        CobbleSafari.LOGGER.info("Registering items for " + CobbleSafari.MOD_ID);

        SHINY_INCENSE = new IncenseItem(new Item.Properties().stacksTo(16),
                ModEffects.SHINY_BOOST.holder, SpawnBoostConfig.data.durationSettings.shinyBoostDuration);
        SUPER_SHINY_INCENSE = new IncenseItem(new Item.Properties().stacksTo(16),
                ModEffects.GREAT_SHINY_BOOST.holder, SpawnBoostConfig.data.durationSettings.superShinyBoostDuration);
        ULTRA_SHINY_INCENSE = new IncenseItem(new Item.Properties().stacksTo(16),
                ModEffects.ULTRA_SHINY_BOOST.holder, SpawnBoostConfig.data.durationSettings.ultraShinyBoostDuration);
        REPEL = new IncenseItem(new Item.Properties().stacksTo(16),
                ModEffects.REPEL.holder, SpawnBoostConfig.data.durationSettings.repelDuration);

        PERFUME_UNCOMMON = new IncenseItem(new Item.Properties().stacksTo(16),
                ModEffects.UNCOMMON_BOOST.holder, SpawnBoostConfig.data.durationSettings.uncommonBoostDuration);
        PERFUME_RARE = new IncenseItem(new Item.Properties().stacksTo(16),
                ModEffects.RARE_BOOST.holder, SpawnBoostConfig.data.durationSettings.rareBoostDuration);
        PERFUME_ULTRARARE = new IncenseItem(new Item.Properties().stacksTo(16),
                ModEffects.ULTRA_RARE_BOOST.holder, SpawnBoostConfig.data.durationSettings.ultraRareBoostDuration);

        registerItem("hyperspace_boat", HYPERSPACE_BOAT);
        registerItem("hyperspace_chest_boat", HYPERSPACE_CHEST_BOAT);

        registerItem("wildegg", WILD_EGG_BASE);

        for (Map.Entry<String, Item> entry : WILD_EGGS.entrySet()) {
            String type = entry.getKey();
            Item item = entry.getValue();
            registerItem("wildegg_" + type, item);
        }

        registerItem("auroradial", AURORA_DIAL);
        registerItem("blooddial", BLOOD_DIAL);
        registerItem("bluedial", BLUE_DIAL);
        registerItem("harvestdial", HARVEST_DIAL);
        registerItem("mooncalendar", MOON_CALENDAR);
        registerItem("pc", PC);
        registerItem("repel", REPEL);

        registerItem("shinyincense", SHINY_INCENSE);
        registerItem("greatshinyincense", SUPER_SHINY_INCENSE);
        registerItem("ultrashinyincense", ULTRA_SHINY_INCENSE);

        registerItem("perfume_uncommon", PERFUME_UNCOMMON);
        registerItem("perfume_rare", PERFUME_RARE);
        registerItem("perfume_ultrarare", PERFUME_ULTRARARE);

        registerItem("sphere_blue_s", SPHERE_BLUE_S);
        registerItem("sphere_blue_l", SPHERE_BLUE_L);
        registerItem("sphere_red_s", SPHERE_RED_S);
        registerItem("sphere_red_l", SPHERE_RED_L);
        registerItem("sphere_green_s", SPHERE_GREEN_S);
        registerItem("sphere_green_l", SPHERE_GREEN_L);
        registerItem("sphere_pale_s", SPHERE_PALE_S);
        registerItem("sphere_pale_l", SPHERE_PALE_L);
        registerItem("sphere_prism_s", SPHERE_PRISM_S);
        registerItem("sphere_prism_l", SPHERE_PRISM_L);

        registerItem("heartscale", HEART_SCALE);
        registerItem("starpiece", STAR_PIECE);

        registerItem("ticket_safari", TICKET_SAFARI);
        registerItem("ticket_dungeon", TICKET_DUNGEON);
        registerItem("ticket_wondertrade", TICKET_WONDERTRADE);

        registerItem("fossil_random", FOSSIL_RANDOM);
        registerItem("fossil_perfect", FOSSIL_PERFECT);
        registerItem("fossil_iv_max", FOSSIL_IV_MAX);
        registerItem("fossil_hidden_ability", FOSSIL_HIDDEN_ABILITY);
        registerItem("fossil_shiny", FOSSIL_SHINY);

        registerItem("underground_flag_regular", FLAG_REGULAR);
        registerItem("underground_flag_bronze", FLAG_BRONZE);
        registerItem("underground_flag_silver", FLAG_SILVER);
        registerItem("underground_flag_gold", FLAG_GOLD);
        registerItem("underground_flag_platinum", FLAG_PLATINUM);
        registerItem("underground_flag_creative", FLAG_CREATIVE);

        registerItem("egg_creative", EGG_CREATIVE);

        registerItem("rotomphone", ROTOM_PHONE);
        registerItem("emptyearpiece", EMPTY_EARPIECE);
        registerItem("rotom_earpiece", ROTOM_EARPIECE);
        registerItem("rotom_app_unlock_gts", ROTOM_APP_UNLOCK_GTS);
        registerItem("rotom_app_unlock_wonder", ROTOM_APP_UNLOCK_WONDER);
        registerItem("rotom_app_unlock_union", ROTOM_APP_UNLOCK_UNION);
        registerItem("rotom_app_unlock_skin", ROTOM_APP_UNLOCK_SKIN);
        registerItem("rotom_app_unlock_settings", ROTOM_APP_UNLOCK_SETTINGS);
        registerItem("rotom_app_unlock_all", ROTOM_APP_UNLOCK_ALL);
        registerItem("rotom_skin_unlock", ROTOM_SKIN_UNLOCK);
        registerItem("rotom_skin_unlock_all", ROTOM_SKIN_UNLOCK_ALL);
        scanSkinUnlockTargets();

        registerItem("mud_ball", MUD_BALL);
        registerItem("tinkagear", TINKAGEAR);
        registerItem("tinkhammer", TINKHAMMER);
        registerItem("bait", BAIT);
        registerItem("balm", BALM);
        registerItem("balm_distortion", BALM_DISTORTION);

        registerItem("redchain_random_ball", REDCHAIN_RANDOM_BALL);
        registerItem("redchain_random_ev", REDCHAIN_RANDOM_EV);
        registerItem("redchain_random_gender", REDCHAIN_RANDOM_GENDER);
        registerItem("redchain_random_iv", REDCHAIN_RANDOM_IV);
        registerItem("redchain_random_level", REDCHAIN_RANDOM_LEVEL);
        registerItem("redchain_random_shiny", REDCHAIN_RANDOM_SHINY);
        registerItem("redchain_fragment", REDCHAIN_FRAGMENT);
        registerItem("red_chain_core", RED_CHAIN_CORE);
        registerItem("red_chain", RED_CHAIN);

        registerItem("hyperberry_enigma", HYPERBERRY_ENIGMA);
        registerItem("hyperberry_tamato", HYPERBERRY_TAMATO);
        registerItem("hyperberry_grepa", HYPERBERRY_GREPA);
        registerItem("hyperberry_hondew", HYPERBERRY_HONDEW);
        registerItem("hyperberry_qualot", HYPERBERRY_QUALOT);
        registerItem("hyperberry_kelpsy", HYPERBERRY_KELPSY);
        registerItem("hyperberry_pomeg", HYPERBERRY_POMEG);
        registerItem("hyperberry_salac", HYPERBERRY_SALAC);
        registerItem("hyperberry_apicot", HYPERBERRY_APICOT);
        registerItem("hyperberry_petaya", HYPERBERRY_PETAYA);
        registerItem("hyperberry_ganlon", HYPERBERRY_GANLON);
        registerItem("hyperberry_liechi", HYPERBERRY_LIECHI);
        registerItem("hyperberry_starf", HYPERBERRY_STARF);
        registerItem("hyperberry_lansat", HYPERBERRY_LANSAT);

        registerItem("donut_mix", DONUT_MIX);
        registerItem("butter_luminosian", BUTTER_LUMINOSIAN);
        registerItem("butter_great", BUTTER_GREAT);
        registerItem("butter_amazing", BUTTER_AMAZING);
        registerItem("butter_supreme", BUTTER_SUPREME);
        registerItem("butter_hyperspace", BUTTER_HYPERSPACE);
        registerItem("donut", DONUT);
        registerItem("donut_random", DONUT_RANDOM);
        for (Map.Entry<String, Item> entry : DONUT_RANDOMS.entrySet()) {
            registerItem("donut_random_" + entry.getKey(), entry.getValue());
        }
        registerItem("ingredient_dungeon_portal", INGREDIENT_DUNGEON_PORTAL);
        registerItem("ingredient_dungeon_paris", INGREDIENT_DUNGEON_PARIS);
        registerItem("ingredient_dungeon_distortion", INGREDIENT_DUNGEON_DISTORTION);
        registerItem("ingredient_dungeon_underground", INGREDIENT_DUNGEON_UNDERGROUND);
        registerItem("donut_dungeon_portal", DONUT_DUNGEON_PORTAL);
        registerItem("donut_dungeon_paris", DONUT_DUNGEON_PARIS);
        registerItem("donut_dungeon_distortion", DONUT_DUNGEON_DISTORTION);
        registerItem("donut_dungeon_underground", DONUT_DUNGEON_UNDERGROUND);

        Holder<ArmorMaterial> luckyHelmetMaterial = Registry.registerForHolder(
                BuiltInRegistries.ARMOR_MATERIAL,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, ID_LUCKY_MINING_HELMET),
                new ArmorMaterial(
                        Map.of(ArmorItem.Type.HELMET, 2),
                        25,
                        SoundEvents.ARMOR_EQUIP_CHAIN,
                        () -> Ingredient.of(BuiltInRegistries.ITEM.getOptional(
                                ResourceLocation.fromNamespaceAndPath("cobblemon", "hard_stone")
                        ).orElse(Items.IRON_INGOT)),
                        List.of(new ArmorMaterial.Layer(
                                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, ID_LUCKY_MINING_HELMET)
                        )),
                        0.0f,
                        0.0f
                )
        );

        LUCKY_MINING_HELMET = registerItem(ID_LUCKY_MINING_HELMET,
                new LuckyMiningHelmetItem(luckyHelmetMaterial, new Item.Properties().stacksTo(1)));

        BATCH_ITEMS.add(AURORA_DIAL);
        BATCH_ITEMS.add(BLOOD_DIAL);
        BATCH_ITEMS.add(BLUE_DIAL);
        BATCH_ITEMS.add(HARVEST_DIAL);
        BATCH_ITEMS.add(MOON_CALENDAR);
        BATCH_ITEMS.add(PC);
        BATCH_ITEMS.add(SHINY_INCENSE);
        BATCH_ITEMS.add(SUPER_SHINY_INCENSE);
        BATCH_ITEMS.add(ULTRA_SHINY_INCENSE);
        BATCH_ITEMS.add(REPEL);
        BATCH_ITEMS.add(PERFUME_UNCOMMON);
        BATCH_ITEMS.add(PERFUME_RARE);
        BATCH_ITEMS.add(PERFUME_ULTRARARE);
        BATCH_ITEMS.add(SPHERE_BLUE_S);
        BATCH_ITEMS.add(SPHERE_BLUE_L);
        BATCH_ITEMS.add(SPHERE_RED_S);
        BATCH_ITEMS.add(SPHERE_RED_L);
        BATCH_ITEMS.add(SPHERE_GREEN_S);
        BATCH_ITEMS.add(SPHERE_GREEN_L);
        BATCH_ITEMS.add(SPHERE_PALE_S);
        BATCH_ITEMS.add(SPHERE_PALE_L);
        BATCH_ITEMS.add(SPHERE_PRISM_S);
        BATCH_ITEMS.add(SPHERE_PRISM_L);
        BATCH_ITEMS.add(HEART_SCALE);
        BATCH_ITEMS.add(STAR_PIECE);
        BATCH_ITEMS.add(FOSSIL_RANDOM);
        BATCH_ITEMS.add(FOSSIL_PERFECT);
        BATCH_ITEMS.add(FOSSIL_IV_MAX);
        BATCH_ITEMS.add(FOSSIL_HIDDEN_ABILITY);
        BATCH_ITEMS.add(FOSSIL_SHINY);
        BATCH_ITEMS.add(MUD_BALL);
        BATCH_ITEMS.add(BAIT);
        BATCH_ITEMS.add(REDCHAIN_RANDOM_BALL);
        BATCH_ITEMS.add(REDCHAIN_RANDOM_EV);
        BATCH_ITEMS.add(REDCHAIN_RANDOM_GENDER);
        BATCH_ITEMS.add(REDCHAIN_RANDOM_IV);
        BATCH_ITEMS.add(REDCHAIN_RANDOM_LEVEL);
        BATCH_ITEMS.add(REDCHAIN_RANDOM_SHINY);
        BATCH_ITEMS.add(REDCHAIN_FRAGMENT);
        BATCH_ITEMS.add(RED_CHAIN_CORE);
        BATCH_ITEMS.add(RED_CHAIN);
        BATCH_ITEMS.add(HYPERBERRY_ENIGMA);
        BATCH_ITEMS.add(HYPERBERRY_TAMATO);
        BATCH_ITEMS.add(HYPERBERRY_GREPA);
        BATCH_ITEMS.add(HYPERBERRY_HONDEW);
        BATCH_ITEMS.add(HYPERBERRY_QUALOT);
        BATCH_ITEMS.add(HYPERBERRY_KELPSY);
        BATCH_ITEMS.add(HYPERBERRY_POMEG);
        BATCH_ITEMS.add(HYPERBERRY_SALAC);
        BATCH_ITEMS.add(HYPERBERRY_APICOT);
        BATCH_ITEMS.add(HYPERBERRY_PETAYA);
        BATCH_ITEMS.add(HYPERBERRY_GANLON);
        BATCH_ITEMS.add(HYPERBERRY_LIECHI);
        BATCH_ITEMS.add(HYPERBERRY_STARF);
        BATCH_ITEMS.add(HYPERBERRY_LANSAT);
        BATCH_ITEMS.add(DONUT_MIX);
        BATCH_ITEMS.add(BUTTER_LUMINOSIAN);
        BATCH_ITEMS.add(BUTTER_GREAT);
        BATCH_ITEMS.add(BUTTER_AMAZING);
        BATCH_ITEMS.add(BUTTER_SUPREME);
        BATCH_ITEMS.add(BUTTER_HYPERSPACE);
        BATCH_ITEMS.add(DONUT);
        BATCH_ITEMS.add(DONUT_RANDOM);
        BATCH_ITEMS.addAll(DONUT_RANDOMS.values());
        BATCH_ITEMS.add(INGREDIENT_DUNGEON_PORTAL);
        BATCH_ITEMS.add(INGREDIENT_DUNGEON_PARIS);
        BATCH_ITEMS.add(INGREDIENT_DUNGEON_DISTORTION);
        BATCH_ITEMS.add(INGREDIENT_DUNGEON_UNDERGROUND);
        BATCH_ITEMS.add(DONUT_DUNGEON_PORTAL);
        BATCH_ITEMS.add(DONUT_DUNGEON_PARIS);
        BATCH_ITEMS.add(DONUT_DUNGEON_DISTORTION);
        BATCH_ITEMS.add(DONUT_DUNGEON_UNDERGROUND);
    }
}
