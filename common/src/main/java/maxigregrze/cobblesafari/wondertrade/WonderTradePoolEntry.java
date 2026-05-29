package maxigregrze.cobblesafari.wondertrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class WonderTradePoolEntry {
    private static final String NBT_SYSTEM_GEN = "SystemGen";

    private CompoundTag pokemonData;
    private int timeSinceDeposit;
    /** {@code true} = généré par autofill ; {@code false} = déposé via échange joueur. */
    private boolean systemGenerated;

    public WonderTradePoolEntry(CompoundTag pokemonData, int timeSinceDeposit, boolean systemGenerated) {
        this.pokemonData = pokemonData.copy();
        this.timeSinceDeposit = timeSinceDeposit;
        this.systemGenerated = systemGenerated;
    }

    public CompoundTag getPokemonData() {
        return pokemonData;
    }

    public void setPokemonData(CompoundTag pokemonData) {
        this.pokemonData = pokemonData.copy();
    }

    public int getTimeSinceDeposit() {
        return timeSinceDeposit;
    }

    public void setTimeSinceDeposit(int timeSinceDeposit) {
        this.timeSinceDeposit = timeSinceDeposit;
    }

    public void incrementTimeSinceDeposit() {
        this.timeSinceDeposit++;
    }

    public boolean isSystemGenerated() {
        return systemGenerated;
    }

    public void setSystemGenerated(boolean systemGenerated) {
        this.systemGenerated = systemGenerated;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("Pokemon", pokemonData.copy());
        tag.putInt("TimeSinceDeposit", timeSinceDeposit);
        tag.putBoolean(NBT_SYSTEM_GEN, systemGenerated);
        return tag;
    }

    public static WonderTradePoolEntry fromNbt(CompoundTag tag) {
        CompoundTag p = tag.getCompound("Pokemon");
        int t = tag.getInt("TimeSinceDeposit");
        boolean sys = tag.contains(NBT_SYSTEM_GEN, Tag.TAG_BYTE) && tag.getBoolean(NBT_SYSTEM_GEN);
        return new WonderTradePoolEntry(p, t, sys);
    }
}
