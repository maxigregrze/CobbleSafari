package maxigregrze.cobblesafari.block.csboss;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.network.OpenCsBossMimicConfigPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Mimic block entity: persists the id of the block whose appearance is copied, and carries the
 * <em>resolved</em> state of that block - the one it would hold if it really stood here, connections
 * and all. The {@code reverse} flag lives on the blockstate, not here.
 *
 * <p>The resolved state is derived, never authored: it is recomputed server-side by
 * {@link CsBossMimicBlock#refresh} and pushed to clients, but deliberately <strong>not written to
 * disk</strong>. Only {@link #KEY_MIMIC} is saved. That is what makes the feature free of any save
 * migration - an old mimic simply loads with no resolved state and the block's server ticker fills
 * it in - and it stops a stale state from outliving a datapack that removed the block it named.</p>
 */
public class CsBossMimicBlockEntity extends BlockEntity {

    private static final String KEY_MIMIC = "MimicBlock";
    private static final String KEY_BATTLE_MIMIC = "BattleMimicBlock";

    /** Update-tag only. Absent from {@code saveAdditional} on purpose - see the class javadoc. */
    private static final String KEY_RESOLVED = "Resolved";

    private String mimicBlockId = "";

    /** Block copied during a fight in swap mode ({@link CsBossMimicBlock.Mode#SWAP}); unused otherwise. */
    private String battleMimicBlockId = "";

    @Nullable
    private BlockState resolvedMimicState;

    public CsBossMimicBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CSBOSS_MIMIC, pos, state);
    }

    protected CsBossMimicBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public String getMimicBlockId() {
        return mimicBlockId;
    }

    public void setMimicBlockId(String id) {
        this.mimicBlockId = id == null ? "" : id.trim();
        setChanged();
    }

    public String getBattleMimicBlockId() {
        return battleMimicBlockId;
    }

    public void setBattleMimicBlockId(String id) {
        this.battleMimicBlockId = id == null ? "" : id.trim();
        setChanged();
    }

    /**
     * The id copied in {@code state}: the battle block while a swap-mode mimic is active (falling back
     * to the idle block when none is set), the idle block in every other case.
     */
    public String getTargetId(BlockState state) {
        boolean battle = state.hasProperty(CsBossMimicBlock.SWAP) && state.getValue(CsBossMimicBlock.SWAP)
                && state.getValue(CsBossMimicBlock.ACTIVE);
        return battle && !battleMimicBlockId.isBlank() ? battleMimicBlockId : mimicBlockId;
    }

    /** The mimicked block's full state, or {@code null} until the server has resolved it once. */
    @Nullable
    public BlockState getResolvedMimicState() {
        return resolvedMimicState;
    }

    /**
     * Stores the resolved state. Returns whether it actually changed - the caller uses that as the
     * termination guard of the refresh cascade, which is sound because block states are interned
     * per state definition and so compare by identity.
     */
    public boolean setResolvedMimicState(@Nullable BlockState state) {
        if (this.resolvedMimicState == state) {
            return false;
        }
        this.resolvedMimicState = state;
        setChanged();
        return true;
    }

    /** One-shot: resolves this mimic the first tick it exists, then costs a null check. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, CsBossMimicBlockEntity be) {
        if (be.resolvedMimicState == null) {
            CsBossMimicBlock.refresh(level, pos, state, true);
        }
    }

    public OpenCsBossMimicConfigPayload createOpenPayload() {
        BlockState state = this.getBlockState();
        CsBossMimicBlock.Mode mode = state.getBlock() instanceof CsBossMimicBlock
                ? CsBossMimicBlock.Mode.of(state) : CsBossMimicBlock.Mode.HIDE_IN_BATTLE;
        return new OpenCsBossMimicConfigPayload(this.worldPosition, this.mimicBlockId, this.battleMimicBlockId,
                mode.ordinal());
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
        tag.putString(KEY_MIMIC, mimicBlockId);
        if (!battleMimicBlockId.isEmpty()) {
            tag.putString(KEY_BATTLE_MIMIC, battleMimicBlockId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mimicBlockId = tag.getString(KEY_MIMIC);
        this.battleMimicBlockId = tag.getString(KEY_BATTLE_MIMIC);
        // Present only on the client, which receives it through this same method (block entity
        // update packets are applied by loadWithComponents). On disk the key never exists.
        this.resolvedMimicState = tag.contains(KEY_RESOLVED, Tag.TAG_COMPOUND)
                ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound(KEY_RESOLVED))
                : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        if (resolvedMimicState != null) {
            tag.put(KEY_RESOLVED, NbtUtils.writeBlockState(resolvedMimicState));
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
