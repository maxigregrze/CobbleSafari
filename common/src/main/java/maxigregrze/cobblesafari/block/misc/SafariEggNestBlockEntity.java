package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SafariEggNestBlockEntity extends BlockEntity {

    private String biomeType = "";
    private long nextRefillTick = 0;

    public SafariEggNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAFARI_EGG_NEST, pos, state);
    }

    public String getBiomeType() {
        return biomeType;
    }

    public void setBiomeType(String biomeType) {
        this.biomeType = biomeType;
        setChanged();
    }

    public long getNextRefillTick() {
        return nextRefillTick;
    }

    public void setNextRefillTick(long nextRefillTick) {
        this.nextRefillTick = nextRefillTick;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("BiomeType", biomeType);
        tag.putLong("NextRefillTick", nextRefillTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("BiomeType")) {
            biomeType = tag.getString("BiomeType");
        }
        if (tag.contains("NextRefillTick")) {
            nextRefillTick = tag.getLong("NextRefillTick");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
