package maxigregrze.cobblesafari.block.pokestatue;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creative Statue configuration. Single block, so no derived block state and nothing to sync
 * beyond itself; the mode is a {@link CreativeStatueTexture} rather than a material.
 *
 * <p>The only statue that can play its animation instead of holding a frame, and the only one that
 * can hide a species Cobblemon knows but has no model for (action plan 190, B7). Progress and speed
 * are independent: each is kept while the other mode is shown.</p>
 */
public class CreativeStatueBlockEntity extends AbstractPokeStatueBlockEntity {

    public static final float MAX_SCALE = 5.0f;
    public static final float MIN_SPEED = 0.01f;
    public static final float MAX_SPEED = 5.0f;
    public static final float DEFAULT_SPEED = 1.0f;

    private static final String KEY_FIXED = "AnimationFixed";
    private static final String KEY_SPEED = "AnimationSpeed";
    private static final String KEY_SHOW_DUMMY = "ShowDummy";

    /** On by default, and on for every statue saved before the option existed: same look as before. */
    private boolean animationFixed = true;
    private float animationSpeed = DEFAULT_SPEED;
    private boolean showDummyWithoutModel = false;

    public CreativeStatueBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_STATUE, pos, state);
    }

    @Override
    public PokeStatueVariant variant() {
        return PokeStatueVariant.CREATIVE;
    }

    public CreativeStatueTexture getTexture() {
        return CreativeStatueTexture.byId(getMode());
    }

    @Override
    public PokeStatueRenderStyle renderStyle() {
        return getTexture().styleFor(getColor());
    }

    // -- playback options ---------------------------------------------------

    @Override
    protected void applyPlaybackOptions(boolean fixed, float speed, boolean showDummy) {
        this.animationFixed = fixed;
        this.animationSpeed = sanitizeSpeed(speed);
        this.showDummyWithoutModel = showDummy;
    }

    @Override
    public boolean isAnimationFixed() {
        return animationFixed;
    }

    @Override
    public float getAnimationSpeed() {
        return animationSpeed;
    }

    @Override
    public boolean showsDummyWithoutModel() {
        return showDummyWithoutModel;
    }

    public static float sanitizeSpeed(float value) {
        if (!Float.isFinite(value)) {
            return DEFAULT_SPEED;
        }
        return Math.round(Math.clamp(value, MIN_SPEED, MAX_SPEED) * 100.0f) / 100.0f;
    }

    // -- persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(KEY_FIXED, animationFixed);
        tag.putFloat(KEY_SPEED, animationSpeed);
        tag.putBoolean(KEY_SHOW_DUMMY, showDummyWithoutModel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.animationFixed = !tag.contains(KEY_FIXED) || tag.getBoolean(KEY_FIXED);
        this.animationSpeed = tag.contains(KEY_SPEED) ? sanitizeSpeed(tag.getFloat(KEY_SPEED)) : DEFAULT_SPEED;
        this.showDummyWithoutModel = tag.getBoolean(KEY_SHOW_DUMMY);
    }
}
