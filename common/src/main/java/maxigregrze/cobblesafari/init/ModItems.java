package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.item.BaitItem;
import maxigregrze.cobblesafari.item.CreativeEggItem;
import maxigregrze.cobblesafari.item.CreativeFlagItem;
import maxigregrze.cobblesafari.item.IncenseItem;
import maxigregrze.cobblesafari.item.MudBallItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

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

    public static final Item TAB_ICON = new Item(new Item.Properties());

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

        registerItem("auspiciouspokeball", TAB_ICON);
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
    }
}
