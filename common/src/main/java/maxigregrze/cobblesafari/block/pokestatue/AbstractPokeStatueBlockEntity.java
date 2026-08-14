package maxigregrze.cobblesafari.block.pokestatue;

import maxigregrze.cobblesafari.network.OpenPokeStatueConfigPayload;
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
 * Configuration shared by every statue block: which Pokémon, how it is finished, how big, which
 * animation it is frozen in and where in that animation.
 *
 * <p>Subclasses supply only what genuinely differs - the scale cap, the mode enum's meaning, which
 * positions to refresh on a sync, and any block state derived from the data.</p>
 */
public abstract class AbstractPokeStatueBlockEntity extends BlockEntity {

    /** No colour set. */
    public static final int NO_COLOR = -1;
    public static final float MIN_SCALE = 0.01f;
    public static final float DEFAULT_SCALE = 1.0f;
    public static final int MAX_SPECIES_LENGTH = 256;
    public static final int MAX_ANIMATION_LENGTH = 64;

    private static final String KEY_SPECIES = "Species";
    /** Kept as "Material" for the regular statue's sake; it is the mode ordinal for both blocks. */
    private static final String KEY_MODE = "Material";
    private static final String KEY_SCALE = "Scale";
    private static final String KEY_COLOR = "Color";
    private static final String KEY_ANIMATION = "Animation";
    private static final String KEY_PROGRESS = "Progress";

    private String species = "";
    private int mode = 0;
    private float scale = DEFAULT_SCALE;
    private int color = NO_COLOR;
    /** Empty means the default frozen pose (the portrait pose plus its idles at tick 0). */
    private String animation = "";
    /** Fraction of the selected animation, 0 to 1. */
    private float progress = 0.0f;

    protected AbstractPokeStatueBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // -- subclass contract --------------------------------------------------

    /** Which flavour of config screen this statue opens, and therefore its clamps and labels. */
    public abstract PokeStatueVariant variant();

    /** How the Pokémon model should be drawn for the current mode and colour. */
    public abstract PokeStatueRenderStyle renderStyle();

    /** Positions to refresh when the configuration changes. */
    protected BlockPos[] syncPositions() {
        return new BlockPos[]{worldPosition};
    }

    /** Hook for block states derived from the data (light level, model selection). */
    protected void applyDerivedState() {
        // optional
    }

    // -- accessors ----------------------------------------------------------

    public String getSpecies() {
        return species;
    }

    public int getMode() {
        return mode;
    }

    public float getScale() {
        return scale;
    }

    /** Configured colour, or {@link #NO_COLOR} when the field was left empty. */
    public int getColor() {
        return color;
    }

    public boolean hasColor() {
        return color != NO_COLOR;
    }

    public String getAnimation() {
        return animation;
    }

    public float getProgress() {
        return progress;
    }

    // -- mutation -----------------------------------------------------------

    /**
     * Applies a whole configuration at once and re-syncs. Values are expected to be sanitized by
     * {@code PokeStatueConfigServerHandler}; they are clamped again here so a direct caller cannot
     * store nonsense.
     */
    public void configure(String species, int mode, float scale, int color, String animation, float progress) {
        this.species = truncate(species, MAX_SPECIES_LENGTH);
        this.mode = Math.clamp(mode, 0, variant().getModeCount() - 1);
        this.scale = sanitizeScale(scale, variant().getMaxScale());
        this.color = color < 0 ? NO_COLOR : (color & 0xFFFFFF);
        this.animation = truncate(animation, MAX_ANIMATION_LENGTH);
        this.progress = sanitizeProgress(progress);
        applyDerivedState();
        syncToClients();
    }

    private static String truncate(@Nullable String s, int max) {
        if (s == null) {
            return "";
        }
        String trimmed = s.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    public static float sanitizeScale(float value, float max) {
        if (!Float.isFinite(value)) {
            return DEFAULT_SCALE;
        }
        return Math.round(Math.clamp(value, MIN_SCALE, max) * 100.0f) / 100.0f;
    }

    public static float sanitizeProgress(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.round(Math.clamp(value, 0.0f, 1.0f) * 100.0f) / 100.0f;
    }

    public OpenPokeStatueConfigPayload createOpenPayload() {
        return new OpenPokeStatueConfigPayload(
                worldPosition, variant().getId(), species, mode, scale, color, animation, progress);
    }

    /** Pushes the data to clients for every position that renders part of this statue. */
    public void syncToClients() {
        setChanged();
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        for (BlockPos p : syncPositions()) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof AbstractPokeStatueBlock) {
                level.sendBlockUpdated(p, s, s, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** The data-bearing block entity for a statue block at {@code pos}, whichever part was hit. */
    @Nullable
    public static AbstractPokeStatueBlockEntity dataAt(@Nullable Level level, BlockPos pos, BlockState state) {
        if (level == null || !(state.getBlock() instanceof AbstractPokeStatueBlock block)) {
            return null;
        }
        return level.getBlockEntity(block.dataPos(state, pos)) instanceof AbstractPokeStatueBlockEntity be
                ? be : null;
    }

    // -- persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(KEY_SPECIES, species);
        tag.putInt(KEY_MODE, mode);
        tag.putFloat(KEY_SCALE, scale);
        tag.putInt(KEY_COLOR, color);
        tag.putString(KEY_ANIMATION, animation);
        tag.putFloat(KEY_PROGRESS, progress);
    }

    /**
     * Marks the render section dirty when the colour changed, so the pedestal re-tints at once.
     *
     * <p>The tinted overlay is baked into the <b>chunk mesh</b> (see {@code PokeStatueBlock.TINTED})
     * and coloured by a {@code BlockColor} that reads this block entity. A block-entity-only update
     * does not invalidate that mesh: {@code Level.setBlock} bails out as soon as
     * {@code LevelChunk.setBlockState} returns {@code null} for an unchanged block state, so
     * {@code markAndNotifyBlock} - and with it the renderer's section invalidation - never runs. The
     * new colour would then only appear the next time something else happened to dirty the section,
     * which is seconds later. So ask for the re-mesh here.</p>
     *
     * <p>Only the colour needs this. Every other field either already lives in the block state
     * ({@code HOLOGRAM} / {@code TINTED}, whose change re-meshes on its own - which is exactly why
     * setting a colour for the <em>first</em> time looked instant) or is drawn per frame by the
     * block entity renderer (species, scale, animation, progress).</p>
     */
    private void requestPedestalRemesh(int previousColor) {
        Level level = getLevel();
        if (level == null || !level.isClientSide() || this.color == previousColor) {
            return;
        }
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int previousColor = this.color;
        this.species = truncate(tag.getString(KEY_SPECIES), MAX_SPECIES_LENGTH);
        this.mode = Math.clamp(tag.getInt(KEY_MODE), 0, variant().getModeCount() - 1);
        this.scale = tag.contains(KEY_SCALE)
                ? sanitizeScale(tag.getFloat(KEY_SCALE), variant().getMaxScale()) : DEFAULT_SCALE;
        this.color = tag.contains(KEY_COLOR) ? tag.getInt(KEY_COLOR) : NO_COLOR;
        // Absent on statues saved before animated poses existed: empty means the default pose.
        this.animation = truncate(tag.getString(KEY_ANIMATION), MAX_ANIMATION_LENGTH);
        this.progress = tag.contains(KEY_PROGRESS) ? sanitizeProgress(tag.getFloat(KEY_PROGRESS)) : 0.0f;
        requestPedestalRemesh(previousColor);
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
