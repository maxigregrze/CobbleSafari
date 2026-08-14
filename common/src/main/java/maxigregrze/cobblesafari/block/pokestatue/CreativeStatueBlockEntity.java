package maxigregrze.cobblesafari.block.pokestatue;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creative Statue configuration. Single block, so no derived block state and nothing to sync
 * beyond itself; the mode is a {@link CreativeStatueTexture} rather than a material.
 */
public class CreativeStatueBlockEntity extends AbstractPokeStatueBlockEntity {

    public static final float MAX_SCALE = 5.0f;

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
}
