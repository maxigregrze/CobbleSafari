package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.base.BlockShapeUtils;
import maxigregrze.cobblesafari.block.base.HorizontalModelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Kalosian Chimney (plan 194 - 11 bis): an oriented model with one of three stack layouts, which a
 * right-click with a pickaxe chips down to its body and cap - and back again.
 *
 * <p>{@code STACKS} picks between the random stacked models and the single stackless one in the
 * blockstate; the hitbox drops the stacks' envelope with them.</p>
 */
public class HyperspaceChimneyBlock extends HorizontalModelBlock {

    public static final BooleanProperty STACKS = BooleanProperty.create("stacks");

    /** Body and cap, authored for FACING=north: what every layout shares. */
    private static final Map<Direction, VoxelShape> BASE_SHAPES = BlockShapeUtils.precompute(
            Shapes.or(Block.box(0, 0, 5, 16, 16, 11), Block.box(0, 16, 4, 16, 19, 12)), Direction.NORTH);

    public HyperspaceChimneyBlock(Properties properties, Settings settings) {
        super(properties, settings);
        this.registerDefaultState(this.defaultBlockState().setValue(STACKS, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new HyperspaceChimneyBlock(props, this.settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STACKS);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ItemTags.PICKAXES)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide) {
            BlockState toggled = state.cycle(STACKS);
            level.setBlock(pos, toggled, Block.UPDATE_ALL);
            SoundType sound = state.getSoundType();
            level.playSound(null, pos, sound.getHitSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Boolean.TRUE.equals(state.getValue(STACKS))
                ? super.getShape(state, level, pos, context)
                : BASE_SHAPES.get(state.getValue(FACING));
    }
}
