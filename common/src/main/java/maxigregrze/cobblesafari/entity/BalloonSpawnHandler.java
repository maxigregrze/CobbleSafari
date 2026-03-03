package maxigregrze.cobblesafari.entity;

import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.config.SafariConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

public class BalloonSpawnHandler {

    private static final Random RANDOM = new Random();
    private static final double BASE_SPAWN_CHANCE = 0.001;
    private static final int BALLOON_SAFARI_BASE_MULTIPLIER = 4;

    private BalloonSpawnHandler() {}

    public static void onServerTick(MinecraftServer server) {
        int checkInterval = MiscConfig.getBalloonCheckIntervalTicks();
        ResourceLocation safariDimensionId = ResourceLocation.parse(SafariTimerConfig.getSafariDimensionId());

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ResourceLocation playerDimension = player.level().dimension().location();
            boolean isOverworld = player.level().dimension() == Level.OVERWORLD;
            boolean isSafari = playerDimension.equals(safariDimensionId);

            if (!isOverworld && !isSafari) continue;
            if (player.tickCount % checkInterval != 0) continue;
            if (!canSeeSky(player)) continue;

            double spawnChance;
            if (isSafari) {
                if (!SafariConfig.isBalloonSafariEnabled()) continue;
                spawnChance = BASE_SPAWN_CHANCE * SafariConfig.getBalloonSafariSpawnMultiplier();
            } else {
                if (!MiscConfig.isBalloonEnabled()) continue;
                spawnChance = BASE_SPAWN_CHANCE * MiscConfig.getBalloonSpawnMultiplier();
            }

            if (RANDOM.nextDouble() > spawnChance) continue;

            trySpawnBalloon(player, isSafari);
        }
    }

    private static boolean canSeeSky(ServerPlayer player) {
        return player.serverLevel().canSeeSky(player.blockPosition());
    }

    private static void trySpawnBalloon(ServerPlayer player, boolean isSafariDimension) {
        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        int spawnRadius = MiscConfig.getBalloonSpawnRadius();
        int heightAboveGround = MiscConfig.getBalloonHeightAboveGround();

        double angle = RANDOM.nextDouble() * Math.PI * 2;
        int distance = RANDOM.nextInt(spawnRadius) + 1;

        int spawnX = playerPos.getX() + (int) (Math.cos(angle) * distance);
        int spawnZ = playerPos.getZ() + (int) (Math.sin(angle) * distance);

        int groundHeight = level.getHeight(Heightmap.Types.WORLD_SURFACE, spawnX, spawnZ);
        int spawnY = groundHeight + heightAboveGround;

        if (spawnY >= level.getMaxBuildHeight() - 10) {
            spawnY = level.getMaxBuildHeight() - 10;
        }

        BlockPos spawnPos = new BlockPos(spawnX, spawnY, spawnZ);
        if (!level.getBlockState(spawnPos).isAir()) return;

        if (isSafariDimension) {
            BalloonSafariEntity balloon = ModEntities.BALLOON_SAFARI.create(level);
            if (balloon != null) {
                balloon.moveTo(spawnX + 0.5, spawnY, spawnZ + 0.5,
                        RANDOM.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntityWithPassengers(balloon);
            }
        } else {
            BalloonEntity balloon = ModEntities.BALLOON.create(level);
            if (balloon != null) {
                balloon.moveTo(spawnX + 0.5, spawnY, spawnZ + 0.5,
                        RANDOM.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntityWithPassengers(balloon);
            }
        }
    }
}
