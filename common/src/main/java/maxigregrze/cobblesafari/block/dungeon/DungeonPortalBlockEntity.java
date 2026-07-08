package maxigregrze.cobblesafari.block.dungeon;

import maxigregrze.cobblesafari.dungeon.PortalSpawnConfig;
import maxigregrze.cobblesafari.dungeon.PortalSpawnManager;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DungeonPortalBlockEntity extends BlockEntity {

    private static final String KEY_LINKED_X = "LinkedX";
    private static final String KEY_LINKED_DIMENSION = "LinkedDimension";
    private static final String KEY_ORIGIN_X = "OriginX";
    private static final String KEY_ORIGIN_DIMENSION = "OriginDimension";
    private static final String KEY_DUNGEON_DIMENSION_ID = "DungeonDimensionId";
    private static final String KEY_PORTAL_ID = "PortalId";
    private static final String KEY_SPAWN_TICK = "SpawnTick";
    private static final String KEY_DUNGEON_STRUCTURE_X = "DungeonStructureX";
    private static final String KEY_DUNGEON_EXIT_PORTAL_X = "DungeonExitPortalX";
    private static final String KEY_HAS_DUNGEON_CHUNK_BOUNDS = "HasDungeonChunkBounds";
    private static final String KEY_AUTO_RENEW_PORTAL = "AutoRenewPortal";
    private static final String KEY_RANDOM_DESTINATION_MODE = "RandomDestinationMode";
    private static final String KEY_FIXED_DUNGEON_ID = "FixedDungeonId";
    private static final String KEY_PLAYER_ID = "PlayerId";
    private static final String KEY_ENTERED_PLAYERS = "EnteredPlayers";

    private BlockPos linkedPortalPos;
    private ResourceKey<Level> linkedDimension;
    private BlockPos originPos;
    private ResourceKey<Level> originDimension;
    private String dungeonDimensionId;
    private UUID portalId;
    private long spawnTick = -1;
    private BlockPos dungeonStructurePos;
    private BlockPos dungeonExitPortalPos;
    private int dungeonChunkMinX;
    private int dungeonChunkMinZ;
    private int dungeonChunkMaxX;
    private int dungeonChunkMaxZ;
    private boolean hasDungeonChunkBounds = false;
    private boolean autoRenewPortal = false;
    private boolean randomDestinationMode = true;
    private String fixedDungeonId;
    private final Set<UUID> enteredPlayers = new HashSet<>();

    public DungeonPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DUNGEON_PORTAL, pos, state);
        this.portalId = UUID.randomUUID();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DungeonPortalBlockEntity blockEntity) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        if (state.getValue(DungeonPortalBlock.PORTAL_TYPE) != DungeonPortalBlock.PortalType.ENTRANCE) return;

        if (blockEntity.spawnTick < 0) {
            PortalSpawnManager.ActivePortal activePortal = PortalSpawnManager.getActivePortalById(blockEntity.portalId);
            if (activePortal != null) {
                blockEntity.spawnTick = activePortal.spawnTick();
            } else {
                // Orphaned portal with no persisted spawn tick and no tracking entry: adopt it
                // now so it expires after one lifetime instead of lingering forever.
                blockEntity.setSpawnTick(level.getGameTime());
                return;
            }
        }

        long currentTick = level.getGameTime();
        int lifetime = PortalSpawnConfig.getPortalLifetimeTicks();
        long elapsed = currentTick - blockEntity.spawnTick;

        if (elapsed >= lifetime && PortalSpawnConfig.isEnabled()) {
            // Self-expiry safety net: covers portals that expired while their chunk was unloaded
            // and portals whose tracking entry was lost (they would otherwise linger forever).
            PortalSpawnManager.expirePortalInPlace(serverLevel, pos, blockEntity);
            return;
        }

        int warningThreshold = (int) (lifetime * 0.2);

        if (elapsed >= lifetime - warningThreshold && elapsed < lifetime && currentTick % 5 == 0) {
            spawnDisappearingParticles(serverLevel, pos);
        }

        if (serverLevel.getRandom().nextInt(40) == 0) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            serverLevel.playSound(null, x, y, z, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5f, serverLevel.getRandom().nextFloat() * 0.4f + 0.8f);
        }
    }

    private static void spawnDisappearingParticles(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        var random = level.getRandom();

        for (int i = 0; i < 3; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 1.5;
            double offsetY = random.nextDouble() * 2.0;
            double offsetZ = (random.nextDouble() - 0.5) * 1.5;

            level.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.05, 0.0, 0.02
            );
        }

        if (random.nextInt(3) == 0) {
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    x, y + 0.5, z,
                    2, 0.3, 0.5, 0.3, 0.01
            );
        }
    }

    public long getRemainingLifetimeTicks() {
        if (level == null || spawnTick < 0) return -1;
        long currentTick = level.getGameTime();
        int lifetime = PortalSpawnConfig.getPortalLifetimeTicks();
        long elapsed = currentTick - spawnTick;
        return Math.max(0, lifetime - elapsed);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (linkedPortalPos != null) {
            tag.putInt(KEY_LINKED_X, linkedPortalPos.getX());
            tag.putInt("LinkedY", linkedPortalPos.getY());
            tag.putInt("LinkedZ", linkedPortalPos.getZ());
        }

        if (linkedDimension != null) {
            tag.putString(KEY_LINKED_DIMENSION, linkedDimension.location().toString());
        }

        if (originPos != null) {
            tag.putInt(KEY_ORIGIN_X, originPos.getX());
            tag.putInt("OriginY", originPos.getY());
            tag.putInt("OriginZ", originPos.getZ());
        }

        if (originDimension != null) {
            tag.putString(KEY_ORIGIN_DIMENSION, originDimension.location().toString());
        }

        if (dungeonDimensionId != null) {
            tag.putString(KEY_DUNGEON_DIMENSION_ID, dungeonDimensionId);
        }

        if (portalId != null) {
            tag.putUUID(KEY_PORTAL_ID, portalId);
        }

        tag.putLong(KEY_SPAWN_TICK, spawnTick);

        if (dungeonStructurePos != null) {
            tag.putInt(KEY_DUNGEON_STRUCTURE_X, dungeonStructurePos.getX());
            tag.putInt("DungeonStructureY", dungeonStructurePos.getY());
            tag.putInt("DungeonStructureZ", dungeonStructurePos.getZ());
        }

        if (dungeonExitPortalPos != null) {
            tag.putInt(KEY_DUNGEON_EXIT_PORTAL_X, dungeonExitPortalPos.getX());
            tag.putInt("DungeonExitPortalY", dungeonExitPortalPos.getY());
            tag.putInt("DungeonExitPortalZ", dungeonExitPortalPos.getZ());
        }

        if (hasDungeonChunkBounds) {
            tag.putBoolean(KEY_HAS_DUNGEON_CHUNK_BOUNDS, true);
            tag.putInt("DungeonChunkMinX", dungeonChunkMinX);
            tag.putInt("DungeonChunkMinZ", dungeonChunkMinZ);
            tag.putInt("DungeonChunkMaxX", dungeonChunkMaxX);
            tag.putInt("DungeonChunkMaxZ", dungeonChunkMaxZ);
        }

        if (autoRenewPortal) {
            tag.putBoolean(KEY_AUTO_RENEW_PORTAL, true);
        }
        tag.putBoolean(KEY_RANDOM_DESTINATION_MODE, randomDestinationMode);
        if (fixedDungeonId != null) {
            tag.putString(KEY_FIXED_DUNGEON_ID, fixedDungeonId);
        }

        if (!enteredPlayers.isEmpty()) {
            ListTag playerList = new ListTag();
            for (UUID playerId : enteredPlayers) {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putUUID(KEY_PLAYER_ID, playerId);
                playerList.add(playerTag);
            }
            tag.put(KEY_ENTERED_PLAYERS, playerList);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains(KEY_LINKED_X)) {
            linkedPortalPos = new BlockPos(
                    tag.getInt(KEY_LINKED_X),
                    tag.getInt("LinkedY"),
                    tag.getInt("LinkedZ")
            );
        }

        if (tag.contains(KEY_LINKED_DIMENSION)) {
            linkedDimension = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(tag.getString(KEY_LINKED_DIMENSION))
            );
        }

        if (tag.contains(KEY_ORIGIN_X)) {
            originPos = new BlockPos(
                    tag.getInt(KEY_ORIGIN_X),
                    tag.getInt("OriginY"),
                    tag.getInt("OriginZ")
            );
        }

        if (tag.contains(KEY_ORIGIN_DIMENSION)) {
            originDimension = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(tag.getString(KEY_ORIGIN_DIMENSION))
            );
        }

        if (tag.contains(KEY_DUNGEON_DIMENSION_ID)) {
            dungeonDimensionId = tag.getString(KEY_DUNGEON_DIMENSION_ID);
        }

        if (tag.contains(KEY_PORTAL_ID)) {
            portalId = tag.getUUID(KEY_PORTAL_ID);
        }

        if (tag.contains(KEY_SPAWN_TICK)) {
            spawnTick = tag.getLong(KEY_SPAWN_TICK);
        }

        if (tag.contains(KEY_DUNGEON_STRUCTURE_X)) {
            dungeonStructurePos = new BlockPos(
                    tag.getInt(KEY_DUNGEON_STRUCTURE_X),
                    tag.getInt("DungeonStructureY"),
                    tag.getInt("DungeonStructureZ")
            );
        }

        if (tag.contains(KEY_DUNGEON_EXIT_PORTAL_X)) {
            dungeonExitPortalPos = new BlockPos(
                    tag.getInt(KEY_DUNGEON_EXIT_PORTAL_X),
                    tag.getInt("DungeonExitPortalY"),
                    tag.getInt("DungeonExitPortalZ")
            );
        }

        if (tag.contains(KEY_HAS_DUNGEON_CHUNK_BOUNDS)) {
            hasDungeonChunkBounds = tag.getBoolean(KEY_HAS_DUNGEON_CHUNK_BOUNDS);
            dungeonChunkMinX = tag.getInt("DungeonChunkMinX");
            dungeonChunkMinZ = tag.getInt("DungeonChunkMinZ");
            dungeonChunkMaxX = tag.getInt("DungeonChunkMaxX");
            dungeonChunkMaxZ = tag.getInt("DungeonChunkMaxZ");
        }

        autoRenewPortal = tag.contains(KEY_AUTO_RENEW_PORTAL) && tag.getBoolean(KEY_AUTO_RENEW_PORTAL);
        randomDestinationMode = !tag.contains(KEY_RANDOM_DESTINATION_MODE) || tag.getBoolean(KEY_RANDOM_DESTINATION_MODE);
        fixedDungeonId = tag.contains(KEY_FIXED_DUNGEON_ID) ? tag.getString(KEY_FIXED_DUNGEON_ID) : null;

        enteredPlayers.clear();
        if (tag.contains(KEY_ENTERED_PLAYERS, Tag.TAG_LIST)) {
            ListTag playerList = tag.getList(KEY_ENTERED_PLAYERS, Tag.TAG_COMPOUND);
            for (int i = 0; i < playerList.size(); i++) {
                CompoundTag playerTag = playerList.getCompound(i);
                if (playerTag.hasUUID(KEY_PLAYER_ID)) {
                    enteredPlayers.add(playerTag.getUUID(KEY_PLAYER_ID));
                }
            }
        }
    }

    public BlockPos getLinkedPortalPos() {
        return linkedPortalPos;
    }

    public void setLinkedPortalPos(BlockPos pos) {
        this.linkedPortalPos = pos;
        setChanged();
    }

    public ResourceKey<Level> getLinkedDimension() {
        return linkedDimension;
    }

    public void setLinkedDimension(ResourceKey<Level> dimension) {
        this.linkedDimension = dimension;
        setChanged();
    }

    public BlockPos getOriginPos() {
        return originPos;
    }

    public void setOriginPos(BlockPos pos) {
        this.originPos = pos;
        setChanged();
    }

    public ResourceKey<Level> getOriginDimension() {
        return originDimension;
    }

    public void setOriginDimension(ResourceKey<Level> dimension) {
        this.originDimension = dimension;
        setChanged();
    }

    public String getDungeonDimensionId() {
        return dungeonDimensionId;
    }

    public void setDungeonDimensionId(String dimensionId) {
        this.dungeonDimensionId = dimensionId;
        setChanged();
    }

    public UUID getPortalId() {
        return portalId;
    }

    public void setPortalId(UUID id) {
        this.portalId = id;
        setChanged();
    }

    public BlockPos getDungeonStructurePos() {
        return dungeonStructurePos;
    }

    public void setDungeonStructurePos(BlockPos pos) {
        this.dungeonStructurePos = pos;
        setChanged();
    }

    public BlockPos getDungeonExitPortalPos() {
        return dungeonExitPortalPos;
    }

    public void setDungeonExitPortalPos(BlockPos pos) {
        this.dungeonExitPortalPos = pos;
        setChanged();
    }

    public long getSpawnTick() {
        return spawnTick;
    }

    public void setSpawnTick(long tick) {
        this.spawnTick = tick;
        setChanged();
    }

    public boolean hasDungeonChunkBounds() {
        return hasDungeonChunkBounds;
    }

    public int getDungeonChunkMinX() {
        return dungeonChunkMinX;
    }

    public int getDungeonChunkMinZ() {
        return dungeonChunkMinZ;
    }

    public int getDungeonChunkMaxX() {
        return dungeonChunkMaxX;
    }

    public int getDungeonChunkMaxZ() {
        return dungeonChunkMaxZ;
    }

    public void setDungeonChunkBounds(int minX, int minZ, int maxX, int maxZ) {
        this.dungeonChunkMinX = minX;
        this.dungeonChunkMinZ = minZ;
        this.dungeonChunkMaxX = maxX;
        this.dungeonChunkMaxZ = maxZ;
        this.hasDungeonChunkBounds = true;
        setChanged();
    }

    public boolean hasPlayerEntered(UUID playerId) {
        return enteredPlayers.contains(playerId);
    }

    public void addEnteredPlayer(UUID playerId) {
        enteredPlayers.add(playerId);
        setChanged();
    }

    public Set<UUID> getEnteredPlayers() {
        return enteredPlayers;
    }

    public void clearEnteredPlayers() {
        enteredPlayers.clear();
        setChanged();
    }

    public void resetDungeonRuntimeData() {
        dungeonStructurePos = null;
        dungeonExitPortalPos = null;
        dungeonChunkMinX = 0;
        dungeonChunkMinZ = 0;
        dungeonChunkMaxX = 0;
        dungeonChunkMaxZ = 0;
        hasDungeonChunkBounds = false;
        enteredPlayers.clear();
        setChanged();
    }

    public boolean isAutoRenewPortal() {
        return autoRenewPortal;
    }

    public void setAutoRenewPortal(boolean autoRenewPortal) {
        this.autoRenewPortal = autoRenewPortal;
        setChanged();
    }

    public boolean isRandomDestinationMode() {
        return randomDestinationMode;
    }

    public void setRandomDestinationMode(boolean randomDestinationMode) {
        this.randomDestinationMode = randomDestinationMode;
        if (randomDestinationMode) {
            this.fixedDungeonId = null;
        }
        setChanged();
    }

    public String getFixedDungeonId() {
        return fixedDungeonId;
    }

    public void setFixedDungeonId(String fixedDungeonId) {
        this.fixedDungeonId = fixedDungeonId;
        this.randomDestinationMode = fixedDungeonId == null;
        setChanged();
    }
}
