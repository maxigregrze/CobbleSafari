package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A {@link SideConnectedModelBlock} that also knows whether something solid sits directly beneath
 * it, so a platform can drop a skirt down to meet the ground and stay flat when it floats.
 *
 * <p>{@code BOTTOM} lives on the Y axis but is <em>not</em> a connection: any sturdy block counts
 * and tag membership is irrelevant. That is why this is a separate class rather than a setting on
 * {@code connectTag}. The criterion is {@code isFaceSturdy(UP)} - the same test the rest of the
 * repo uses for "can hold something up" - so a bottom slab or a stair counts and a torch does not.
 */
public class GroundedSideConnectedModelBlock extends SideConnectedModelBlock {

    public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;

    public GroundedSideConnectedModelBlock(Properties properties, Settings settings) {
        super(properties, settings);
        this.registerDefaultState(this.defaultBlockState().setValue(BOTTOM, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new GroundedSideConnectedModelBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BOTTOM);
    }

    @Override
    protected BlockState computeState(BlockState state, BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState under = ApparentState.at(level, below);
        // A tagged block counts even when its collision is not a sturdy face: the scaffolding tube
        // is hollow so you can drop through it, yet a platform resting on one should still skirt.
        boolean grounded = under.isFaceSturdy(level, below, Direction.UP)
                || (settings.groundTag() != null && under.is(settings.groundTag()));
        return super.computeState(state, level, pos).setValue(BOTTOM, grounded);
    }
}
