package maxigregrze.cobblesafari.init;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.item.BaitItem;
import maxigregrze.cobblesafari.item.CreativeEggItem;
import maxigregrze.cobblesafari.item.CreativeFlagItem;
import maxigregrze.cobblesafari.item.IncenseItem;
import maxigregrze.cobblesafari.item.LuckyMiningHelmetItem;
import maxigregrze.cobblesafari.item.MudBallItem;
import maxigregrze.cobblesafari.item.hyperberries.HyperBerryEVItem;
import maxigregrze.cobblesafari.item.hyperberries.HyperBerryFriendshipItem;
import maxigregrze.cobblesafari.item.hyperberries.HyperBerryIVItem;
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
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.function.Supplier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModItems {

    private ModItems() {}

    public static final String[] BIOME_TYPES = {
        "bug", "dark", "dragon", "electric", "fairy", "fighting",
        "fire", "flying", "ghost", "grass", "ground", "ice",
        "normal", "poison", "psychic", "rock", "steel", "water"
    };

    public static final Item WILD_EGG_BASE = new Item(new Item.Properties());
    public static final Map<String, Item> WILD_EGGS = new LinkedHashMap<>();

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

    public static final Item AUSPICIOUS_POKEBALL = new Item(new Item.Properties());

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

    public static final Item FLAG_REGULAR = new Item(new Item.Properties().stacksTo(1));
    public static final Item FLAG_BRONZE = new Item(new Item.Properties().stacksTo(1));
    public static final Item FLAG_SILVER = new Item(new Item.Properties().stacksTo(1));
    public static final Item FLAG_GOLD = new Item(new Item.Properties().stacksTo(1));
    public static final Item FLAG_PLATINUM = new Item(new Item.Properties().stacksTo(1));
    public static final Item FLAG_CREATIVE = new CreativeFlagItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final Item EGG_CREATIVE = new CreativeEggItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final Item MUD_BALL = new MudBallItem(new Item.Properties().stacksTo(64));
    public static final Item BAIT = new BaitItem(new Item.Properties().stacksTo(64));

    public static final Item REDCHAIN_RANDOM_BALL = new RedChainRandomBallItem(new Item.Properties().stacksTo(1), "redchain_random_ball");
    public static final Item REDCHAIN_RANDOM_EV = new RedChainRandomEVItem(new Item.Properties().stacksTo(1), "redchain_random_ev");
    public static final Item REDCHAIN_RANDOM_GENDER = new RedChainRandomGenderItem(new Item.Properties().stacksTo(1), "redchain_random_gender");
    public static final Item REDCHAIN_RANDOM_IV = new RedChainRandomIVItem(new Item.Properties().stacksTo(1), "redchain_random_iv");
    public static final Item REDCHAIN_RANDOM_LEVEL = new RedChainRandomLevelItem(new Item.Properties().stacksTo(1), "redchain_random_level");
    public static final Item REDCHAIN_RANDOM_SHINY = new RedChainRandomShinyItem(new Item.Properties().stacksTo(1), "redchain_random_shiny");
    public static final Item REDCHAIN_FRAGMENT = new RedChainFragmentItem(new Item.Properties().stacksTo(64));

    public static final Item HYPERBERRY_ENIGMA = new HyperBerryFriendshipItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_enigma");
    public static final Item HYPERBERRY_TAMATO = new HyperBerryEVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_tamato", Stats.SPEED);
    public static final Item HYPERBERRY_GREPA = new HyperBerryEVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_grepa", Stats.SPECIAL_DEFENCE);
    public static final Item HYPERBERRY_HONDEW = new HyperBerryEVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_hondew", Stats.SPECIAL_ATTACK);
    public static final Item HYPERBERRY_QUALOT = new HyperBerryEVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_qualot", Stats.DEFENCE);
    public static final Item HYPERBERRY_KELPSY = new HyperBerryEVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_kelpsy", Stats.ATTACK);
    public static final Item HYPERBERRY_POMEG = new HyperBerryEVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_pomeg", Stats.HP);
    public static final Item HYPERBERRY_SALAC = new HyperBerryIVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_salac", Stats.SPEED);
    public static final Item HYPERBERRY_APICOT = new HyperBerryIVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_apicot", Stats.SPECIAL_DEFENCE);
    public static final Item HYPERBERRY_PETAYA = new HyperBerryIVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_petaya", Stats.SPECIAL_ATTACK);
    public static final Item HYPERBERRY_GANLON = new HyperBerryIVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_ganlon", Stats.DEFENCE);
    public static final Item HYPERBERRY_LIECHI = new HyperBerryIVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_liechi", Stats.ATTACK);
    public static final Item HYPERBERRY_STARF = new HyperBerryIVItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "hyperberry_starf", Stats.HP);

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

        registerItem("auspiciouspokeball", AUSPICIOUS_POKEBALL);
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

        registerItem("mud_ball", MUD_BALL);
        registerItem("bait", BAIT);
        registerItem("redchain_random_ball", REDCHAIN_RANDOM_BALL);
        registerItem("redchain_random_ev", REDCHAIN_RANDOM_EV);
        registerItem("redchain_random_gender", REDCHAIN_RANDOM_GENDER);
        registerItem("redchain_random_iv", REDCHAIN_RANDOM_IV);
        registerItem("redchain_random_level", REDCHAIN_RANDOM_LEVEL);
        registerItem("redchain_random_shiny", REDCHAIN_RANDOM_SHINY);
        registerItem("redchain_fragment", REDCHAIN_FRAGMENT);
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

        Holder<ArmorMaterial> luckyHelmetMaterial = Registry.registerForHolder(
                BuiltInRegistries.ARMOR_MATERIAL,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "lucky_mining_helmet"),
                new ArmorMaterial(
                        Map.of(ArmorItem.Type.HELMET, 2),
                        25,
                        SoundEvents.ARMOR_EQUIP_CHAIN,
                        () -> Ingredient.of(BuiltInRegistries.ITEM.getOptional(
                                ResourceLocation.fromNamespaceAndPath("cobblemon", "hard_stone")
                        ).orElse(Items.IRON_INGOT)),
                        List.of(new ArmorMaterial.Layer(
                                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "lucky_mining_helmet")
                        )),
                        0.0f,
                        0.0f
                )
        );

        LUCKY_MINING_HELMET = registerItem("lucky_mining_helmet",
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
    }
}
