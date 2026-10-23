package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * {@link GroundedSideConnectedModelBlock}'s {@code BOTTOM} skirt on top of the diagonal-aware
 * connections of {@link DiagonalSideConnectedModelBlock}. Java has no mix-ins, and a property cannot
 * be added from a setting, hence this pairing class.
 */
public class GroundedDiagonalSideConnectedModelBlock extends DiagonalSideConnectedModelBlock {

    public static final BooleanProperty BOTTOM = GroundedSideConnectedModelBlock.BOTTOM;

    public GroundedDiagonalSideConnectedModelBlock(Properties properties, Settings settings) {
        super(properties, settings);
        this.registerDefaultState(this.defaultBlockState().setValue(BOTTOM, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new GroundedDiagonalSideConnectedModelBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BOTTOM);
    }

    @Override
    protected BlockState computeState(BlockState state, BlockGetter level, BlockPos pos) {
        return super.computeState(state, level, pos)
                .setValue(BOTTOM, GroundedSideConnectedModelBlock.isGrounded(level, pos, settings.groundTag()));
    }
}
