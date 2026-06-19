package maxigregrze.cobblesafari.block.csboss;

import maxigregrze.cobblesafari.config.CsBossSettings;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.network.OpenCsBossTriggerConfigPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Arena trigger block entity: persists creative-tuned config
 * (boss, cost, radii) + active session id (0 = free).
 */
public class CsBossTriggerBlockEntity extends BlockEntity {

    private static final String KEY_BOSS_REF = "BossRef";
    private static final String KEY_COST = "CostItem";
    private static final String KEY_PLAYER_RADIUS = "PlayerRadius";
    private static final String KEY_BLOCK_RADIUS = "BlockRadius";
    private static final String KEY_SESSION = "ActiveSessionId";

    /** -1 = no override: use config default. */
    private static final int NO_OVERRIDE = -1;

    private String bossRef = "";
    private String costItemId = "";
    private int playerRadius = NO_OVERRIDE;
    private int blockRadius = NO_OVERRIDE;
    private int activeSessionId = 0;

    public CsBossTriggerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CSBOSS_TRIGGER, pos, state);
    }

    protected CsBossTriggerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public String getBossRef() {
        return bossRef;
    }

    public void setBossRef(String bossRef) {
        this.bossRef = bossRef == null ? "" : bossRef.trim();
    }

    public String getCostItemId() {
        return costItemId;
    }

    public void setCostItemId(String costItemId) {
        this.costItemId = costItemId == null ? "" : costItemId.trim();
    }

    public void setPlayerRadiusOverride(int radius) {
        CsBossSettings cfg = CsBossSettings.get();
        this.playerRadius = radius <= 0 ? NO_OVERRIDE : Mth.clamp(radius, 1, cfg.getMaxPlayerRadius());
    }

    public void setBlockRadiusOverride(int radius) {
        CsBossSettings cfg = CsBossSettings.get();
        this.blockRadius = radius < 0 ? NO_OVERRIDE : Mth.clamp(radius, 0, cfg.getMaxBlockRadius());
    }

    public int getPlayerRadiusRaw() {
        return playerRadius;
    }

    public int getBlockRadiusRaw() {
        return blockRadius;
    }

    public int effectivePlayerRadius() {
        CsBossSettings cfg = CsBossSettings.get();
        int r = playerRadius == NO_OVERRIDE ? cfg.getDefaultPlayerRadius() : playerRadius;
        return Mth.clamp(r, 1, cfg.getMaxPlayerRadius());
    }

    public int effectiveBlockRadius() {
        CsBossSettings cfg = CsBossSettings.get();
        int r = blockRadius == NO_OVERRIDE ? cfg.getDefaultBlockRadius() : blockRadius;
        return Mth.clamp(r, 0, cfg.getMaxBlockRadius());
    }

    public int getActiveSessionId() {
        return activeSessionId;
    }

    public void setActiveSessionId(int id) {
        this.activeSessionId = id;
        setChanged();
    }

    public OpenCsBossTriggerConfigPayload createOpenPayload() {
        return new OpenCsBossTriggerConfigPayload(
                this.worldPosition,
                this.bossRef,
                this.costItemId,
                this.playerRadius == NO_OVERRIDE ? CsBossSettings.get().getDefaultPlayerRadius() : this.playerRadius,
                this.blockRadius == NO_OVERRIDE ? CsBossSettings.get().getDefaultBlockRadius() : this.blockRadius,
                this.getBlockState().getValue(CsBossTriggerBlock.VARIANT).getSerializedName()
        );
    }

    public void syncToClients() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(KEY_BOSS_REF, bossRef);
        tag.putString(KEY_COST, costItemId);
        tag.putInt(KEY_PLAYER_RADIUS, playerRadius);
        tag.putInt(KEY_BLOCK_RADIUS, blockRadius);
        tag.putInt(KEY_SESSION, activeSessionId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.bossRef = tag.getString(KEY_BOSS_REF);
        this.costItemId = tag.getString(KEY_COST);
        // re-clamp overrides on load (safety: tampered NBT)
        setPlayerRadiusOverride(tag.contains(KEY_PLAYER_RADIUS) ? tag.getInt(KEY_PLAYER_RADIUS) : NO_OVERRIDE);
        setBlockRadiusOverride(tag.contains(KEY_BLOCK_RADIUS) ? tag.getInt(KEY_BLOCK_RADIUS) : NO_OVERRIDE);
        this.activeSessionId = tag.getInt(KEY_SESSION);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
