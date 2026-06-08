package maxigregrze.cobblesafari.advancement;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers all custom advancement criterion triggers (cf. action plan 94 §2.2).
 * Called from {@link CobbleSafari#initRegistries()} before the registry freeze.
 */
public final class ModCriteria {

    private ModCriteria() {}

    // Count-based (threshold defined per advancement JSON)
    public static CountTrigger SAFARI_DAY;
    public static CountTrigger BAIT_USED;
    public static CountTrigger POKEMON_FLED;
    public static CountTrigger SPECIES_CAUGHT;
    public static CountTrigger ROTO_GLIDE;
    public static CountTrigger PORTAL_USED;
    public static CountTrigger DIGSITE_USED;
    public static CountTrigger EXCAVATION_FAILED;
    public static CountTrigger EXCAVATION_PERFECT;
    public static CountTrigger CSTRADER_TRADE;
    public static CountTrigger GIRATINA_TRADE;
    public static CountTrigger HOOPA_SAVE;
    public static CountTrigger WONDER_TRADE;
    public static CountTrigger GTS_TRADE_CONFIRMED;
    public static CountTrigger GTS_TRADE_DEPOSIT_SOLD;
    public static CountTrigger UNION_CREATED;

    // Boss battle
    public static CsBossWinTrigger CSBOSS_WIN;

    // One-shot events
    public static SimpleEventTrigger ROTOM_PHONE_MADE;
    public static SimpleEventTrigger ROTOM_PHONE_SHINY;
    public static SimpleEventTrigger ROTOM_SKIN_CHANGED;
    public static SimpleEventTrigger UNION_PARTY_POPPER;

    public static void register() {
        SAFARI_DAY = reg("safari_day", new CountTrigger());
        BAIT_USED = reg("bait_used", new CountTrigger());
        POKEMON_FLED = reg("pokemon_fled", new CountTrigger());
        SPECIES_CAUGHT = reg("species_caught", new CountTrigger());
        ROTO_GLIDE = reg("roto_glide", new CountTrigger());
        PORTAL_USED = reg("portal_used", new CountTrigger());
        DIGSITE_USED = reg("digsite_used", new CountTrigger());
        EXCAVATION_FAILED = reg("excavation_failed", new CountTrigger());
        EXCAVATION_PERFECT = reg("excavation_perfect", new CountTrigger());
        CSTRADER_TRADE = reg("cstrader_trade", new CountTrigger());
        GIRATINA_TRADE = reg("giratina_trade", new CountTrigger());
        HOOPA_SAVE = reg("hoopa_save", new CountTrigger());
        WONDER_TRADE = reg("wonder_trade", new CountTrigger());
        GTS_TRADE_CONFIRMED = reg("gts_trade_confirmed", new CountTrigger());
        GTS_TRADE_DEPOSIT_SOLD = reg("gts_trade_deposit_sold", new CountTrigger());
        UNION_CREATED = reg("union_created", new CountTrigger());

        CSBOSS_WIN = reg("csboss_win", new CsBossWinTrigger());

        ROTOM_PHONE_MADE = reg("rotom_phone_made", new SimpleEventTrigger());
        ROTOM_PHONE_SHINY = reg("rotom_phone_shiny", new SimpleEventTrigger());
        ROTOM_SKIN_CHANGED = reg("rotom_skin_changed", new SimpleEventTrigger());
        UNION_PARTY_POPPER = reg("union_party_popper", new SimpleEventTrigger());

        CobbleSafari.LOGGER.info("CobbleSafari >> Advancement criteria registered!");
    }

    private static <T extends CriterionTrigger<?>> T reg(String path, T trigger) {
        return Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, path), trigger);
    }
}
