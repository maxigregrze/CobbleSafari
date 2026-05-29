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
import maxigregrze.cobblesafari.influence.AlphaSpawnBoostInfluence;
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
        // Utility class; not meant to be instantiated.
    }

    public static void initRegistries() {
        LOGGER.info("CobbleSafari registering game objects...");

        SpawnBoostConfig.load();
        DonutPowerRegistry.init();
        ModEffects.register();
        ModPowerEffects.register();
        ModBlocks.register();
        ModBlockEntities.register();
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

        DungeonDimensions.register();
        SafariTimerConfig.syncDungeonDimensionTimersFromRegistry();
        CobbleSafariCommand.register();
        SafariExitCommand.register();
        UndergroundMinigame.registerCommon();
        ShinyBoostEvent.register();
        DimensionalBanEventHandler.registerCobblemonEvents();
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(RepelInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(BucketBoostInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(TypedAtypicalBoostInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(AlphaSpawnBoostInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(HiddenAbilityBoostInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(TypedEncounterBoostInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(WildLevelModifierInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(GuaranteedShinyInfluence::new);
        SafariCatchEventHandler.register();
        PowerCaptureCatchRateHandler.register();
        PowerSalvageLootHandler.register();
        PowerFriendshipCatchHandler.register();
        PartyBattlePowerEffects.register();

        LOGGER.info("CobbleSafari initialized successfully!");
    }

    public static void init() {
        LOGGER.info("CobbleSafari initializing...");
        initRegistries();
        initLogic();
    }
}
