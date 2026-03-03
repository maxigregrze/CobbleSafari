package maxigregrze.cobblesafari;

import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory;
import maxigregrze.cobblesafari.command.CobbleSafariCommand;
import maxigregrze.cobblesafari.command.SafariExitCommand;
import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.config.IncubatorConfig;
import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.config.SafariConfig;
import maxigregrze.cobblesafari.config.SecretBasePCConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.PortalSpawnConfig;
import maxigregrze.cobblesafari.event.DimensionalBanEventHandler;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.init.ModCreativeTabs;
import maxigregrze.cobblesafari.init.ModEffects;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.init.ModProcessors;
import maxigregrze.cobblesafari.influence.BucketBoostInfluence;
import maxigregrze.cobblesafari.influence.RepelInfluence;
import maxigregrze.cobblesafari.influence.ShinyBoostEvent;
import maxigregrze.cobblesafari.safari.SafariCatchEventHandler;
import maxigregrze.cobblesafari.underground.UndergroundMinigame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobbleSafari {
    public static final String MOD_ID = "cobblesafari";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void initRegistries() {
        LOGGER.info("CobbleSafari registering game objects...");

        ModEffects.register();
        ModBlocks.register();
        ModBlockEntities.register();
        ModItems.register();
        ModCreativeTabs.register();
        ModEntities.register();
        ModProcessors.register();
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

        DungeonDimensions.register();
        CobbleSafariCommand.register();
        SafariExitCommand.register();
        UndergroundMinigame.registerCommon();
        ShinyBoostEvent.register();
        DimensionalBanEventHandler.registerCobblemonEvents();
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(RepelInfluence::new);
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(BucketBoostInfluence::new);
        SafariCatchEventHandler.register();

        LOGGER.info("CobbleSafari initialized successfully!");
    }

    public static void init() {
        LOGGER.info("CobbleSafari initializing...");
        initRegistries();
        initLogic();
    }
}
