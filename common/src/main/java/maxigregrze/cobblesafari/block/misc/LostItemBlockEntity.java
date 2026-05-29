package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.network.OpenLostItemConfigPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LostItemBlockEntity extends BlockEntity {

    private static final String NBT_CLAIMED_PLAYERS = "ClaimedPlayers";
    private static final String NBT_POOL_BERRY = "PoolBerry";
    private static final String NBT_POOL_CANDY = "PoolCandy";
    private static final String NBT_POOL_BALLS = "PoolBalls";
    private static final String NBT_POOL_TREASURES = "PoolTreasures";
    private static final String NBT_MIN_ROLL = "MinRoll";
    private static final String NBT_MAX_ROLL = "MaxRoll";
    private static final String NBT_LOOT_ITEM = "LootItem";
    private static final String NBT_LOST_ITEM_LOOT_TABLE = "LostItemLootTable";
    private static final String NBT_MODE = "Mode";

    private final Set<UUID> claimedPlayers = new HashSet<>();

    private String poolBerryId;
    private String poolCandyId;
    private String poolBallsId;
    private String poolTreasuresId;
    private int minRoll;
    private int maxRoll;
    private String lootItemId;
    private String lostItemLootTableId;
    private int mode;

    public LostItemBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOST_ITEM, pos, state);
        this.applyPlacementDefaults();
    }

    private void applyPlacementDefaults() {
        this.poolBerryId = MiscConfig.getLostItemPoolBerryId();
        this.poolCandyId = MiscConfig.getLostItemPoolCandyId();
        this.poolBallsId = MiscConfig.getLostItemPoolBallsId();
        this.poolTreasuresId = MiscConfig.getLostItemPoolTreasuresId();
        this.minRoll = MiscConfig.getLostItemMinRoll();
        this.maxRoll = MiscConfig.getLostItemMaxRoll();
        this.lootItemId = MiscConfig.getLostItemLootItemId();
        this.lostItemLootTableId = MiscConfig.getLostItemLootTableId();
        this.mode = MiscConfig.getLostItemMode();
    }

    public boolean hasClaimed(UUID playerId) {
        return this.claimedPlayers.contains(playerId);
    }

    public boolean tryClaim(UUID playerId) {
        boolean added = this.claimedPlayers.add(playerId);
        if (added) {
            this.syncClaimData();
        }
        return added;
    }

    public Set<UUID> getClaimedPlayers() {
        return Collections.unmodifiableSet(this.claimedPlayers);
    }

    public void resetClaims() {
        if (!this.claimedPlayers.isEmpty()) {
            this.claimedPlayers.clear();
            this.syncClaimData();
        }
    }

    public String getPoolBerryId() {
        return this.poolBerryId;
    }

    public String getPoolCandyId() {
        return this.poolCandyId;
    }

    public String getPoolBallsId() {
        return this.poolBallsId;
    }

    public String getPoolTreasuresId() {
        return this.poolTreasuresId;
    }

    public int getMinRoll() {
        return this.minRoll;
    }

    public int getMaxRoll() {
        return this.maxRoll;
    }

    public String getLootItemId() {
        return this.lootItemId;
    }

    public String getLostItemLootTableId() {
        return this.lostItemLootTableId;
    }

    public int getMode() {
        return this.mode;
    }

    public void setPoolBerryId(String poolBerryId) {
        this.poolBerryId = poolBerryId;
    }

    public void setPoolCandyId(String poolCandyId) {
        this.poolCandyId = poolCandyId;
    }

    public void setPoolBallsId(String poolBallsId) {
        this.poolBallsId = poolBallsId;
    }

    public void setPoolTreasuresId(String poolTreasuresId) {
        this.poolTreasuresId = poolTreasuresId;
    }

    public void setMinRoll(int minRoll) {
        this.minRoll = minRoll;
    }

    public void setMaxRoll(int maxRoll) {
        this.maxRoll = maxRoll;
    }

    public void setLootItemId(String lootItemId) {
        this.lootItemId = lootItemId;
    }

    public void setLostItemLootTableId(String lostItemLootTableId) {
        this.lostItemLootTableId = lostItemLootTableId;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * Ensures {@code minRoll <= maxRoll} by taking the min / max of the two stored values.
     */
    public void normalizeRollBounds() {
        int lo = Math.min(this.minRoll, this.maxRoll);
        int hi = Math.max(this.minRoll, this.maxRoll);
        this.minRoll = lo;
        this.maxRoll = hi;
    }

    public String getPoolIdForCategory(int categoryIndex) {
        return switch (categoryIndex) {
            case 0 -> this.poolBerryId;
            case 1 -> this.poolCandyId;
            case 2 -> this.poolBallsId;
            case 3 -> this.poolTreasuresId;
            default -> this.poolBerryId;
        };
    }

    public OpenLostItemConfigPayload createOpenPayload() {
        return new OpenLostItemConfigPayload(
                this.worldPosition,
                this.poolBerryId,
                this.poolCandyId,
                this.poolBallsId,
                this.poolTreasuresId,
                this.minRoll,
                this.maxRoll,
                this.lostItemLootTableId,
                this.lootItemId,
                this.mode
        );
    }

    public void syncConfigToClients() {
        this.setChanged();
        Level level = this.getLevel();
        if (level != null && !level.isClientSide()) {
            BlockState state = this.getBlockState();
            level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void syncClaimData() {
        this.setChanged();
        Level level = this.getLevel();
        if (level != null && !level.isClientSide()) {
            BlockState state = this.getBlockState();
            level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (UUID id : this.claimedPlayers) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put(NBT_CLAIMED_PLAYERS, list);

        tag.putString(NBT_POOL_BERRY, this.poolBerryId);
        tag.putString(NBT_POOL_CANDY, this.poolCandyId);
        tag.putString(NBT_POOL_BALLS, this.poolBallsId);
        tag.putString(NBT_POOL_TREASURES, this.poolTreasuresId);
        tag.putInt(NBT_MIN_ROLL, this.minRoll);
        tag.putInt(NBT_MAX_ROLL, this.maxRoll);
        tag.putString(NBT_LOOT_ITEM, this.lootItemId);
        tag.putString(NBT_LOST_ITEM_LOOT_TABLE, this.lostItemLootTableId);
        tag.putInt(NBT_MODE, this.mode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.claimedPlayers.clear();
        ListTag list = tag.getList(NBT_CLAIMED_PLAYERS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            UUID playerId = parseUuid(list.getString(i));
            if (playerId != null) {
                this.claimedPlayers.add(playerId);
            }
        }

        this.poolBerryId = tag.contains(NBT_POOL_BERRY) ? tag.getString(NBT_POOL_BERRY) : MiscConfig.getLostItemPoolBerryId();
        this.poolCandyId = tag.contains(NBT_POOL_CANDY) ? tag.getString(NBT_POOL_CANDY) : MiscConfig.getLostItemPoolCandyId();
        this.poolBallsId = tag.contains(NBT_POOL_BALLS) ? tag.getString(NBT_POOL_BALLS) : MiscConfig.getLostItemPoolBallsId();
        this.poolTreasuresId = tag.contains(NBT_POOL_TREASURES) ? tag.getString(NBT_POOL_TREASURES) : MiscConfig.getLostItemPoolTreasuresId();
        this.minRoll = tag.contains(NBT_MIN_ROLL) ? tag.getInt(NBT_MIN_ROLL) : MiscConfig.getLostItemMinRoll();
        this.maxRoll = tag.contains(NBT_MAX_ROLL) ? tag.getInt(NBT_MAX_ROLL) : MiscConfig.getLostItemMaxRoll();
        this.lootItemId = tag.contains(NBT_LOOT_ITEM) ? tag.getString(NBT_LOOT_ITEM) : MiscConfig.getLostItemLootItemId();
        this.lostItemLootTableId = tag.contains(NBT_LOST_ITEM_LOOT_TABLE) ? tag.getString(NBT_LOST_ITEM_LOOT_TABLE) : MiscConfig.getLostItemLootTableId();
        this.mode = tag.contains(NBT_MODE) ? tag.getInt(NBT_MODE) : MiscConfig.getLostItemMode();
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
