package maxigregrze.cobblesafari.block.csboss;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.network.OpenCsBossMimicConfigPayload;
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
import org.jetbrains.annotations.Nullable;

/**
 * Mimic block entity (plan 125 § 2.2): persists the id of the block whose texture is copied
 * by the renderer. The {@code reverse} flag lives on the blockstate, not here.
 */
public class CsBossMimicBlockEntity extends BlockEntity {

    private static final String KEY_MIMIC = "MimicBlock";

    private String mimicBlockId = "";

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

    public OpenCsBossMimicConfigPayload createOpenPayload() {
        boolean reverse = this.getBlockState().hasProperty(CsBossMimicBlock.REVERSE)
                && this.getBlockState().getValue(CsBossMimicBlock.REVERSE);
        return new OpenCsBossMimicConfigPayload(this.worldPosition, this.mimicBlockId, reverse);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mimicBlockId = tag.getString(KEY_MIMIC);
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
