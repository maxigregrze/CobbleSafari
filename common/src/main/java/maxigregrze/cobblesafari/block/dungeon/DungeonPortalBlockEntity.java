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
                return;
            }
        }

        long currentTick = level.getGameTime();
        int lifetime = PortalSpawnConfig.getPortalLifetimeTicks();
        long elapsed = currentTick - blockEntity.spawnTick;
        int warningThreshold = (int) (lifetime * 0.2);

        if (elapsed >= lifetime - warningThreshold && elapsed < lifetime) {
            if (currentTick % 5 == 0) {
                spawnDisappearingParticles(serverLevel, pos);
            }
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
            tag.putInt("LinkedX", linkedPortalPos.getX());
            tag.putInt("LinkedY", linkedPortalPos.getY());
            tag.putInt("LinkedZ", linkedPortalPos.getZ());
        }

        if (linkedDimension != null) {
            tag.putString("LinkedDimension", linkedDimension.location().toString());
        }

        if (originPos != null) {
            tag.putInt("OriginX", originPos.getX());
            tag.putInt("OriginY", originPos.getY());
            tag.putInt("OriginZ", originPos.getZ());
        }

        if (originDimension != null) {
            tag.putString("OriginDimension", originDimension.location().toString());
        }

        if (dungeonDimensionId != null) {
            tag.putString("DungeonDimensionId", dungeonDimensionId);
        }

        if (portalId != null) {
            tag.putUUID("PortalId", portalId);
        }

        tag.putLong("SpawnTick", spawnTick);

        if (dungeonStructurePos != null) {
            tag.putInt("DungeonStructureX", dungeonStructurePos.getX());
            tag.putInt("DungeonStructureY", dungeonStructurePos.getY());
            tag.putInt("DungeonStructureZ", dungeonStructurePos.getZ());
        }

        if (dungeonExitPortalPos != null) {
            tag.putInt("DungeonExitPortalX", dungeonExitPortalPos.getX());
            tag.putInt("DungeonExitPortalY", dungeonExitPortalPos.getY());
            tag.putInt("DungeonExitPortalZ", dungeonExitPortalPos.getZ());
        }

        if (hasDungeonChunkBounds) {
            tag.putBoolean("HasDungeonChunkBounds", true);
            tag.putInt("DungeonChunkMinX", dungeonChunkMinX);
            tag.putInt("DungeonChunkMinZ", dungeonChunkMinZ);
            tag.putInt("DungeonChunkMaxX", dungeonChunkMaxX);
            tag.putInt("DungeonChunkMaxZ", dungeonChunkMaxZ);
        }

        if (autoRenewPortal) {
            tag.putBoolean("AutoRenewPortal", true);
        }
        tag.putBoolean("RandomDestinationMode", randomDestinationMode);
        if (fixedDungeonId != null) {
            tag.putString("FixedDungeonId", fixedDungeonId);
        }

        if (!enteredPlayers.isEmpty()) {
            ListTag playerList = new ListTag();
            for (UUID playerId : enteredPlayers) {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putUUID("PlayerId", playerId);
                playerList.add(playerTag);
            }
            tag.put("EnteredPlayers", playerList);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("LinkedX")) {
            linkedPortalPos = new BlockPos(
                    tag.getInt("LinkedX"),
                    tag.getInt("LinkedY"),
                    tag.getInt("LinkedZ")
            );
        }

        if (tag.contains("LinkedDimension")) {
            linkedDimension = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(tag.getString("LinkedDimension"))
            );
        }

        if (tag.contains("OriginX")) {
            originPos = new BlockPos(
                    tag.getInt("OriginX"),
                    tag.getInt("OriginY"),
                    tag.getInt("OriginZ")
            );
        }

        if (tag.contains("OriginDimension")) {
            originDimension = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(tag.getString("OriginDimension"))
            );
        }

        if (tag.contains("DungeonDimensionId")) {
            dungeonDimensionId = tag.getString("DungeonDimensionId");
        }

        if (tag.contains("PortalId")) {
            portalId = tag.getUUID("PortalId");
        }

        if (tag.contains("SpawnTick")) {
            spawnTick = tag.getLong("SpawnTick");
        }

        if (tag.contains("DungeonStructureX")) {
            dungeonStructurePos = new BlockPos(
                    tag.getInt("DungeonStructureX"),
                    tag.getInt("DungeonStructureY"),
                    tag.getInt("DungeonStructureZ")
            );
        }

        if (tag.contains("DungeonExitPortalX")) {
            dungeonExitPortalPos = new BlockPos(
                    tag.getInt("DungeonExitPortalX"),
                    tag.getInt("DungeonExitPortalY"),
                    tag.getInt("DungeonExitPortalZ")
            );
        }

        if (tag.contains("HasDungeonChunkBounds")) {
            hasDungeonChunkBounds = tag.getBoolean("HasDungeonChunkBounds");
            dungeonChunkMinX = tag.getInt("DungeonChunkMinX");
            dungeonChunkMinZ = tag.getInt("DungeonChunkMinZ");
            dungeonChunkMaxX = tag.getInt("DungeonChunkMaxX");
            dungeonChunkMaxZ = tag.getInt("DungeonChunkMaxZ");
        }

        autoRenewPortal = tag.contains("AutoRenewPortal") && tag.getBoolean("AutoRenewPortal");
        randomDestinationMode = !tag.contains("RandomDestinationMode") || tag.getBoolean("RandomDestinationMode");
        fixedDungeonId = tag.contains("FixedDungeonId") ? tag.getString("FixedDungeonId") : null;

        enteredPlayers.clear();
        if (tag.contains("EnteredPlayers", Tag.TAG_LIST)) {
            ListTag playerList = tag.getList("EnteredPlayers", Tag.TAG_COMPOUND);
            for (int i = 0; i < playerList.size(); i++) {
                CompoundTag playerTag = playerList.getCompound(i);
                if (playerTag.hasUUID("PlayerId")) {
                    enteredPlayers.add(playerTag.getUUID("PlayerId"));
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
