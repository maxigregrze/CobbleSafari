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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.Random;

public class BalloonSpawnHandler {

    private static final Random RANDOM = new Random();
    private static final double BASE_SPAWN_CHANCE = 0.001;
    private static final int BALLOON_SAFARI_BASE_MULTIPLIER = 4;
    private static final int NEARBY_RANGE_MULTIPLIER = 2;

    private BalloonSpawnHandler() {}

    public static void onServerTick(MinecraftServer server) {
        int checkInterval = MiscConfig.getBalloonCheckIntervalTicks();
        ResourceLocation safariDimensionId = SafariTimerConfig.getSafariDimensionRL();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tryBalloonSpawnForPlayer(player, safariDimensionId, checkInterval);
        }
    }

    private static void tryBalloonSpawnForPlayer(ServerPlayer player, ResourceLocation safariDimensionId, int checkInterval) {
        ResourceLocation playerDimension = player.level().dimension().location();
        boolean isOverworld = player.level().dimension() == Level.OVERWORLD;
        boolean isSafari = playerDimension.equals(safariDimensionId);

        if (!isOverworld && !isSafari) return;
        if (player.tickCount % checkInterval != 0) return;
        if (!canSeeSky(player)) return;

        if (isSafari) {
            if (SafariConfig.isBalloonSafariEnabled()
                    && rollSpawn(player, BASE_SPAWN_CHANCE * SafariConfig.getBalloonSafariSpawnMultiplier(), countNearbyBalloons(player))) {
                trySpawnFromSky(player, ModEntities.BALLOON_SAFARI);
            }
            // The Safari exit drone rolls on its own, with the balloon's rules (action plan 198, part G).
            if (SafariConfig.isSafariDroneEnabled()
                    && rollSpawn(player, BASE_SPAWN_CHANCE * SafariConfig.getSafariDroneSpawnMultiplier(), countNearbyDrones(player))) {
                trySpawnFromSky(player, ModEntities.SAFARI_DRONE);
            }
        } else if (MiscConfig.isBalloonEnabled()
                && rollSpawn(player, BASE_SPAWN_CHANCE * MiscConfig.getBalloonSpawnMultiplier(), countNearbyBalloons(player))) {
            trySpawnFromSky(player, ModEntities.BALLOON);
        }
    }

    /** The chance roll, then the nearby cap shared by balloons and drones (each counted on its own). */
    private static boolean rollSpawn(ServerPlayer player, double spawnChance, int nearby) {
        if (RANDOM.nextDouble() > spawnChance) return false;
        int maxNearby = MiscConfig.getBalloonMaxNearby();
        return maxNearby <= 0 || nearby < maxNearby;
    }

    /** Balloons still holding loot around the player; the Safari variant is a subclass, so it is counted too. */
    private static int countNearbyBalloons(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(BalloonEntity.class, nearbyArea(player), balloon -> !balloon.hasDroppedLoot()).size();
    }

    private static int countNearbyDrones(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(SafariDroneEntity.class, nearbyArea(player), drone -> true).size();
    }

    private static AABB nearbyArea(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int range = MiscConfig.getBalloonSpawnRadius() * NEARBY_RANGE_MULTIPLIER;
        return new AABB(
                player.getX() - range, level.getMinBuildHeight(), player.getZ() - range,
                player.getX() + range, level.getMaxBuildHeight() + 10.0, player.getZ() + range);
    }

    private static boolean canSeeSky(ServerPlayer player) {
        return player.serverLevel().canSeeSky(player.blockPosition());
    }

    /** Places {@code type} above a random spot around the player, from where it comes down on its own. */
    private static void trySpawnFromSky(ServerPlayer player, EntityType<?> type) {
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

        Entity entity = type.create(level);
        if (entity != null) {
            entity.moveTo(spawnX + 0.5, spawnY, spawnZ + 0.5,
                    RANDOM.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntityWithPassengers(entity);
        }
    }
}
