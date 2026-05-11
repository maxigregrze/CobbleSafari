package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.network.OpenAuspiciousPokeballConfigPayload;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AuspiciousPokeballBlockEntity extends BlockEntity {

    private static final String NBT_CLAIMED_PLAYERS = "ClaimedPlayers";
    private static final String NBT_POOL_BERRY = "PoolBerry";
    private static final String NBT_POOL_CANDY = "PoolCandy";
    private static final String NBT_POOL_BALLS = "PoolBalls";
    private static final String NBT_POOL_TREASURES = "PoolTreasures";
    private static final String NBT_MIN_ROLL = "MinRoll";
    private static final String NBT_MAX_ROLL = "MaxRoll";

    private final Set<UUID> claimedPlayers = new HashSet<>();

    private String poolBerryId;
    private String poolCandyId;
    private String poolBallsId;
    private String poolTreasuresId;
    private int minRoll;
    private int maxRoll;

    public AuspiciousPokeballBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.AUSPICIOUS_POKEBALL, pos, state);
    }

    protected AuspiciousPokeballBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.applyPlacementDefaults();
    }

    /**
     * Bloc rendu par le BER (variante {@code _display}).
     */
    public Block displayRenderBlock() {
        return ModBlocks.AUSPICIOUS_POKEBALL_DISPLAY;
    }

    /**
     * Hitbox vide en survie pour ce joueur (réclamé ou règles spécifiques sous-classe).
     */
    public boolean shouldHideShapeForSurvivalPlayer(Player player) {
        if (player.isCreative()) {
            return false;
        }
        if (this.hasClaimed(player.getUUID())) {
            return true;
        }
        return this.shouldHideShapeForEarnRestrictions(player);
    }

    protected boolean shouldHideShapeForEarnRestrictions(Player player) {
        return false;
    }

    /**
     * Masquer le modèle monde pour le joueur local (client).
     */
    public boolean shouldHideWorldModelForLocalPlayer(Player player) {
        if (this.hasClaimed(player.getUUID())) {
            return true;
        }
        return this.shouldHideWorldModelForEarnRestrictions(player);
    }

    protected boolean shouldHideWorldModelForEarnRestrictions(Player player) {
        return false;
    }

    public boolean canPlayerAttemptClaim(Player player) {
        return true;
    }

    private void applyPlacementDefaults() {
        this.poolBerryId = MiscConfig.getAuspiciousPokeballPoolBerryId();
        this.poolCandyId = MiscConfig.getAuspiciousPokeballPoolCandyId();
        this.poolBallsId = MiscConfig.getAuspiciousPokeballPoolBallsId();
        this.poolTreasuresId = MiscConfig.getAuspiciousPokeballPoolTreasuresId();
        this.minRoll = MiscConfig.getAuspiciousPokeballMinRoll();
        this.maxRoll = MiscConfig.getAuspiciousPokeballMaxRoll();
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

    public void resetClaims() {
        if (!this.claimedPlayers.isEmpty()) {
            this.claimedPlayers.clear();
            this.syncClaimData();
        }
    }

    public Set<UUID> getClaimedPlayers() {
        return Collections.unmodifiableSet(this.claimedPlayers);
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

    public OpenAuspiciousPokeballConfigPayload createOpenPayload() {
        return new OpenAuspiciousPokeballConfigPayload(
                this.worldPosition,
                this.poolBerryId,
                this.poolCandyId,
                this.poolBallsId,
                this.poolTreasuresId,
                this.minRoll,
                this.maxRoll
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

        this.poolBerryId = tag.contains(NBT_POOL_BERRY) ? tag.getString(NBT_POOL_BERRY) : MiscConfig.getAuspiciousPokeballPoolBerryId();
        this.poolCandyId = tag.contains(NBT_POOL_CANDY) ? tag.getString(NBT_POOL_CANDY) : MiscConfig.getAuspiciousPokeballPoolCandyId();
        this.poolBallsId = tag.contains(NBT_POOL_BALLS) ? tag.getString(NBT_POOL_BALLS) : MiscConfig.getAuspiciousPokeballPoolBallsId();
        this.poolTreasuresId = tag.contains(NBT_POOL_TREASURES) ? tag.getString(NBT_POOL_TREASURES) : MiscConfig.getAuspiciousPokeballPoolTreasuresId();
        this.minRoll = tag.contains(NBT_MIN_ROLL) ? tag.getInt(NBT_MIN_ROLL) : MiscConfig.getAuspiciousPokeballMinRoll();
        this.maxRoll = tag.contains(NBT_MAX_ROLL) ? tag.getInt(NBT_MAX_ROLL) : MiscConfig.getAuspiciousPokeballMaxRoll();
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
