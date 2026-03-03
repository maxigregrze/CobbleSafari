package maxigregrze.cobblesafari.dungeon;

public class DungeonDimensionEntry {
    private String dimensionId;
    private boolean isEnabled = true;
    private int spawnWeight = 1;
    private boolean enableEntryFee = false;
    private String entryFee = "cobblesafari:ticket_dungeon";
    private boolean cobbledollarEntryFee = false;
    private int entryFeeAmount = 5000;
    private boolean allowMultiplePayment = false;

    public DungeonDimensionEntry() {
    }

    public DungeonDimensionEntry(String dimensionId, boolean isEnabled, int spawnWeight) {
        this.dimensionId = dimensionId;
        this.isEnabled = isEnabled;
        this.spawnWeight = spawnWeight;
    }

    public String getDimensionId() {
        return dimensionId;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public int getSpawnWeight() {
        return spawnWeight;
    }

    public boolean isEntryFeeEnabled() {
        return enableEntryFee;
    }

    public String getEntryFee() {
        return entryFee != null ? entryFee : "cobblesafari:ticket_dungeon";
    }

    public boolean isCobbledollarEntryFee() {
        return cobbledollarEntryFee;
    }

    public int getEntryFeeAmount() {
        return entryFeeAmount > 0 ? entryFeeAmount : 5000;
    }

    public boolean isAllowMultiplePayment() {
        return allowMultiplePayment;
    }
}
