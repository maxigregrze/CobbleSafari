package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Large Hyperspace flag: a thin, dyeable adapter over the reusable 3-cell multiblock
 * {@link HyperspaceTriBlock} (always wall-mounted, models authored for {@code facing=south}).
 * The outline covers the whole silhouette (neon panel + the arms that reach the wall) while the
 * collision is limited to those arms - which only exist on the TOP and BOTTOM cells, leaving the
 * CENTER cell walk-through. Dyeing any cell recolours the whole column. All dye behaviour is
 * delegated to {@link HyperspaceFlagDye}; the bottom cell is recomputed here from the public
 * {@link HyperspaceTriBlock#PART} property.
 */
public class HyperspaceFlagLargeBlock extends HyperspaceTriBlock {

    private final TriShapes outline;
    private final TriShapes collision;

    public HyperspaceFlagLargeBlock(Properties properties, TriShapes outline, TriShapes collision) {
        super(properties, true, Direction.SOUTH, outline, collision);
        this.outline = outline;
        this.collision = collision;
        registerDefaultState(HyperspaceFlagDye.undyed(defaultBlockState()));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new HyperspaceFlagLargeBlock(props, this.outline, this.collision));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        HyperspaceFlagDye.appendProperties(builder);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos bottom = switch (state.getValue(PART)) {
            case BOTTOM -> pos;
            case CENTER -> pos.below();
            case TOP -> pos.below(2);
        };
        return HyperspaceFlagDye.interact(stack, level, player, List.of(bottom, bottom.above(), bottom.above(2)));
    }
}
