package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
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

    private final Set<UUID> claimedPlayers = new HashSet<>();

    public LostItemBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOST_ITEM, pos, state);
    }

    public boolean hasClaimed(UUID playerId) {
        return this.claimedPlayers.contains(playerId);
    }

    public boolean tryClaim(UUID playerId) {
        boolean added = this.claimedPlayers.add(playerId);
        if (added) {
            this.sync();
        }
        return added;
    }

    public Set<UUID> getClaimedPlayers() {
        return Collections.unmodifiableSet(this.claimedPlayers);
    }

    public void resetClaims() {
        if (!this.claimedPlayers.isEmpty()) {
            this.claimedPlayers.clear();
            this.sync();
        }
    }

    private void sync() {
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
