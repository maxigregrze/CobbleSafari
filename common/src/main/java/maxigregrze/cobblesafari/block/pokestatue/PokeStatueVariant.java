package maxigregrze.cobblesafari.block.pokestatue;

/**
 * Which statue block a config screen was opened for. Travels in the payloads so one shared GUI can
 * change its mode row, its labels and its scale clamp without a second screen class.
 *
 * <p>The server never trusts the variant a client announces - it re-derives everything from the
 * block actually present at the target position.</p>
 */
public enum PokeStatueVariant {

    /** Two-block statue with a pedestal: five materials, scale capped at 3. */
    REGULAR(PokeStatueMaterial.values().length, PokeStatueBlockEntity.MAX_SCALE, "material"),
    /** Single creative-only block: three texture modes, scale capped at 5. */
    CREATIVE(CreativeStatueTexture.values().length, CreativeStatueBlockEntity.MAX_SCALE, "texture");

    private static final PokeStatueVariant[] VALUES = values();

    private final int modeCount;
    private final float maxScale;
    private final String modeKey;

    PokeStatueVariant(int modeCount, float maxScale, String modeKey) {
        this.modeCount = modeCount;
        this.maxScale = maxScale;
        this.modeKey = modeKey;
    }

    public int getModeCount() {
        return modeCount;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public int getId() {
        return ordinal();
    }

    public static PokeStatueVariant byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : REGULAR;
    }

    /** Translation key of the mode row's label. */
    public String modeLabelKey() {
        return "gui.cobblesafari.pokestatue." + modeKey;
    }

    /** Translation key of one mode's displayed value. */
    public String modeValueKey(int mode) {
        String name = this == CREATIVE
                ? CreativeStatueTexture.byId(mode).getSerializedName()
                : PokeStatueMaterial.byId(mode).getSerializedName();
        return "gui.cobblesafari.pokestatue." + modeKey + "." + name;
    }

    /** Cycles the mode index, wrapping, in whichever enum this variant uses. */
    public int cycleMode(int mode, int direction) {
        int count = getModeCount();
        return ((mode + direction) % count + count) % count;
    }
}
