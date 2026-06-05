package maxigregrze.cobblesafari.block.balm;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BalmDispenserBlockEntity extends BlockEntity {

    private int ticksRemaining;
    private int totalRechargeTicks;

    public BalmDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BALM_DISPENSER, pos, state);
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public int getTotalRechargeTicks() {
        return totalRechargeTicks;
    }

    public boolean isRecharging() {
        return ticksRemaining > 0;
    }

    public void resetRecharge() {
        this.ticksRemaining = 0;
        this.totalRechargeTicks = 0;
    }

    public void beginRecharge(int rechargeSeconds) {
        this.totalRechargeTicks = Math.max(20, rechargeSeconds * 20);
        this.ticksRemaining = this.totalRechargeTicks;
        updateChargeFrame(computeChargeFrame());
        setChanged();
        syncToClient();
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide() || !state.getValue(BalmDispenserBlock.ACTIVE) || ticksRemaining <= 0) {
            return;
        }

        ticksRemaining--;
        int newFrame = computeChargeFrame();
        int currentFrame = state.getValue(BalmDispenserBlock.CHARGE);
        if (newFrame != currentFrame) {
            updateChargeFrame(newFrame);
            syncToClient();
        }

        setChanged();
    }

    public static int computeChargeFrame(int ticksRemaining, int totalRechargeTicks) {
        if (ticksRemaining <= 0) {
            return BalmDispenserBlock.CHARGE_READY;
        }
        int elapsed = totalRechargeTicks - ticksRemaining;
        return Math.min(BalmDispenserBlock.CHARGE_MAX, Math.max(1, (elapsed * 8 / totalRechargeTicks) + 1));
    }

    private int computeChargeFrame() {
        return computeChargeFrame(ticksRemaining, totalRechargeTicks);
    }

    private void updateChargeFrame(int frame) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState current = getBlockState();
        if (current.getValue(BalmDispenserBlock.CHARGE) != frame) {
            level.setBlock(worldPosition, current.setValue(BalmDispenserBlock.CHARGE, frame), Block.UPDATE_ALL);
        }
    }

    public void syncChargeFromSavedState() {
        if (level == null || level.isClientSide() || !getBlockState().getValue(BalmDispenserBlock.ACTIVE)) {
            return;
        }
        updateChargeFrame(computeChargeFrame());
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TicksRemaining", ticksRemaining);
        tag.putInt("TotalRechargeTicks", totalRechargeTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ticksRemaining = tag.getInt("TicksRemaining");
        totalRechargeTicks = tag.getInt("TotalRechargeTicks");
        syncChargeFromSavedState();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
