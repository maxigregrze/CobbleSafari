package maxigregrze.cobblesafari.block.distortion;

import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GiratinaCoreSideBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    private final int offsetXFromCenter;
    private final int offsetZFromCenter;

    public GiratinaCoreSideBlock(int offsetXFromCenter, int offsetZFromCenter, Properties properties) {
        super(properties);
        this.offsetXFromCenter = offsetXFromCenter;
        this.offsetZFromCenter = offsetZFromCenter;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        if (rotation == Rotation.NONE) {
            return state;
        }
        int[] r = rotateOffset(rotation, offsetXFromCenter, offsetZFromCenter);
        return sideBlockForOffset(r[0], r[1]).defaultBlockState();
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        int[] m = mirrorOffset(mirror, offsetXFromCenter, offsetZFromCenter);
        return sideBlockForOffset(m[0], m[1]).defaultBlockState();
    }

    private static int[] rotateOffset(Rotation rotation, int dx, int dz) {
        return switch (rotation) {
            case NONE -> new int[]{dx, dz};
            case CLOCKWISE_90 -> new int[]{-dz, dx};
            case CLOCKWISE_180 -> new int[]{-dx, -dz};
            case COUNTERCLOCKWISE_90 -> new int[]{dz, -dx};
        };
    }

    private static int[] mirrorOffset(Mirror mirror, int dx, int dz) {
        return switch (mirror) {
            case NONE -> new int[]{dx, dz};
            case LEFT_RIGHT -> new int[]{-dx, dz};
            case FRONT_BACK -> new int[]{dx, -dz};
        };
    }

    private static Block sideBlockForOffset(int dx, int dz) {
        if (dx == 0 && dz == -1) {
            return ModBlocks.GIRATINA_CORE_N;
        }
        if (dx == 1 && dz == -1) {
            return ModBlocks.GIRATINA_CORE_NE;
        }
        if (dx == 1 && dz == 0) {
            return ModBlocks.GIRATINA_CORE_E;
        }
        if (dx == 1 && dz == 1) {
            return ModBlocks.GIRATINA_CORE_SE;
        }
        if (dx == 0 && dz == 1) {
            return ModBlocks.GIRATINA_CORE_S;
        }
        if (dx == -1 && dz == 1) {
            return ModBlocks.GIRATINA_CORE_SW;
        }
        if (dx == -1 && dz == 0) {
            return ModBlocks.GIRATINA_CORE_W;
        }
        if (dx == -1 && dz == -1) {
            return ModBlocks.GIRATINA_CORE_NW;
        }
        return ModBlocks.GIRATINA_CORE_N;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!player.isCreative()) {
            return 0.0f;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos centerPos = pos.offset(-offsetXFromCenter, 0, -offsetZFromCenter);
        return level.getBlockState(centerPos).is(ModBlocks.GIRATINA_CORE);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            BlockPos centerPos = pos.offset(-offsetXFromCenter, 0, -offsetZFromCenter);
            GiratinaCoreBlock.breakMultiblock(level, centerPos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && !GiratinaCoreBlock.isRemovingMultiblock()) {
            BlockPos centerPos = pos.offset(-offsetXFromCenter, 0, -offsetZFromCenter);
            if (level.getBlockState(centerPos).is(ModBlocks.GIRATINA_CORE)) {
                GiratinaCoreBlock.breakMultiblock(level, centerPos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public String getDescriptionId() {
        return "block.cobblesafari.giratina_core";
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos centerPos = pos.offset(-offsetXFromCenter, 0, -offsetZFromCenter);
        if (!level.getBlockState(centerPos).is(ModBlocks.GIRATINA_CORE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return GiratinaCoreBlock.tryUseRedchainFragmentOnCore(stack, level, centerPos, player, hand);
    }
}
