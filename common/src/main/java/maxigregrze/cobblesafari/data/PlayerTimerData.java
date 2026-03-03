package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.config.DimensionTimerEntry;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class PlayerTimerData {
    private final UUID playerId;
    private final String dimensionId;
    private int remainingTicks;
    private long lastResetTimestamp;
    private boolean isActive;
    private BlockPos originPos;
    private ResourceKey<Level> originDimension;
    private long lastSafariBallGrantDay = 0;
    private boolean timerBypassed = false;
    private long lastEntryFeePayDay = -1;
    private boolean needsEvacuation = false;

    public PlayerTimerData(UUID playerId, String dimensionId) {
        this.playerId = playerId;
        this.dimensionId = dimensionId;
        this.remainingTicks = getInitialTicks(dimensionId);
        this.lastResetTimestamp = System.currentTimeMillis();
        this.isActive = false;
        this.originPos = null;
        this.originDimension = null;
    }

    public PlayerTimerData(UUID playerId, String dimensionId, int remainingTicks, long lastResetTimestamp, boolean isActive) {
        this.playerId = playerId;
        this.dimensionId = dimensionId;
        this.remainingTicks = remainingTicks;
        this.lastResetTimestamp = lastResetTimestamp;
        this.isActive = isActive;
        this.originPos = null;
        this.originDimension = null;
    }

    @Deprecated
    public PlayerTimerData(UUID playerId) {
        this(playerId, SafariTimerConfig.getSafariDimensionId());
    }

    @Deprecated
    public PlayerTimerData(UUID playerId, int remainingTicks, long lastResetTimestamp, boolean isActive) {
        this(playerId, SafariTimerConfig.getSafariDimensionId(), remainingTicks, lastResetTimestamp, isActive);
    }

    private static int getInitialTicks(String dimensionId) {
        return SafariTimerConfig.getDimensionConfig(dimensionId)
                .map(DimensionTimerEntry::getTimerDurationTicks)
                .orElse(SafariTimerConfig.getTimerDurationTicks());
    }

    public void tick() {
        if (isActive && remainingTicks > 0) {
            remainingTicks--;
        }
    }

    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    public void reset() {
        this.remainingTicks = getInitialTicks(dimensionId);
        this.lastResetTimestamp = System.currentTimeMillis();
    }

    public String getFormattedTime() {
        int totalSeconds = remainingTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getDimensionId() {
        return dimensionId;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public void setRemainingTicks(int remainingTicks) {
        this.remainingTicks = remainingTicks;
    }

    public long getLastResetTimestamp() {
        return lastResetTimestamp;
    }

    public void setLastResetTimestamp(long lastResetTimestamp) {
        this.lastResetTimestamp = lastResetTimestamp;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getResetHour() {
        return SafariTimerConfig.getDimensionConfig(dimensionId)
                .map(DimensionTimerEntry::getResetHour)
                .orElse(SafariTimerConfig.getResetHour());
    }

    public BlockPos getOriginPos() {
        return originPos;
    }

    public void setOriginPos(BlockPos pos) {
        this.originPos = pos;
    }

    public ResourceKey<Level> getOriginDimension() {
        return originDimension;
    }

    public void setOriginDimension(ResourceKey<Level> dimension) {
        this.originDimension = dimension;
    }

    public long getLastSafariBallGrantDay() {
        return lastSafariBallGrantDay;
    }

    public void setLastSafariBallGrantDay(long day) {
        this.lastSafariBallGrantDay = day;
    }

    public boolean isTimerBypassed() {
        return timerBypassed;
    }

    public void setTimerBypassed(boolean bypassed) {
        this.timerBypassed = bypassed;
    }

    public long getLastEntryFeePayDay() {
        return lastEntryFeePayDay;
    }

    public void setLastEntryFeePayDay(long day) {
        this.lastEntryFeePayDay = day;
    }

    public boolean hasPaidEntryFeeToday() {
        long todayEpochDay = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).toEpochDay();
        return lastEntryFeePayDay == todayEpochDay;
    }

    public void markEntryFeePaidToday() {
        this.lastEntryFeePayDay = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).toEpochDay();
    }

    public void resetEntryFeePayDay() {
        this.lastEntryFeePayDay = -1;
    }

    public boolean isNeedsEvacuation() {
        return needsEvacuation;
    }

    public void setNeedsEvacuation(boolean needsEvacuation) {
        this.needsEvacuation = needsEvacuation;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("playerId", playerId);
        tag.putString("dimensionId", dimensionId);
        tag.putInt("remainingTicks", remainingTicks);
        tag.putLong("lastResetTimestamp", lastResetTimestamp);
        tag.putBoolean("isActive", isActive);
        
        if (originPos != null) {
            tag.putInt("OriginX", originPos.getX());
            tag.putInt("OriginY", originPos.getY());
            tag.putInt("OriginZ", originPos.getZ());
        }
        
        if (originDimension != null) {
            tag.putString("OriginDimension", originDimension.location().toString());
        }

        tag.putLong("lastSafariBallGrantDay", lastSafariBallGrantDay);
        tag.putBoolean("timerBypassed", timerBypassed);
        tag.putLong("lastEntryFeePayDay", lastEntryFeePayDay);
        tag.putBoolean("needsEvacuation", needsEvacuation);

        return tag;
    }

    public static PlayerTimerData fromNbt(CompoundTag tag) {
        UUID playerId = tag.getUUID("playerId");
        String dimensionId = tag.contains("dimensionId") 
                ? tag.getString("dimensionId") 
                : SafariTimerConfig.getSafariDimensionId();
        int remainingTicks = tag.getInt("remainingTicks");
        long lastResetTimestamp = tag.getLong("lastResetTimestamp");
        boolean isActive = tag.getBoolean("isActive");
        
        PlayerTimerData data = new PlayerTimerData(playerId, dimensionId, remainingTicks, lastResetTimestamp, isActive);
        
        if (tag.contains("OriginX")) {
            data.originPos = new BlockPos(
                    tag.getInt("OriginX"),
                    tag.getInt("OriginY"),
                    tag.getInt("OriginZ")
            );
        }
        
        if (tag.contains("OriginDimension")) {
            data.originDimension = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(tag.getString("OriginDimension"))
            );
        }

        if (tag.contains("lastSafariBallGrantDay")) {
            data.lastSafariBallGrantDay = tag.getLong("lastSafariBallGrantDay");
        }

        if (tag.contains("timerBypassed")) {
            data.timerBypassed = tag.getBoolean("timerBypassed");
        }

        if (tag.contains("lastEntryFeePayDay")) {
            data.lastEntryFeePayDay = tag.getLong("lastEntryFeePayDay");
        }

        if (tag.contains("needsEvacuation")) {
            data.needsEvacuation = tag.getBoolean("needsEvacuation");
        }

        return data;
    }
}
