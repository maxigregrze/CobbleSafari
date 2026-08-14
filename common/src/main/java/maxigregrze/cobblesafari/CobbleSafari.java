package maxigregrze.cobblesafari;

import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory;
import maxigregrze.cobblesafari.command.CobbleSafariCommand;
import maxigregrze.cobblesafari.command.SafariExitCommand;
import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.config.IncubatorConfig;
import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.config.SafariConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.config.SecretBasePCConfig;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.config.RandomizerItemsConfig;
import maxigregrze.cobblesafari.config.WonderTradeSettings;
import maxigregrze.cobblesafari.config.RotomPhoneConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.PortalSpawnConfig;
import maxigregrze.cobblesafari.event.DimensionalBanEventHandler;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.init.ModComponents;
import maxigregrze.cobblesafari.init.ModCreativeTabs;
import maxigregrze.cobblesafari.init.ModEffects;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.init.ModProcessors;
import maxigregrze.cobblesafari.item.donut.DonutPowerRegistry;
import maxigregrze.cobblesafari.item.donut.DonutSeasoningProcessor;
import maxigregrze.cobblesafari.init.ModPowerEffects;
// import maxigregrze.cobblesafari.influence.AlphaSpawnBoostInfluence; @TODO ALPHA
import maxigregrze.cobblesafari.influence.GuaranteedShinyInfluence;
import maxigregrze.cobblesafari.influence.BucketBoostInfluence;
import maxigregrze.cobblesafari.influence.HiddenAbilityBoostInfluence;
import maxigregrze.cobblesafari.influence.RepelInfluence;
import maxigregrze.cobblesafari.influence.TypedAtypicalBoostInfluence;
import maxigregrze.cobblesafari.influence.TypedEncounterBoostInfluence;
import maxigregrze.cobblesafari.influence.WildLevelModifierInfluence;
import maxigregrze.cobblesafari.event.PowerCaptureCatchRateHandler;
import maxigregrze.cobblesafari.event.PartyBattlePowerEffects;
import maxigregrze.cobblesafari.event.PowerFriendshipCatchHandler;
import maxigregrze.cobblesafari.event.PowerSalvageLootHandler;
import maxigregrze.cobblesafari.influence.ShinyBoostEvent;
import maxigregrze.cobblesafari.safari.SafariCatchEventHandler;
import maxigregrze.cobblesafari.underground.UndergroundMinigame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobbleSafari {
    public static final String MOD_ID = "cobblesafari";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private CobbleSafari() {
        /* Hello there ! 
        You're probably looking through the code to check if this mod was vibe-coded, and I 100% get you ! 
        As descibed on the modrinth page, the project was initially kickstarted using the help of coding aid tools: during my last job, I was tasked to use this kind of tools on personal projects to better understand them, in order to teach my colleagues how to use them.
        The main reason I started this mod was to let people be able to experience content seen on P2W minecraft servers. What started as a datapack transformed into a fully-fledged mod thanks to said tools.
        I would be lying saying they didn't impact the mod developement, or even let me create things I wouldn't have done by myself otherwise, therefore this is why I'm writing this.
        However, as the usage of thoses tools became worse and worse ethically, and as I got laid off from said job, I started over the past few months doing my best to reduce my usage of theses tools by revoking my subscriptions and clean-up the part of the codebase made that way.
        On top of that, I want to clarify that while I do have a light modding experience in other games and even older versions of minecraft, I'm still pretty new to the modern MC modding scene, so a lot of bad code is 100% human-made slop (mostly due to my incomprehension on how some of the game works)
        If any modrinth moderator or anyone else reading this need clarification on what is and isn't made using thoses tools, and require proof my process is not 100% ai-assisted, feel free to reach out to me on discord; I'll be happy to provide past modding works, PSD files, Blockbench files, gameplay designs documents I've written since the start of 2026 !
        Thanks for the work you're putting to make sure the community stay healthy and free from soulless slop.
        - maxigregrze
        */
    }

    public static void initRegistries() {
        LOGGER.info("CobbleSafari registering game objects...");

        SpawnBoostConfig.load();
        DonutPowerRegistry.init();
        ModEffects.register();
        ModPowerEffects.register();
        ModBlocks.register();
        ModBlockEntities.register();
        maxigregrze.cobblesafari.init.ModFeatures.register();
        ModComponents.register();
        ModItems.register();
        DonutSeasoningProcessor.register();
        ModCreativeTabs.register();
        ModEntities.register();
        ModProcessors.register();
        maxigregrze.cobblesafari.init.ModStats.register();
        maxigregrze.cobblesafari.advancement.ModCriteria.register();
    }

    public static void initLogic() {
        SafariTimerConfig.load();
        IncubatorConfig.load();
        SafariConfig.load();
        SpawnBoostConfig.load();
        PortalSpawnConfig.load();
        DimensionalBanConfig.load();
        MiscConfig.load();
        SecretBasePCConfig.load();
        RandomizerItemsConfig.load();
        RotomPhoneConfig.load();
        WonderTradeSettings.load();
        maxigregrze.cobblesafari.config.CsBossSettings.load();
        maxigregrze.cobblesafari.csboss.attack.CsBossAttackRegistry.registerDefaults();
        maxigregrze.cobblesafari.config.DimensionalMusicConfig.load();

        DungeonDimensions.register();
        SafariTimerConfig.syncDungeonDimensionTimersFromRegistry();
        CobbleSafariCommand.register();
        SafariExitCommand.register();
        UndergroundMinigame.registerCommon();
        ShinyBoostEvent.register();
        DimensionalBanEventHandler.registerCobblemonEvents();
        maxigregrze.cobblesafari.event.CsBossArenaRestrictionHandler.registerCobblemonEvents();
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(RepelInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders()
                .add(maxigregrze.cobblesafari.influence.CsBossArenaSpawnBlockInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(BucketBoostInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(TypedAtypicalBoostInfluence::new);
        // PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(AlphaSpawnBoostInfluence::new); @TODO ALPHA
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(HiddenAbilityBoostInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(TypedEncounterBoostInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(WildLevelModifierInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(GuaranteedShinyInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(maxigregrze.cobblesafari.influence.ObjectivesShinyInfluence::new);
        SafariCatchEventHandler.register();
        maxigregrze.cobblesafari.effect.RedShackledCatchHandler.register();
        maxigregrze.cobblesafari.objectives.ObjectivesManager.register();
        PowerCaptureCatchRateHandler.register();
        PowerSalvageLootHandler.register();
        PowerFriendshipCatchHandler.register();
        PartyBattlePowerEffects.register();
        maxigregrze.cobblesafari.csmusic.BattleMusicTracker.register();

        LOGGER.info("CobbleSafari initialized successfully!");
    }

    public static void init() {
        LOGGER.info("CobbleSafari initializing...");
        initRegistries();
        initLogic();
    }
}
