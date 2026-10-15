package maxigregrze.cobblesafari.block.ultraspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.HorizontalModelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An Ultra Warp Dive prop whose model is (partly) animated by a block entity renderer (action plan
 * 189, section 4.5): the negative's spinning core and ring, the rocks' glint.
 *
 * <p>The block entity carries no data and no ticker; it only exists to host the renderer. The
 * render shape is a setting because the two users differ: the negative bakes its still cage into
 * the chunk ({@code MODEL}), the rocks are drawn entirely by the renderer
 * ({@code ENTITYBLOCK_ANIMATED}), since the armour glint tests depth for equality and must be drawn
 * with exactly the matrices of the geometry it covers.</p>
 */
public class UltraWarpDiveBlock extends HorizontalModelBlock implements EntityBlock {

    private final Settings settings;
    private final RenderShape renderShape;

    public UltraWarpDiveBlock(Properties properties, Settings settings, RenderShape renderShape) {
        super(properties, settings);
        this.settings = settings;
        this.renderShape = renderShape;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new UltraWarpDiveBlock(props, this.settings, this.renderShape));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return renderShape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UltraWarpDiveBlockEntity(pos, state);
    }
}
