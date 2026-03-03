package maxigregrze.cobblesafari.block.incubator;

import com.cobblemon.mod.common.CobblemonSounds;
import maxigregrze.cobblesafari.config.IncubatorConfig;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.incubator.CobbreedingCompat;
import maxigregrze.cobblesafari.incubator.EggIncubatorRecipe;
import maxigregrze.cobblesafari.incubator.EggIncubatorRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class IncubatorBlockEntity extends BlockEntity {

    private ItemStack inputItem = ItemStack.EMPTY;
    private int ticksRemaining = -1;
    private int totalTicks = 14400;
    private boolean isCobbreedingEgg = false;
    private String storedEggSpeciesName = "";

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INCUBATOR, pos, state);
    }

    public ItemStack getInputItem() {
        return inputItem;
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public boolean isEmpty() {
        return inputItem.isEmpty();
    }

    public boolean isWorking() {
        return ticksRemaining > 0;
    }

    public boolean isDone() {
        return !inputItem.isEmpty() && ticksRemaining == 0;
    }

    public void startIncubation(ItemStack egg) {
        this.inputItem = egg.copyWithCount(1);
        EggIncubatorRecipe recipe = EggIncubatorRegistry.getRecipe(egg);
        int hatchTime = recipe != null ? recipe.hatchTime() : IncubatorConfig.getDefaultWildEggHatchTimeTicks();
        this.ticksRemaining = hatchTime;
        this.totalTicks = hatchTime;
        this.isCobbreedingEgg = false;
        updateBlockState(1);
        setChanged();
        syncToClient();
    }

    public void startCobbreedingIncubation(ItemStack cobbreedingEgg, int originalTimerTicks) {
        this.inputItem = cobbreedingEgg.copyWithCount(1);
        this.isCobbreedingEgg = true;
        this.storedEggSpeciesName = CobbreedingCompat.getEggName(cobbreedingEgg);
        if (this.storedEggSpeciesName == null) {
            this.storedEggSpeciesName = "";
        }
        int adjustedTime = Math.max(1, Math.round(originalTimerTicks * IncubatorConfig.getCobbreedingHatchSpeedMultiplier()));
        this.ticksRemaining = adjustedTime;
        this.totalTicks = adjustedTime;
        updateBlockState(1);
        setChanged();
        syncToClient();
    }

    public boolean isCobbreedingEgg() {
        return isCobbreedingEgg;
    }

    public String getStoredEggSpeciesName() {
        return storedEggSpeciesName != null ? storedEggSpeciesName : "";
    }

    public void finishInstantly() {
        if (!isWorking()) return;
        this.ticksRemaining = 0;
        updateBlockState(IncubatorBlock.STATE_DONE);
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, CobblemonSounds.FOSSIL_MACHINE_FINISHED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        setChanged();
        syncToClient();
    }

    public void reset() {
        this.inputItem = ItemStack.EMPTY;
        this.ticksRemaining = -1;
        this.totalTicks = 14400;
        this.isCobbreedingEgg = false;
        this.storedEggSpeciesName = "";
        updateBlockState(IncubatorBlock.STATE_EMPTY);
        setChanged();
        syncToClient();
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide() || ticksRemaining <= 0) return;

        if (ticksRemaining % 20 == 0) {
            level.playSound(null, pos, CobblemonSounds.FOSSIL_MACHINE_ACTIVE_LOOP, SoundSource.BLOCKS, 0.5F, 1.0F);
        }

        ticksRemaining--;

        int elapsed = totalTicks - ticksRemaining;
        int newStage = Math.min(1 + (elapsed * 5 / totalTicks), 5);
        int currentState = state.getValue(IncubatorBlock.STATE);

        if (newStage != currentState) {
            updateBlockState(newStage);
            syncToClient();
        }

        if (ticksRemaining == 0) {
            updateBlockState(IncubatorBlock.STATE_DONE);
            level.playSound(null, pos, CobblemonSounds.FOSSIL_MACHINE_FINISHED, SoundSource.BLOCKS, 1.0F, 1.0F);
            syncToClient();
        }

        setChanged();
    }

    private void updateBlockState(int newState) {
        if (level != null && !level.isClientSide()) {
            BlockState current = getBlockState();
            if (current.getValue(IncubatorBlock.STATE) != newState) {
                level.setBlock(worldPosition, current.setValue(IncubatorBlock.STATE, newState), Block.UPDATE_ALL);
            }
        }
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!inputItem.isEmpty()) {
            tag.put("InputItem", inputItem.save(registries));
        }
        tag.putInt("TicksRemaining", ticksRemaining);
        tag.putInt("TotalTicks", totalTicks);
        tag.putBoolean("IsCobbreedingEgg", isCobbreedingEgg);
        if (isCobbreedingEgg && storedEggSpeciesName != null && !storedEggSpeciesName.isEmpty()) {
            tag.putString("StoredEggSpeciesName", storedEggSpeciesName);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("InputItem")) {
            inputItem = ItemStack.parse(registries, tag.getCompound("InputItem")).orElse(ItemStack.EMPTY);
        } else {
            inputItem = ItemStack.EMPTY;
        }
        ticksRemaining = tag.getInt("TicksRemaining");
        totalTicks = tag.getInt("TotalTicks");
        isCobbreedingEgg = tag.getBoolean("IsCobbreedingEgg");
        storedEggSpeciesName = tag.contains("StoredEggSpeciesName") ? tag.getString("StoredEggSpeciesName") : "";
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
